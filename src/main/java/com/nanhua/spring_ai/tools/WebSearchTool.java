package com.nanhua.spring_ai.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(@Value("${search-api.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            log.info("SearchAPI 原始响应: {}", response);

            JSONObject jsonObject = JSONUtil.parseObj(response);

            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                log.warn("未找到搜索结果，响应keys: {}", jsonObject.keySet());
                return "未找到相关搜索结果";
            }

            int count = Math.min(5, organicResults.size());
            List<Object> objects = organicResults.subList(0, count);

            String result = objects.stream().map(obj -> {
                JSONObject tmp = (JSONObject) obj;
                return tmp.toString();
            }).collect(Collectors.joining(","));
            log.info("搜索成功，返回 {} 条结果", count);
            return result;
        } catch (Exception e) {
            log.error("搜索失败", e);
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}