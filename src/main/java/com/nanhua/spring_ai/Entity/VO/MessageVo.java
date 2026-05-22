package com.nanhua.spring_ai.Entity.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MessageVo {
    private String role;
    private String content;

    public MessageVo(Message message) {
        switch (message.getMessageType()) {
            case USER:
                this.role = "user";
                break;
            case ASSISTANT:
                this.role = "assistant";
                break;
            default:
                this.role = "unknown";
        }
        this.content = message.getText();
    }
}
