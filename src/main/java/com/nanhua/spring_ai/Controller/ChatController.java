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
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,   // ← 2.0.0-M6 的正确 key
                        chatId
                ))
                .stream()
                .content();
    }
}
