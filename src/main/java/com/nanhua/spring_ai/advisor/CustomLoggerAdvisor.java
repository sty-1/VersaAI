package com.nanhua.spring_ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

@Slf4j
public class CustomLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    private final int order;

    public CustomLoggerAdvisor() {
        this(0);
    }

    public CustomLoggerAdvisor(int order) {
        this.order = order;
    }

    // ═══════════════════════════════════════════
    // 同步调用拦截 (.call())
    // ═══════════════════════════════════════════
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        printRequest(request);                              // 前置：打印用户问题
        ChatClientResponse response = chain.nextCall(request); // 调用 AI
        printResponse(response);                            // 后置：打印 AI 回复
        return response;
    }

    // ═══════════════════════════════════════════
    // 流式调用拦截 (.stream())
    // ═══════════════════════════════════════════
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        printRequest(request);                              // 前置：打印用户问题
        Flux<ChatClientResponse> responses = chain.nextStream(request);
        // 流式响应需要聚合为完整回复后再打印，避免逐 chunk 刷屏
        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(responses, this::printResponse);
    }

    // ═══════════════════════════════════════════
    // 精简日志输出（使用 INFO 级别，默认生效）
    // ═══════════════════════════════════════════
    private void printRequest(ChatClientRequest request) {
        log.info("【AI 请求】{}", request.context());
    }

    private void printResponse(ChatClientResponse response) {
        if (response.chatResponse() != null
                && response.chatResponse().getResult() != null
                && response.chatResponse().getResult().getOutput() != null) {
            String text = response.chatResponse().getResult().getOutput().getText();
            log.info("【AI 响应】{}", text);
        }
    }

    // ═══════════════════════════════════════════
    // Advisor 元信息
    // ═══════════════════════════════════════════
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}