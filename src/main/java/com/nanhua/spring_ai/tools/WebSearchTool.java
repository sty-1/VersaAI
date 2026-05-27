package com.nanhua.spring_ai.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WebSearchTool {

    private static final int TIMEOUT_MS = 15_000;
    private static final int MAX_RESULTS = 5;

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    @Tool(description = """
            Search the web for information. Returns clean structured search results.
            Use this tool whenever you need up-to-date information that you don't already know.
            """)
    public String searchWeb(
            @ToolParam(description = "Search query keyword or question") String query) {

        // Bing 在国内可直接访问，作为主力引擎
        try {
            return searchBing(query);
        } catch (Exception e) {
            log.warn("Bing 搜索失败: {}", e.getMessage());
        }

        // 备用：百度（国内最快，但偶有验证码拦截）
        try {
            return searchBaidu(query);
        } catch (Exception ex) {
            log.warn("百度搜索失败: {}", ex.getMessage());
        }

        return "搜索失败：所有搜索引擎均不可用，请稍后重试";
    }

    // ═══════════════════════════════════════════
    // Bing 搜索（国内可访问）
    // ═══════════════════════════════════════════
    private String searchBing(String query) throws Exception {
        String url = "https://www.bing.com/search?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&setlang=zh-Hans&count=" + MAX_RESULTS;

        Document doc = Jsoup.connect(url)
                .userAgent(UA)
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(TIMEOUT_MS)
                .get();

        Elements items = doc.select("li.b_algo");
        List<JSONObject> results = new ArrayList<>();

        for (Element item : items) {
            Element linkEl = item.selectFirst("h2 a");
            Element snippetEl = item.selectFirst(".b_caption p, .b_lineclamp2, p");

            if (linkEl == null) continue;

            JSONObject obj = new JSONObject();
            obj.set("title", linkEl.text());
            obj.set("url", linkEl.attr("href"));
            obj.set("snippet", snippetEl != null ? snippetEl.text() : "");
            results.add(obj);

            if (results.size() >= MAX_RESULTS) break;
        }

        if (results.isEmpty()) {
            return "Bing 未找到与\"" + query + "\"相关的搜索结果";
        }

        log.info("Bing 搜索成功，返回 {} 条结果", results.size());
        return JSONUtil.toJsonStr(results);
    }

    // ═══════════════════════════════════════════
    // 百度搜索（备用，国内引擎）
    // ═══════════════════════════════════════════
    private String searchBaidu(String query) throws Exception {
        String url = "https://www.baidu.com/s?wd="
                + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&rn=" + MAX_RESULTS;

        Document doc = Jsoup.connect(url)
                .userAgent(UA)
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(TIMEOUT_MS)
                .get();

        Elements items = doc.select("div.result.c-container, div.c-container");
        List<JSONObject> results = new ArrayList<>();

        for (Element item : items) {
            Element linkEl = item.selectFirst("h3 a, .t a");
            Element snippetEl = item.selectFirst(".c-abstract, .c-span-last, .content-right_8Zs40");

            if (linkEl == null) continue;

            JSONObject obj = new JSONObject();
            obj.set("title", linkEl.text());
            obj.set("url", linkEl.attr("href"));
            obj.set("snippet", snippetEl != null ? snippetEl.text() : "");
            results.add(obj);

            if (results.size() >= MAX_RESULTS) break;
        }

        if (results.isEmpty()) {
            return "百度未找到与\"" + query + "\"相关的搜索结果";
        }

        log.info("百度搜索成功，返回 {} 条结果", results.size());
        return JSONUtil.toJsonStr(results);
    }
}
