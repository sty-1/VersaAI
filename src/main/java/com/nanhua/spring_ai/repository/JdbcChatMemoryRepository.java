// src/main/java/com/nanhua/spring_ai/repository/JdbcChatMemoryRepository.java
package com.nanhua.spring_ai.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nanhua.spring_ai.Entity.po.ChatMessagePo;
import com.nanhua.spring_ai.mapper.ChatMessageMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JdbcChatMemoryRepository implements ChatMemory {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public void add(String chatId, List<Message> messages) {
        List<ChatMessagePo> pos = messages.stream().map(msg -> {
            ChatMessagePo po = new ChatMessagePo();
            po.setChatId(chatId);
            po.setMessageType(msg.getMessageType().name());
            po.setContent(msg.getText());
            return po;
        }).collect(Collectors.toList());

        for (ChatMessagePo po : pos) {
            chatMessageMapper.insert(po);
        }
    }

    @Override
    public List<Message> get(String chatId) {
        LambdaQueryWrapper<ChatMessagePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessagePo::getChatId, chatId)
               .orderByAsc(ChatMessagePo::getCreatedAt);
        List<ChatMessagePo> pos = chatMessageMapper.selectList(wrapper);

        return pos.stream()
                .map(this::toMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String chatId) {
        LambdaQueryWrapper<ChatMessagePo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessagePo::getChatId, chatId);
        chatMessageMapper.delete(wrapper);
    }

    private Message toMessage(ChatMessagePo po) {
        MessageType type = MessageType.valueOf(po.getMessageType());
        return switch (type) {
            case USER -> new UserMessage(po.getContent());
            case ASSISTANT -> new AssistantMessage(po.getContent());
            case SYSTEM -> new SystemMessage(po.getContent());
            default -> new UserMessage(po.getContent()); // 兜底
        };
    }
}
