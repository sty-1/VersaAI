package com.nanhua.spring_ai;

import com.nanhua.spring_ai.tools.WebSearchTool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WebSearchToolTest {

    @Test
    public void testSearchWeb() {
        WebSearchTool tool = new WebSearchTool();
        String query = "南华大学";
        String result = tool.searchWeb(query);
        System.out.println("搜索结果: " + result);
        assertNotNull(result);
    }
}
