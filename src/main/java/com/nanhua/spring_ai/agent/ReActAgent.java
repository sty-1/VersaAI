package com.nanhua.spring_ai.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {
    public abstract boolean think();

    public abstract String act();

    @Override
    public String step() {
        try {
            boolean thinkResult=think();
            if(!thinkResult)
            {
                return "思考完成，无需行动";
            }
            return act();
        } catch (Exception e) {
            return "执行错误：" + e.getMessage();
        }
    }
}
