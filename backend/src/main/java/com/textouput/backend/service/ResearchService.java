package com.textouput.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.textouput.backend.dto.ReportRequest;
import com.textouput.backend.entity.ResearchContext;
import com.textouput.backend.entity.ResearchSection;
import com.textouput.backend.entity.SearchResult;
import com.textouput.backend.exception.DeepSeekException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchService {
    private final DeepSeekService deepSeekService;
    private final TavilyService tavilyService;

    public ResearchContext initializeResearch(ReportRequest request) throws Exception {
        ResearchContext context = new ResearchContext();
        context.setTitle(request.getTheme());
        context.setIndustry(request.getIndustry());
        context.setCreativityLevel(request.getCreativity());

        String structureJson = deepSeekService.generateResearchStructure(request);
        //空值判断
        if (structureJson == null || structureJson.isEmpty()) {
            throw new IllegalArgumentException("生成的报告结构为空");
        }
        List<ResearchSection> sections = parseStructure(structureJson).stream()
                .peek(section -> {
                    // 防御性检查
                    if (section == null) {
                        log.warn("发现空章节项");
                        return;
                    }
                    section.setResearchContext(context); // 安全调用
                })
                .collect(Collectors.toList());
        context.setSections(sections);
        return context;
    }

    public void conductResearch(ResearchContext context) throws ResearchException {
        // 配置并发参数（从配置读取）
        final int parallelism = Math.min(
                context.getSections().size(),
                Runtime.getRuntime().availableProcessors() * 2
        );
        ExecutorService executor = null;

        try {
            // 创建带监控的线程池
            executor = Executors.newWorkStealingPool(parallelism);

            // 创建并行任务列表
            ExecutorService finalExecutor = executor;
            List<CompletableFuture<Void>> futures = context.getSections().stream()
                    .map(section -> CompletableFuture.runAsync(() -> {
                        try {
                            // 带重试机制的研究执行
                            executeWithRetry(section, 3, 1000L);

                            // 创意优化迭代（次数由创造力等级决定）
                            for (int i = 0; i < context.getCreativityLevel(); i++) {
                                refineWithRetry(section, 2, 1500L);
                            }
                        } catch (ResearchException e) {
                            throw new CompletionException(e);
                        }
                    }, finalExecutor))
                    .collect(Collectors.toList());

            // 等待所有任务完成（带超时控制）
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .exceptionally(ex -> {
                        throw new CompletionException("并行执行失败", ex);
                    })
                    .join();

        } catch (CompletionException e) {
            // 解包底层异常
            Throwable rootCause = unwrapException(e);

            // 记录详细错误日志
            log.error("研究过程中断 - 类型: {} 信息: {}",
                    rootCause.getClass().getSimpleName(),
                    rootCause.getMessage(),
                    rootCause);

            // 分类处理已知异常
            if (rootCause instanceof ResearchException) {
                throw (ResearchException) rootCause;
            } else if (rootCause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new ResearchException("研究过程被意外中断", rootCause);
            }

            throw new ResearchException("研究过程发生严重错误: " + rootCause.getMessage(), rootCause);

        } catch (Exception e) {
            // 处理未预期异常
            log.error("未预期错误 - 类型: {} 信息: {}",
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);

            throw new ResearchException("系统内部错误: " + e.getMessage(), e);

        } finally {
            // 安全关闭线程池
            if (executor != null) {
                try {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.warn("线程池未完全关闭");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("线程池关闭被中断", ie);
                }
            }

            // 记录研究完成状态
            log.info("研究上下文 {} 完成状态: {}",
                    context.getTaskId(),
                    context.getSections().stream()
                            .allMatch(s -> s.getRevisionCount() > 0) ? "成功" : "部分失败");
        }
    }

    // 带重试的研究执行
    private void executeWithRetry(ResearchSection section, int maxRetries, long baseDelay) {
        int attempt = 0;
        while (attempt <= maxRetries) {
            try {
                conductSectionResearch(section);
                return;
            } catch (ResearchException e) {
                if (attempt++ >= maxRetries) {
                    throw new CompletionException(e);
                }
                handleRetry(section, "研究", attempt, maxRetries, baseDelay);
            }
        }
    }

    // 带重试的内容优化
    private void refineWithRetry(ResearchSection section, int maxRetries, long baseDelay) {
        int attempt = 0;
        while (attempt <= maxRetries) {
            try {
                refineSectionContent(section);
                return;
            } catch (ResearchException e) {
                if (attempt++ >= maxRetries) {
                    throw new CompletionException(e);
                }
                handleRetry(section, "优化", attempt, maxRetries, baseDelay);
            }
        }
    }

    // 异常解包
    private Throwable unwrapException(Throwable e) {
        while (e instanceof CompletionException && e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

    // 统一重试处理
    private void handleRetry(ResearchSection section, String type,
                             int attempt, int maxRetries, long baseDelay) {
        long delay = (long) (baseDelay * Math.pow(2, attempt));
        log.warn("{}重试 [{}/{}] {}ms后重试 [章节: {}]",
                type,
                attempt,
                maxRetries,
                delay,
                section.getSectionTitle());

        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CompletionException(new ResearchException("操作被中断", ie));
        }
    }

    // 统一异常处理
    private void handleResearchFailure(Throwable cause) throws ResearchException {
        if (cause instanceof ResearchException) {
            throw (ResearchException) cause;
        }
        log.error("研究过程发生未预期错误: {}", cause.getMessage());
        throw new ResearchException("研究失败: " + cause.getMessage(), cause);
    }

    private String generateSectionContent(ResearchSection section, List<SearchResult> results) {
        String searchSummary = results.stream()
                .filter(r -> r.getContent() != null && !r.getContent().isEmpty())
                .map(r -> "#### " + r.getUrl() + "\n> " + r.getContent().substring(0, 200) + "...")
                .collect(Collectors.joining("\n\n"));

        return String.format(
                "**原始提示**: %s\n\n**研究结果**:\n%s\n**AI分析**: %s",
                section.getOriginalPrompt(),
                searchSummary,
                "（待生成）"
        );
    }

    private void conductSectionResearch(ResearchSection section) throws ResearchException {
        final int maxRetries = 3; // 最大重试次数（实际尝试次数为 maxRetries + 1）
        int retryCount = 0;

        while (true) {
            try {
                // 执行Tavily搜索
                List<SearchResult> results = tavilyService.executeSearch(section);

                // 生成章节内容
                String generatedContent = generateSectionContent(section, results);

                // 更新章节数据
                section.setSearchResults(results);
                section.setGeneratedContent(generatedContent);

                // 记录成功日志
                log.debug("章节研究成功 [{}] 搜索结果数: {}",
                        section.getSectionTitle(),
                        results.size());
                return;

            } catch (DeepSeekException e) {
                // DeepSeek专用异常处理
                if (retryCount >= maxRetries) {
                    log.error("深度研究API最终失败 [{}] 原因: {}",
                            section.getSectionTitle(),
                            e.getMessage());
                    throw new ResearchException(
                            String.format("'%s'章节深度研究失败", section.getSectionTitle()),
                            e
                    );
                }

                handleRetry(retryCount, maxRetries, section, e, "DeepSeek");
                retryCount++;

            } catch (Exception e) {
                // 通用异常处理
                if (retryCount >= maxRetries) {
                    log.error("章节研究最终失败 [{}] 原因: {}",
                            section.getSectionTitle(),
                            e.getMessage());
                    throw new ResearchException(
                            String.format("'%s'章节研究失败", section.getSectionTitle()),
                            e
                    );
                }

                handleRetry(retryCount, maxRetries, section, e, "通用");
                retryCount++;
            }
        }
    }

    private void handleRetry(int retryCount, int maxRetries, ResearchSection section,
                             Exception e, String errorType) {
        // 计算指数退避时间（初始1s, 2s, 4s...）
        long backoffMs = (long) (1000 * Math.pow(2, retryCount));

        log.warn("{}错误重试中 ({}/{}), {}ms后重试 [章节: {}] 错误类型: {} - {}",
                errorType,
                retryCount + 1,
                maxRetries,
                backoffMs,
                section.getSectionTitle(),
                e.getClass().getSimpleName(),
                e.getMessage());

        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ResearchException("研究过程被中断", ie);
        }
    }

    //内容优化
    private void refineSectionContent(ResearchSection section) throws ResearchException {
        final int maxRetries = 2; // 最大重试次数（实际尝试次数为 maxRetries + 1）
        int retryCount = 0;

        while (true) {
            try {
                // 生成优化提示词
                String refinementPrompt = createRefinementPrompt(section);

                // 调用DeepSeek服务生成优化内容
                String optimizedContent = deepSeekService.generateContent(refinementPrompt);

                // 更新章节内容
                section.setGeneratedContent(optimizedContent);
                section.setRevisionCount(section.getRevisionCount() + 1);

                // 成功时记录调试日志
                log.debug("章节优化成功 [{}] 修订次数: {}",
                        section.getSectionTitle(),
                        section.getRevisionCount());
                return;

            } catch (DeepSeekException e) {
                // 异常处理逻辑
                if (retryCount >= maxRetries) {
                    log.error("内容优化最终失败 [{}] 原因: {}",
                            section.getSectionTitle(),
                            e.getMessage());
                    throw new ResearchException(
                            String.format("'%s'章节内容优化失败", section.getSectionTitle()),
                            e
                    );
                }

                // 计算退避时间（指数退避算法）
                long backoffTime = (long) (1500 * Math.pow(2, retryCount));
                log.warn("内容优化重试中 ({}/{}), {}ms后重试 [章节: {}] 错误: {}",
                        retryCount + 1,
                        maxRetries,
                        backoffTime,
                        section.getSectionTitle(),
                        e.getMessage());

                try {
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ResearchException("优化过程被中断", ie);
                }

                retryCount++;
            }
        }
    }

    private String createRefinementPrompt(ResearchSection section) {
        return String.format("""
        根据以下研究结果优化内容：
        原始内容：%s
        搜索结果：%s
        要求：提升专业度，补充数据支持
        """, section.getGeneratedContent(),
                section.getSearchResults().stream()
                        .map(SearchResult::getContent)
                        .collect(Collectors.joining("\n")));
    }

    // 解析方法
    public List<ResearchSection> parseStructure(String jsonInput) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String cleanJson = sanitizeJson(jsonInput);
        log.debug("清洗后的JSON：{}", cleanJson);  // 调试日志

        try {
            // JSON结构验证
            JsonNode rootNode = objectMapper.readTree(cleanJson);
            if (!rootNode.isArray()) {
                throw new Exception("报告结构必须是JSON数组格式");
            }

            // 明确指定反序列化目标类型
            return objectMapper.readValue(
                    cleanJson,
                    new TypeReference<List<ResearchSection>>() {
                    } // 修改目标类型
            );
        } catch (JsonProcessingException e) {
            log.error("JSON解析错误详情 | 原始输入: {}", jsonInput); // 添加原始输入日志
            throw new Exception("无效的报告结构，错误详情: " + e.getOriginalMessage());
        }
    }


    // 自定义异常
    public static class ResearchException extends RuntimeException {
        public ResearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private String sanitizeJson(String rawJson) {
        if (rawJson == null) return "[]"; // 返回空数组避免解析失败

        return rawJson
                .replaceAll("^```json\\n*", "")
                .replaceAll("\\n*```$", "")
                .replaceAll("\\\\\"", "\"") // 修复转义引号
                .replaceAll("(?m)^\\s*//.*", ""); // 移除可能存在的注释
    }
}
