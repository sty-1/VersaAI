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
                我想吃饭我在南华大学，请帮我找到 5 公里内合适的地点，  
                并结合一些网络图片，制定一份详细的吃饭计划，  
                并以 文档 格式输出""";
        String answer = superManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }  
}
