package com.textouput.backend.ExceptionHandler;

import com.textouput.backend.service.ResearchService;
import com.textouput.backend.exception.DeepSeekException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.io.FileNotFoundException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DeepSeekException.class) // 移除了DeepSeekService前缀
    public ResponseEntity<String> handleDeepSeekException(DeepSeekException ex) {
        log.error("DeepSeek服务异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("AI服务暂时不可用: " + ex.getMessage());
    }
    // 研究异常处理
    @ExceptionHandler(ResearchService.ResearchException.class)
    public ResponseEntity<String> handleResearchException(ResearchService.ResearchException ex) {
        log.error("研究过程异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("文档生成失败: " + ex.getMessage());
    }

    // 增强文件异常处理
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<String> handleFileNotFound(FileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("研究文档未找到: " + ex.getMessage());
    }

}
