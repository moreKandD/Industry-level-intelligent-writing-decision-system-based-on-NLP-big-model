package com.textouput.backend.config;

import com.textouput.backend.service.DeepSeekService;
import com.textouput.backend.service.TavilyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ApiConfig {
    @Value("${deepseek.api.key}")
    private String deepseekKey;

    @Value("${tavily.api.key}")
    private String tavilyKey;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public DeepSeekService deepSeekService(RestTemplate restTemplate) {
        return new DeepSeekService(restTemplate, deepseekKey);
    }

    @Bean
    public TavilyService tavilyService(RestTemplate restTemplate) {
        return new TavilyService(restTemplate, tavilyKey);
    }
}