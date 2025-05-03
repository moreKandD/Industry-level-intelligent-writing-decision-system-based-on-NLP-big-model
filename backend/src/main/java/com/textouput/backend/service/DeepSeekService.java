package com.textouput.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.textouput.backend.dto.ReportRequest;
import com.textouput.backend.exception.DeepSeekException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import com.textouput.backend.exception.DeepSeekException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Slf4j
public class DeepSeekService {
    // API配置常量
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL_NAME = "deepseek-chat";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeepSeekService(RestTemplate restTemplate, String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public String generateResearchStructure(ReportRequest request) throws DeepSeekException {
        String systemPrompt = """
                作为行业研究专家，请根据以下要素生成报告结构：
                - 主标题：${title}
                - 行业领域：${industry}
                - 详细程度：${depth}
                - 创意要求：${creativity}/5
                输出JSON结构：[{"sectionTitle":"章节标题","prompt":"内容生成提示"}]""";

        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt
                                .replace("${title}", request.getTheme())
                                .replace("${industry}", request.getIndustry())
                                .replace("${depth}", request.getLength())
                                .replace("${creativity}", String.valueOf(request.getCreativity()))),
                        Map.of("role", "user", "content", "生成完整的研究报告结构")
                ),
                "temperature", 0.7
        );

        return executeApiCall(requestBody);
    }

    public String generateContent(String userPrompt) throws DeepSeekException {
        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7
        );
        return executeApiCall(requestBody);
    }


    //executeApiCall方法
    private String executeApiCall(Map<String, Object> requestBody) throws DeepSeekException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );
            // 检查HTTP状态码
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = String.format("API请求失败，状态码：%d，响应：%s",
                        response.getStatusCode().value(),
                        response.getBody());
                throw new DeepSeekException(errorMsg);
            }

            return parseResponse(response.getBody());
        } catch (RestClientException e) {
            throw new DeepSeekException("API通信异常：" + e.getMessage(), e);
        }
    }

    private String parseResponse(Map<String, Object> response) throws DeepSeekException {
        try {
            // 防御性校验
            if (response == null) {
                throw new DeepSeekException("API返回空响应");
            }

            // 校验错误响应
            if (response.containsKey("error")) {
                Object error = response.get("error");
                String errorMsg = error instanceof Map ?
                        ((Map<?, ?>) error).get("message").toString() :
                        error.toString();
                throw new DeepSeekException("API返回错误：" + errorMsg);
            }

            // 校验数据结构
            if (!response.containsKey("choices")) {
                throw new DeepSeekException("响应缺少choices字段");
            }

            Object choicesObj = response.get("choices");
            if (!(choicesObj instanceof List)) {
                throw new DeepSeekException("choices字段类型不合法");
            }
            List<?> choices = (List<?>) choicesObj;

            if (choices.isEmpty()) {
                throw new DeepSeekException("choices列表为空");
            }

            Object firstChoice = choices.get(0);
            if (!(firstChoice instanceof Map)) {
                throw new DeepSeekException("choice项格式错误");
            }

            Map<?, ?> choiceMap = (Map<?, ?>) firstChoice;
            if (!choiceMap.containsKey("message")) {
                throw new DeepSeekException("choice缺少message字段");
            }

            Object messageObj = choiceMap.get("message");
            if (!(messageObj instanceof Map)) {
                throw new DeepSeekException("message字段格式错误");
            }

            Map<?, ?> messageMap = (Map<?, ?>) messageObj;
            if (!messageMap.containsKey("content")) {
                throw new DeepSeekException("message缺少content字段");
            }

            Object content = messageMap.get("content");
            if (content instanceof String) {
                return (String) content;
            }
            throw new DeepSeekException("content字段类型不合法");
        } catch (ClassCastException e) {
            throw new DeepSeekException("响应解析类型转换错误", e);
        }
    }
}
