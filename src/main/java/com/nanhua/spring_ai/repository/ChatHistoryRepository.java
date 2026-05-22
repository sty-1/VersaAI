package com.nanhua.spring_ai.repository;

import java.util.List;

public interface ChatHistoryRepository {
    void save(String type,String chatId);

    List<String> getAllChatIds(String type);
}
