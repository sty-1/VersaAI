package com.nanhua.spring_ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class WebScrapingTool {

    @Tool(description = "抓取指定 URL 的网页内容，返回纯文本（已去除 HTML 标签）")
    public String scrapeWebPage(@ToolParam(description = "目标网页 URL") String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(8000)
                    .get();
            log.info("网页抓取成功: {}", url);
            return doc.text();  // 纯文本，AI 友好
        } catch (IOException e) {
            log.error("网页抓取失败: url={}", url, e);
            return "抓取失败: " + e.getMessage();
        }
    }
}
