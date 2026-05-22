package com.nanhua.spring_ai.Controller;

import com.nanhua.spring_ai.repository.ChatHistoryRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@Slf4j
@RequestMapping("/ai")
public class CustomerServiceController {
    @Resource
    private ChatClient serviceClient;
    @Resource
    private ChatHistoryRepository chatHistoryRepository;

    @RequestMapping(value = "/service", produces = "text/html;charset=utf-8")
    public Flux<String> service(
            String prompt,
            @RequestParam(required = false) String chatId) {
        log.info("收到消息 - prompt: {}, chatId: {}", prompt, chatId);

        chatHistoryRepository.save("service", chatId);

        return serviceClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,   // ← 2.0.0-M6 的正确 key
                        chatId
                ))
                .stream()
                .content();
    }
}
