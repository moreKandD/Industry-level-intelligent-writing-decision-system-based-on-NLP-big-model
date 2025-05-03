package com.textouput.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReportResponse {
    private String filename;
    private String reportTitle;
    private LocalDateTime generatedAt = LocalDateTime.now();
}