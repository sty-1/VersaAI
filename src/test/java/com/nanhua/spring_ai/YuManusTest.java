package com.nanhua.spring_ai;

import com.nanhua.spring_ai.agent.SuperManus;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class YuManusTest {  
  
    @Resource
    private SuperManus superManus;
  
    @Test
    void run() {  
        String userPrompt = """  
                帮我写一篇关于衡阳的描述""";
        String answer = superManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }  
}
