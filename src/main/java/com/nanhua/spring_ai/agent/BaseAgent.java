package com.nanhua.spring_ai.agent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Select;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

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

















}
