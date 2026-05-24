package com.nanhua.spring_ai.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

@SuppressWarnings("all")
public class ReReadingAdvisor implements BaseAdvisor {

    private static final String DEFAULT_RE2_ADVISE_TEMPLATE = """
            {re2_input_query}
            Read the question again: {re2_input_query}
            """;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // 拿到之前的提示词
        String userPrompt = chatClientRequest.prompt().getUserMessage().getText();
        String re2InputQuery = PromptTemplate.builder().template(DEFAULT_RE2_ADVISE_TEMPLATE)
                .variables(Map.of("re2_input_query", userPrompt))
                .build().render();
        return chatClientRequest.mutate() // 复制一个新的chatClientRequest
                .prompt(chatClientRequest.prompt().augmentUserMessage(re2InputQuery)).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }

}