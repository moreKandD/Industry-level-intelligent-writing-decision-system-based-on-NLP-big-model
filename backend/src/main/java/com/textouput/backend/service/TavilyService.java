package com.textouput.backend.service;

import ch.qos.logback.classic.Logger;
import com.textouput.backend.entity.ResearchContext;
import com.textouput.backend.entity.ResearchSection;
import com.textouput.backend.entity.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TavilyService {
    private static final String SEARCH_API = "https://api.tavily.com/search";
    private final RestTemplate restTemplate;
    private final String apiKey;

    public TavilyService(RestTemplate restTemplate, String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public List<SearchResult> executeSearch(ResearchSection section) throws Exception {
        ResearchContext context = section.getResearchContext();
        String query = generateSearchQuery(section);
        int maxResults = calculateMaxResults(section);

        // 调整搜索参数
        Map<String, Object> request = Map.of(
                "api_key", apiKey,
                "query", query,
                "max_results", 5 + (section.getRevisionCount() * 2),
                "include_raw_content", true,
                "search_depth", "advanced"
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    SEARCH_API,
                    new HttpEntity<>(request),
                    Map.class
            );

            return parseResults(response.getBody());
        } catch (RestClientException e) {
            throw new Exception("网络搜索失败", e);
        }
    }
    private String generateSearchQuery(ResearchSection section) {
        ResearchContext context = section.getResearchContext();
        return String.format(
                "%s 行业:%s 素材关键词:%s",
                section.getSectionTitle(),
                context.getIndustry(),
                String.join("|", context.getMaterialKeywords())
        );
    }

    private int calculateMaxResults(ResearchSection section) {
        return 3 + section.getRevisionCount();
    }

    private List<SearchResult> parseResults(Map<String, Object> response) {
        // 1. 安全获取results列表
        Object resultsObj = response.get("results");
        if (!(resultsObj instanceof List<?>)) {
            throw new IllegalStateException("'results'字段类型不合法");
        }

        // 2. 类型安全转换
        List<?> rawResults = (List<?>) resultsObj;
        return rawResults.stream()
                .filter(result -> result instanceof Map<?, ?>)
                .map(result -> (Map<String, Object>) result)
                .map(result -> {
                    try {
                        // 3. 安全获取query参数
                        String query = Optional.ofNullable(response.get("query"))
                                .map(Object::toString)
                                .orElse("");

                        // 4. 安全处理raw_content
                        String rawContent = Optional.ofNullable(result.get("raw_content"))
                                .map(Object::toString)
                                .orElse("");

                        // 5. 安全截取子字符串
                        int length = Math.min(rawContent.length(), 1000);
                        String safeContent = rawContent.substring(0, length);

                        return new SearchResult(
                                query,
                                Optional.ofNullable(result.get("url")).map(Object::toString).orElse(""),
                                safeContent
                        );
                    } catch (Exception e) {
                        // 6. 异常处理
                        log.error("解析结果失败: {}", result, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
