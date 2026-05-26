package com.nanhua.spring_ai;

import com.nanhua.spring_ai.tools.WebSearchTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class WebSearchToolTest {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Test
    public void testSearchWeb() {
        WebSearchTool tool = new WebSearchTool(searchApiKey);
        String query = "";
        String result = tool.searchWeb(query);
        System.out.println("搜索结果: " + result);
        assertNotNull(result);
    }
}