package com.textouput.backend.controller;

import com.textouput.backend.dto.ReportRequest;
import com.textouput.backend.dto.WritingRequest;
import com.textouput.backend.entity.ResearchContext;
import com.textouput.backend.exception.DeepSeekException;
import com.textouput.backend.service.ResearchService;
import com.textouput.backend.utils.ReportFormatter;
import com.textouput.backend.utils.RequestAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class WritingController {

    @Autowired
    private ResearchService researchService;  // 替换MarkdownService

    @Autowired
    private ReportFormatter reportFormatter;  // 新增依赖

    // 配置输出目录（默认值：系统临时目录）
    @Value("${app.file.output-dir:}")
    private String outputDir;

    // 下载接口
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName) throws IOException {

        // 获取文件路径（使用统一路径方法）
        Path filePath = getFilePath(fileName);

        if (!Files.exists(filePath)) {
            log.error("文件不存在: {}", filePath);
            throw new FileNotFoundException("文件不存在");
        }

        Resource resource = new FileSystemResource(filePath.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(resource);
    }

    @PostMapping("/generate")
    public Map<String, String> generateDocument (
            @RequestBody WritingRequest request) throws Exception,DeepSeekException, ResearchService.ResearchException {  // 增加异常声明

        // 转换请求格式
        ReportRequest reportRequest = RequestAdapter.convert(request);

        // 初始化研究上下文
        ResearchContext context = researchService.initializeResearch(reportRequest);
        context.setUserMaterial(reportRequest.getMaterial());  // 设置用户素材
        context.analyzeMaterial();  // 分析素材关键词

        // 执行深度研究（核心逻辑）
        researchService.conductResearch(context);

        // 格式化报告内容
        String content = reportFormatter.formatReport(context);

        // 生成文件名
        String fileName = "research_" + System.currentTimeMillis() + ".md";
        Path filePath = getFilePath(fileName);

        // 保存文件
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);

        log.info("深度研究文档生成成功：{}", filePath);

        return Collections.singletonMap("fileName", fileName);
    }

    /**
     * 统一获取文件路径（解决路径硬编码问题）
     */
    private Path getFilePath(String fileName) {
        // 优先使用配置目录
        if (!outputDir.isBlank()) {
            return Paths.get(outputDir, fileName);
        }

        // 默认路径：系统临时目录 + 项目子目录
        String defaultDir = System.getProperty("java.io.tmpdir") + "/my-app/files";
        return Paths.get(defaultDir, fileName);
    }
}