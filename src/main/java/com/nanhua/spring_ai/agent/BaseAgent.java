package com.nanhua.spring_ai.agent;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public abstract class BaseAgent {
     private String name;
     private String systemPrompt;
     private String nextStepPrompt;

     private AgentState state = AgentState.IDLE;

     //步骤控制
     private int currentStep = 0;
     private int maxSteps = 5;

     //大模型
     private ChatClient chatClient;

     //会话ID，用于 ChatMemory 存取历史记录
     private String chatId;

     //需要自己维护的memory
    private List<Message> messageList=new ArrayList<>();

    public String run(String userPrompt){
        if(this.state!=AgentState.IDLE)
        {
            throw new RuntimeException("Agent is not idle"+this.state);
        }
        if(userPrompt==null)
        {
            throw new RuntimeException("userPrompt is null");
        }
        //执行步骤，先改状态
        this.state=AgentState.RUNNING;
        //将用户prompt变为第一个消息
        messageList.add(new UserMessage(userPrompt));
        //创建结果列表
        List<String> resultList=new ArrayList<>();


        try {
            //开始循环
            for(int i=0;i<maxSteps&&this.state!=AgentState.FINISHED;i++)
            {
                int stepNum=i+1;
                currentStep=stepNum;
                log.info("Agent is running step {}/{}",stepNum,maxSteps);
                //单步执行
                String stepresult=step();
                String result="step"+stepNum+": "+stepresult;
                resultList.add(result);
            }
            //检查是否超出最大步数
            if(currentStep>=maxSteps)
            {
                this.state=AgentState.FINISHED;
                resultList.add("超出最大步数("+maxSteps+")");
            }
            return String.join("\n",resultList);
        } catch (Exception e) {
            state=AgentState.ERROR;
            log.info("Agent is error",e);
            return "Agent is error: "+e.getMessage();
        }finally {
            this.clean();
        }
    }


    public abstract String step();

    //清理资源
    protected void clean(){};

    protected void cleanup() {
        clean();
    }

    /**
     * SSE 流式运行 Agent，通过 SseEmitter 将每一步的执行结果实时推送给客户端。
     * 使用 CompletableFuture 异步执行，避免长时间占用 Web 线程。
     */
    public SseEmitter runStream(String userPrompt) {
        SseEmitter emitter = new SseEmitter(300_000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误：无法从状态运行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    emitter.send("错误：不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }

                state = AgentState.RUNNING;
                messageList.add(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step {}/{}", stepNumber, maxSteps);

                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;
                        emitter.send(result);
                    }

                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }

                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE 连接超时");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE 连接完成");
        });

        return emitter;
    }

















}
