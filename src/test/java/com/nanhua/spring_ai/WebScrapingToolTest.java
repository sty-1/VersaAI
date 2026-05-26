package com.nanhua.spring_ai;

import com.nanhua.spring_ai.tools.WebScrapingTool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class WebScrapingToolTest {

    @Test
    public void testScrapeWebPage() {
        WebScrapingTool tool = new WebScrapingTool();
        String url = "";
        String result = tool.scrapeWebPage(url);
        assertNotNull(result);
    }
}
