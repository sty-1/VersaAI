// src/main/java/com/nanhua/spring_ai/repository/JdbcChatHistoryRepository.java
package com.nanhua.spring_ai.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nanhua.spring_ai.Entity.po.ChatSessionPo;
import com.nanhua.spring_ai.mapper.ChatSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class JdbcChatHistoryRepository implements ChatHistoryRepository {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Override
    public void save(String type, String chatId) {
        LambdaQueryWrapper<ChatSessionPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionPo::getType, type)
               .eq(ChatSessionPo::getChatId, chatId);
        if (chatSessionMapper.selectCount(wrapper) > 0) {
            return; // 已存在则跳过
        }
        ChatSessionPo po = new ChatSessionPo();
        po.setType(type);
        po.setChatId(chatId);
        chatSessionMapper.insert(po);
    }

    @Override
    public List<String> getAllChatIds(String type) {
        LambdaQueryWrapper<ChatSessionPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionPo::getType, type)
               .orderByAsc(ChatSessionPo::getCreatedAt);
        return chatSessionMapper.selectList(wrapper)
                .stream()
                .map(ChatSessionPo::getChatId)
                .toList();
    }
}
