package com.nanhua.spring_ai.Controller;

import com.nanhua.spring_ai.Entity.VO.MessageVo;
import com.nanhua.spring_ai.repository.ChatHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/ai/history")
@RestController
@Slf4j
public class ChatHistoryController {
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;


    @Autowired
    private ChatMemory chatMemory;
    @GetMapping("/{type}")
    public List<String> getAllChatIds(@PathVariable String type) {
        return chatHistoryRepository.getAllChatIds(type);
    }

    @GetMapping("/{type}/{chatId}")
    public List<MessageVo> getChatHistoryById(@PathVariable String type, @PathVariable String chatId)
    {
        List<Message> messages = chatMemory.get(chatId);
        if(messages==null)
        {
            return new ArrayList<>();
        }


       return messages.stream().map(message -> new MessageVo(message)).toList();
    }
}
