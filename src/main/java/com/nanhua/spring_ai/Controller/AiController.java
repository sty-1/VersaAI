package com.nanhua.spring_ai.Controller;

import com.nanhua.spring_ai.agent.SuperManus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Slf4j
@RequestMapping("/ai")
public class AiController {

    @Resource
    private ChatModel deepSeekChatModel;

    @Resource
    private ToolCallback[] allTools;

    /**
     * 同步接口：一次性返回 Agent 完整执行结果。
     * 适用场景：简单的短任务；任务耗时较长时体验较差。
     */
    @GetMapping("/manus/chat/sync")
    public String doChatWithManusSync(String message) {
        log.info("同步请求 - message: {}", message);
        SuperManus superManus = new SuperManus(allTools, deepSeekChatModel);
        return superManus.run(message);
    }

    /**
     * SSE 流式接口：将 Agent 每一步的执行结果实时推送给前端。
     * 适用场景：多步推理任务，用户需要看到中间过程。
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        log.info("SSE 流式请求 - message: {}", message);
        SuperManus superManus = new SuperManus(allTools, deepSeekChatModel);
        return superManus.runStream(message);
    }
}
