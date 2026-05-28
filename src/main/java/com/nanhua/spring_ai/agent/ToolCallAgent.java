package com.nanhua.spring_ai.agent;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nanhua.spring_ai.advisor.CustomLoggerAdvisor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class ToolCallAgent extends ReActAgent {

    /**
     * 当前 Agent 可调用的所有工具（由外部注入）
     */
    private final ToolCallback[] availableTools;

    /**
     * 最近一次 think() 的完整响应，供 act() 从中提取工具调用信息
     */
    private ChatResponse toolCallChatResponse;

    /**
     * 上一次 think() 时 API 返回的实际 promptTokens，
     * 这个值包含了 ChatMemory 中的历史消息（advisor 透明注入，messageList 不可见），
     * 是所有阈值检查的基准值。
     */
    private long lastKnownTotalTokens = 0;

    /**
     * 上一次 think() 时 messageList 的字符数快照，
     * 用于计算自那以后 messageList 增长了多少 token。
     */
    private long lastMessageListChars = 0;

    /**
     * 工具调用管理器，负责执行具体的工具逻辑
     */
    private final ToolCallingManager toolCallingManager;

    /**
     * 自定义 ChatOptions，关闭 internalToolExecutionEnabled 以禁用 Spring AI 自动工具调用，
     * 让 Agent 在 think/act 循环中手动控制工具执行
     */
    private final ToolCallingChatOptions chatOptions;

    // ═══════════════════════════════════════════
    // 滑动窗口压缩 —— 防止多步累积导致上下文溢出
    // ═══════════════════════════════════════════
    private static final long COMPRESSION_THRESHOLD_TOKENS = 100_000L;
    private static final int KEEP_RECENT_STEPS = 5;
    /** act() 后如果 token 超过此值，立即强制压缩（留 8K 余量给模型回复） */
    private static final long EMERGENCY_THRESHOLD_TOKENS = 120_000L;
    /** 紧急压缩时只保留最近 2 步，比正常压缩更激进 */
    private static final int EMERGENCY_KEEP_STEPS = 2;
    private static final Pattern FILE_PATH_PATTERN =
            Pattern.compile("tmp/(?:file|download)/[^\\s,;:<>\"'(){}\\[\\]\n\r]+");

    // ═══════════════════════════════════════════
    // LLM 摘要 —— 防止工具输出过大挤爆上下文
    // ═══════════════════════════════════════════
    private static final int SUMMARIZE_THRESHOLD_CHARS = 3000;
    private static final Set<String> SUMMARIZE_TOOLS = Set.of(
            "webSearch", "webScraping", "downloadResource");
    private ChatClient summarizeClient;

    public ToolCallAgent(ToolCallback[] avaliableTools, ChatModel model) {
        super();
        this.availableTools = avaliableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // internalToolExecutionEnabled=false：让模型返回工具调用请求但不自动执行，
        // 由我们自己在 act() 中通过 ToolCallingManager 手动执行
        this.chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .toolCallbacks(Arrays.asList(availableTools))
                .build();

        // 摘要专用 ChatClient：无工具，无 Memory，防止摘要过程调用工具或引入幻觉
        this.summarizeClient = ChatClient.builder(model)
                .defaultAdvisors(new CustomLoggerAdvisor())
                .build();
    }

    @Override
    public boolean think() {
        // 在构建 Prompt 之前检查是否需要滑动窗口压缩上下文
        maybeCompressContext();

        // 如果有下一步提示词，只加入一次，之后清空避免重复
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
            setNextStepPrompt(null);
        }

        // 构建完整 Prompt：消息历史 + 自定义选项（含 proxyToolCalls）
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);

        try {
            // 调用大模型：toolCallbacks 注册工具，options(mutate) 强制覆盖 internalToolExecutionEnabled=false
            // 顺序不能变 —— options() 必须在 toolCallbacks() 之后，否则会被默认值覆盖
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .options(chatOptions.mutate())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, getChatId()))
                    .call()
                    .chatResponse();

            // 保存响应供 act() 使用
            this.toolCallChatResponse = chatResponse;

            // 捕获 API 返回的实际 promptTokens（包含 ChatMemory 注入的历史消息），
            // 作为后续阈值检查的基准值。不依赖 estimateMessageListTokens()，
            // 因为它看不到 advisor 注入的 ChatMemory 消息。
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                var usage = chatResponse.getMetadata().getUsage();
                if (usage.getPromptTokens() != null && usage.getPromptTokens() > 0) {
                    this.lastKnownTotalTokens = usage.getPromptTokens();
                    // 同时记录 messageList 的字符数快照，用于后续计算增量
                    this.lastMessageListChars = countMessageListChars();
                }
            }

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

            // 提取模型回复的文本和工具调用请求
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

            // 打印思考过程和工具选择日志
            log.info(getName() + "的思考: " + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",
                            toolCall.name(),
                            toolCall.arguments())
                    )
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);

            if (toolCallList.isEmpty()) {
                // 模型未选择工具 → 思考完成，将回复加入历史，告诉 step() 无需执行 act()
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // 模型要求调用工具 → 返回 true，由 step() 驱动执行 act()
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题: " + e.getMessage());
            getMessageList().add(
                    new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        // 追加工具执行产生的消息，对超长输出做 LLM 摘要后再存入 messageList
        List<Message> history = toolExecutionResult.conversationHistory();
        getMessageList().addAll(summarizeToolResults(history));

        // 紧急守卫：如果工具执行后上下文超过 120K 安全线，立刻强制压缩，
        // 确保下一次 think() 不会发出超过 128K 的请求
        maybeEmergencyCompress();

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));

        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }
        log.info(results);
        return results;
    }

    // ═══════════════════════════════════════════
    // SSE 流式运行 —— 发送结构化 JSON 事件
    // ═══════════════════════════════════════════
    @Override
    public SseEmitter runStream(String userPrompt) {
        SseEmitter emitter = new SseEmitter(300_000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (getState() != AgentState.IDLE) {
                    sendEvent(emitter, "error", "Agent is not idle: " + getState());
                    emitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sendEvent(emitter, "error", "Prompt cannot be empty");
                    emitter.complete();
                    return;
                }

                setState(AgentState.RUNNING);
                getMessageList().add(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < getMaxSteps() && getState() != AgentState.FINISHED; i++) {
                        setCurrentStep(i + 1);
                        log.info("Executing step {}/{}", getCurrentStep(), getMaxSteps());

                        // ── THINK ──
                        sendEvent(emitter, "thinking", null);

                        boolean hasToolCalls = think();
                        sendTokenUsage(emitter);

                        AssistantMessage msg = toolCallChatResponse.getResult().getOutput();
                        String thought = msg.getText();
                        List<AssistantMessage.ToolCall> toolCalls = msg.getToolCalls();

                        if (!hasToolCalls) {
                            if (thought != null && !thought.isBlank()) {
                                sendEvent(emitter, "done", thought);
                            }
                            setState(AgentState.FINISHED);
                            break;
                        }

                        for (AssistantMessage.ToolCall tc : toolCalls) {
                            JSONObject payload = new JSONObject();
                            payload.set("tool", tc.name());
                            payload.set("args", tc.arguments());
                            sendEvent(emitter, "tool_call", payload.toString());
                        }

                        // ── ACT ──
                        String actResult = act();
                        sendEvent(emitter, "tool_result", actResult);
                        sendTokenUsage(emitter);
                    }

                    if (getCurrentStep() >= getMaxSteps()) {
                        setState(AgentState.FINISHED);
                        sendEvent(emitter, "done", "Maximum steps reached (" + getMaxSteps() + ")");
                    }

                    emitter.complete();
                } catch (Exception e) {
                    setState(AgentState.ERROR);
                    log.error("Agent execution failed", e);
                    try {
                        sendEvent(emitter, "error", e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            setState(AgentState.ERROR);
            this.cleanup();
            log.warn("SSE timeout");
        });

        emitter.onCompletion(() -> {
            if (getState() == AgentState.RUNNING) {
                setState(AgentState.FINISHED);
            }
            this.cleanup();
            log.info("SSE completed");
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String type, String content) {
        try {
            JSONObject event = new JSONObject();
            event.set("type", type);
            if (content != null) {
                event.set("content", content);
            }
            emitter.send(event.toString());
        } catch (Exception e) {
            log.error("Failed to send SSE event [{}]: {}", type, e.getMessage());
        }
    }

    private void sendTokenUsage(SseEmitter emitter) {
        try {
            long maxContext = 128_000L;
            // 优先用 getEstimatedTotalTokens（包含 ChatMemory + messageList 增量）
            long tokens = getEstimatedTotalTokens();

            JSONObject event = new JSONObject();
            event.set("type", "token_usage");
            event.set("step", getCurrentStep());
            event.set("maxSteps", getMaxSteps());
            event.set("tokens", Math.toIntExact(tokens));
            event.set("maxTokens", Math.toIntExact(maxContext));
            emitter.send(event.toString());
        } catch (Exception e) {
            log.error("Failed to send token_usage event: {}", e.getMessage());
        }
    }

    private long countMessageListChars() {
        long chars = 0;
        for (Message m : getMessageList()) {
            String text = m.getText();
            if (text != null) chars += text.length();
        }
        return chars;
    }

    /**
     * 估算当前实际的总 token 数（messageList + ChatMemory + system + tool defs）。
     *
     * 核心逻辑：lastKnownTotalTokens 是 API 在上次 think() 时返回的精确值（包含一切），
     * 自那以后 messageList 增加了 Δ chars 的内容（工具输出等），加上这部分即为当前总量。
     *
     * 如果还没有 API 数据（第一次 think 之前），降级为纯 messageList 估算。
     */
    private long getEstimatedTotalTokens() {
        if (lastKnownTotalTokens <= 0) {
            return estimateMessageListTokens(getMessageList());
        }
        long currentChars = countMessageListChars();
        long deltaChars = Math.max(0, currentChars - lastMessageListChars);
        // 增量用 2.5 chars/token 估算，与 API 的 tokenizer 基本一致
        long deltaTokens = (long)(deltaChars / 2.5);
        return lastKnownTotalTokens + deltaTokens;
    }

    private long estimateMessageListTokens() {
        return estimateMessageListTokens(getMessageList());
    }

    private long estimateMessageListTokens(List<Message> messages) {
        long totalChars = 0;
        for (Message m : messages) {
            String text = m.getText();
            if (text != null) {
                totalChars += text.length();
            }
        }
        return Math.max(1, (long)(totalChars / 2.5));
    }

    // ═══════════════════════════════════════════
    // 滑动窗口压缩方法
    // ═══════════════════════════════════════════

    /**
     * 紧急上下文守卫 —— act() 后调用，与 maybeCompressContext() 的区别：
     *
     * 1. 阈值更高（120K vs 100K），只在真正危险时触发
     * 2. 保留步数更少（2 vs 5），宁可丢信息也不能让下次请求超出 128K
     * 3. 日志用 WARN 级别，方便告警监控
     *
     * 这个方法是最后一道防线。正常情况下 maybeCompressContext()（think 时 100K）
     * 足以控制上下文增长；但工具可能在一步内灌入 50K+ tokens 的输出，
     * 从 90K 直接跳到 150K，此时等不到下次 think() 就需要立刻截断。
     */
    private void maybeEmergencyCompress() {
        List<Message> messages = getMessageList();
        long estimatedTokens = getEstimatedTotalTokens();
        if (estimatedTokens < EMERGENCY_THRESHOLD_TOKENS) return;

        int splitIdx = findSplitIndex(messages, EMERGENCY_KEEP_STEPS);
        if (splitIdx <= 1) return;

        try {
            List<Message> recentMsgs = new ArrayList<>(
                    messages.subList(splitIdx, messages.size()));
            List<Message> oldMsgs = new ArrayList<>(
                    messages.subList(0, splitIdx));

            String summary = buildCompressionSummary(oldMsgs);

            messages.clear();
            messages.add(new UserMessage(summary));
            messages.addAll(recentMsgs);

            // 压缩后更新字符数快照，防止增量计算出错
            this.lastMessageListChars = countMessageListChars();
            // 保守估计：压缩后的实际总 token 为 messageList 估算值 + 安全余量
            this.lastKnownTotalTokens = estimateMessageListTokens(messages) + 5000;

            log.warn("[Emergency Compression] Context spiked to {} tokens — forced "
                    + "compression: {} old messages → summary, kept {} recent. "
                    + "Now ~{} tokens",
                    estimatedTokens, oldMsgs.size(), recentMsgs.size(),
                    lastKnownTotalTokens);
        } catch (Exception e) {
            log.error("Emergency compression failed: {}", e.getMessage(), e);
        }
    }

    private void maybeCompressContext() {
        List<Message> messages = getMessageList();
        if (messages.isEmpty()) return;

        long estimatedTokens = getEstimatedTotalTokens();
        if (estimatedTokens < COMPRESSION_THRESHOLD_TOKENS) return;

        int splitIdx = findSplitIndex(messages, KEEP_RECENT_STEPS);
        if (splitIdx <= 1) {
            log.debug("Context compression skipped: only {} messages, splitIdx={}",
                    messages.size(), splitIdx);
            return;
        }

        try {
            List<Message> recentMsgs = new ArrayList<>(
                    messages.subList(splitIdx, messages.size()));
            List<Message> oldMsgs = new ArrayList<>(
                    messages.subList(0, splitIdx));

            String summary = buildCompressionSummary(oldMsgs);

            messages.clear();
            messages.add(new UserMessage(summary));
            messages.addAll(recentMsgs);

            // 压缩后更新字符数快照，防止增量计算出错
            this.lastMessageListChars = countMessageListChars();
            this.lastKnownTotalTokens = estimateMessageListTokens(messages) + 5000;

            log.info("[Context Compression] {} old messages → {} char summary, kept {} recent messages. Tokens: {} → ~{}",
                    oldMsgs.size(), summary.length(), recentMsgs.size(),
                    estimatedTokens, lastKnownTotalTokens);
        } catch (Exception e) {
            log.error("Context compression failed: {}", e.getMessage(), e);
        }
    }

    private int findSplitIndex(List<Message> messages, int keepSteps) {
        int count = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof AssistantMessage) {
                count++;
                if (count >= keepSteps) return i;
            }
        }
        return messages.size();
    }

    private String buildCompressionSummary(List<Message> oldMessages) {
        StringBuilder sb = new StringBuilder();
        sb.append("[HISTORY COMPRESSION]——以下是之前任务的摘要\n\n");

        // 第 1 层：用户提问（去重）
        sb.append("## 用户提问\n");
        LinkedHashSet<String> seenQuestions = new LinkedHashSet<>();
        for (Message m : oldMessages) {
            if (!(m instanceof UserMessage)) continue;
            String text = m.getText();
            if (text == null || text.isBlank()) continue;
            if (text.startsWith("[HISTORY COMPRESSION]")) continue;
            if (text.startsWith("Based on user needs") || text.startsWith("根据用户需求")) continue;
            if (text.length() > 500) continue;
            if (seenQuestions.add(text)) {
                sb.append("- ").append(text).append("\n");
            }
        }
        if (seenQuestions.isEmpty()) sb.append("- (无明显用户提问)\n");
        sb.append("\n");

        // 第 2 层：工具调用历史
        sb.append("## 已执行的工具\n");
        int toolCount = 0;
        for (int idx = 0; idx < oldMessages.size(); idx++) {
            Message m = oldMessages.get(idx);
            if (m instanceof AssistantMessage am) {
                List<AssistantMessage.ToolCall> toolCalls = am.getToolCalls();
                if (toolCalls == null || toolCalls.isEmpty()) continue;
                for (AssistantMessage.ToolCall tc : toolCalls) {
                    toolCount++;
                    sb.append("- ").append(tc.name());
                    String resultPreview = findToolResult(oldMessages, idx + 1);
                    if (resultPreview != null && resultPreview.contains("Error")) {
                        sb.append(" ✗ (").append(resultPreview).append(")\n");
                    } else {
                        sb.append(" ✓");
                        if (resultPreview != null && !resultPreview.isBlank()) {
                            sb.append(" (").append(resultPreview).append(")");
                        }
                        sb.append("\n");
                    }
                }
            }
        }
        if (toolCount == 0) sb.append("- (无工具调用记录)\n");
        sb.append("\n");

        // 第 3 层：文件路径
        LinkedHashSet<String> seenFiles = new LinkedHashSet<>();
        for (Message m : oldMessages) {
            String text = m.getText();
            if (text == null) continue;
            Matcher matcher = FILE_PATH_PATTERN.matcher(text);
            while (matcher.find()) {
                seenFiles.add(matcher.group());
            }
        }
        if (!seenFiles.isEmpty()) {
            sb.append("## 生成的文件\n");
            for (String filePath : seenFiles) {
                sb.append("- ").append(filePath).append("\n");
            }
            sb.append("\n");
        }

        sb.append("请继续完成上述未完成的任务。");
        return sb.toString();
    }

    private String findToolResult(List<Message> messages, int fromIndex) {
        int end = Math.min(fromIndex + 3, messages.size());
        for (int i = fromIndex; i < end; i++) {
            if (messages.get(i) instanceof ToolResponseMessage trm) {
                for (ToolResponseMessage.ToolResponse r : trm.getResponses()) {
                    String data = r.responseData();
                    if (data != null && !data.isBlank()) {
                        String preview = data.replace('\n', ' ').replace('\r', ' ');
                        return preview.length() > 200
                                ? preview.substring(0, 200) + "..."
                                : preview;
                    }
                }
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════
    // LLM 工具输出摘要方法
    // ═══════════════════════════════════════════

    private List<Message> summarizeToolResults(List<Message> history) {
        List<Message> result = new ArrayList<>(history.size());
        for (Message msg : history) {
            if (!(msg instanceof ToolResponseMessage trm)) {
                result.add(msg);
                continue;
            }

            List<ToolResponseMessage.ToolResponse> originalResponses = trm.getResponses();
            List<ToolResponseMessage.ToolResponse> newResponses = new ArrayList<>();

            for (ToolResponseMessage.ToolResponse response : originalResponses) {
                String toolName = response.name();
                String data = response.responseData();

                if (shouldSummarize(toolName, data)) {
                    log.info("Summarizing tool output: {} ({} chars)", toolName, data.length());
                    String summary = callSummarizer(data);
                    if (summary == null || summary.isBlank()) {
                        log.warn("Summarization failed for {}, falling back to truncation", toolName);
                        summary = data.substring(0, Math.min(data.length(), 2000))
                                + "\n\n[输出过长已截断，原始长度: " + data.length() + " 字符]";
                    }
                    newResponses.add(new ToolResponseMessage.ToolResponse(response.id(), response.name(), summary));
                } else {
                    newResponses.add(response);
                }
            }

            result.add(ToolResponseMessage.builder()
                    .responses(newResponses)
                    .build());
        }
        return result;
    }

    private boolean shouldSummarize(String toolName, String data) {
        if (!SUMMARIZE_TOOLS.contains(toolName)) return false;
        if (data == null || data.isBlank()) return false;
        return data.length() > SUMMARIZE_THRESHOLD_CHARS;
    }

    private String callSummarizer(String rawOutput) {
        // 先提取文件路径，确保摘要后不丢失
        List<String> filePaths = new ArrayList<>();
        Matcher matcher = FILE_PATH_PATTERN.matcher(rawOutput);
        while (matcher.find()) {
            filePaths.add(matcher.group());
        }

        String filesSection = filePaths.isEmpty() ? ""
                : "\n\n文件路径（必须原样保留）：\n" + String.join("\n", filePaths);

        String promptText = """
                你是信息提取器。将以下网页/搜索结果压缩为 3-5 个要点，每条不超过 80 字。
                要求：
                1. 只保留关键事实、数据、结论
                2. 丢弃广告、导航栏、样式、脚本、重复内容
                3. 文件路径必须原样输出（如 tmp/download/xxx.pdf）
                4. 用中文回复，直接给要点不要加"以下是摘要"之类的开头
                %s

                原始内容：
                %s
                """.formatted(filesSection, rawOutput);

        try {
            String summary = summarizeClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            if (summary == null || summary.isBlank()) return null;

            log.info("Summarization: {} chars → {} chars (reduced {}%)",
                    rawOutput.length(), summary.length(),
                    Math.round((1.0 - (double) summary.length() / rawOutput.length()) * 100));

            // 检查摘要是否丢失了文件路径
            for (String path : filePaths) {
                if (!summary.contains(path)) {
                    summary += "\n\n[文件]: " + path;
                }
            }

            return summary;
        } catch (Exception e) {
            log.error("Summarization failed: {}", e.getMessage());
            return null;
        }
    }
}