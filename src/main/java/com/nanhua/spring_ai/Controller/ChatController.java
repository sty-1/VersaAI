package com.nanhua.spring_ai.Controller;

import com.nanhua.spring_ai.repository.ChatHistoryRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequestMapping("/ai")
public class ChatController {
    @Resource
    private ChatClient chatClient;
    @Resource
    private ChatHistoryRepository chatHistoryRepository;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chatStream(
            String prompt,
            @RequestParam(required = false) String chatId) {
        log.info("收到消息 - prompt: {}, chatId: {}", prompt, chatId);

        chatHistoryRepository.save("chat", chatId);

        return chatClient.prompt()
                .user(prompt)
                /**
                 * 配置消息记忆顾问（MessageChatMemoryAdvisor）的会话 ID 参数，
                 * 使顾问能够根据 chatId 自动加载历史消息拼接到 Prompt 中，
                 * 并在 AI 回复后将本轮对话保存，从而实现多轮上下文记忆。
                 *
                 * @param ChatMemory.CONVERSATION_ID 参数键名，Spring AI 2.0.0-M6 版本约定的常量
                 * @param chatId 会话唯一标识，由前端传入，同一会话的多轮消息共享此 ID
                 */
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        chatId
                ))
                .stream()
                .content();
    }
}
