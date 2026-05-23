package com.nanhua.spring_ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringAiApplicationTests {

    @Test
    void contextLoads() {
        // 打印环境变量，看看 IDEA 是否能读到
        System.out.println("DEEPSEEK_KEY = " + System.getenv("DEEPSEEK_API_KEY"));
        System.out.println("BAILIAN_KEY = " + System.getenv("BAILIAN_API_KEY"));
    }

}
