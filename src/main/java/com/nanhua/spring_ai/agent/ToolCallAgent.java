package com.nanhua.spring_ai.agent;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
     * 工具调用管理器，负责执行具体的工具逻辑
     */
    private final ToolCallingManager toolCallingManager;

    /**
     * 自定义 ChatOptions，关闭 internalToolExecutionEnabled 以禁用 Spring AI 自动工具调用，
     * 让 Agent 在 think/act 循环中手动控制工具执行
     */
    private final ToolCallingChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] avaliableTools) {
        super();
        this.availableTools = avaliableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // internalToolExecutionEnabled=false：让模型返回工具调用请求但不自动执行，
        // 由我们自己在 act() 中通过 ToolCallingManager 手动执行
        this.chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .toolCallbacks(Arrays.asList(availableTools))
                .build();
    }

    @Override
    public boolean think() {
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

        // 追加工具执行产生的消息，而非替换，保留上下文
        getMessageList().addAll(toolExecutionResult.conversationHistory());

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
            long tokens = estimateMessageListTokens();

            // 优先用 API 返回的实际 promptTokens（最精确）
            if (toolCallChatResponse != null && toolCallChatResponse.getMetadata() != null) {
                var usage = toolCallChatResponse.getMetadata().getUsage();
                if (usage != null && usage.getPromptTokens() != null) {
                    tokens = usage.getPromptTokens();
                }
            }

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

    private long estimateMessageListTokens() {
        long totalChars = getMessageList().stream()
                .mapToLong(m -> {
                    String text = m.getText();
                    return text != null ? text.length() : 0;
                })
                .sum();
        return totalChars * 4 / 3;
    }
}