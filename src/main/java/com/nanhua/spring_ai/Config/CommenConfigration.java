package com.nanhua.spring_ai.Config;

import com.nanhua.spring_ai.prompt.GameClientPrompt;
import com.nanhua.spring_ai.prompt.ServiceClientPrompt;
import com.nanhua.spring_ai.tools.CourseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommenConfigration {



    @Bean
    public ChatClient chatClient(DeepSeekChatModel model, ChatMemory chatMemory) {
        return ChatClient.builder(model)
                .defaultSystem("你是一个编程助手，你最擅长Java，喜欢帮助他人解决编程方面的问题")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatClient gameClient(DeepSeekChatModel model, ChatMemory chatMemory) {
        return ChatClient.builder(model)
                .defaultSystem(GameClientPrompt.PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatClient serviceClient(DeepSeekChatModel model, ChatMemory chatMemory, CourseTools courseTools) {
        return ChatClient.builder(model)
                .defaultSystem(ServiceClientPrompt.PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(courseTools)
                .build();
    }
}
