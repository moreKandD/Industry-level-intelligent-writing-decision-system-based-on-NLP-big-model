package com.textouput.backend.entity;

import com.textouput.backend.dto.ReportRequest;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


    @Data
    public class ResearchContext {
        private String taskId;      // 新增任务ID字段
        private String title;
        private String industry;
        private int creativityLevel;
        private List<ResearchSection> sections = new ArrayList<>();
        private LocalDateTime createTime = LocalDateTime.now();
        private String userMaterial;
        private List<String> materialKeywords;

        // 新增带taskId的构造函数
        public ResearchContext(String taskId, ReportRequest request) {
            this.taskId = taskId;
            this.title = request.getTheme();
            this.industry = request.getIndustry();
            this.creativityLevel = request.getCreativity();
            this.userMaterial = request.getMaterial();
            this.analyzeMaterial(); // 初始化时自动分析素材
        }

        // 保留默认构造函数（必须）
        public ResearchContext() {
        }

        public void analyzeMaterial() {
            if (this.userMaterial == null) return;

            this.materialKeywords = Arrays.stream(userMaterial.split("[，。\\s]+"))
                    .filter(word -> word.length() > 2)
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());
        }

        // 新增转换方法（用于服务层）
        public ReportRequest convertToReportRequest() {
            ReportRequest request = new ReportRequest();
            request.setTheme(this.title);
            request.setIndustry(this.industry);
            request.setCreativity(this.creativityLevel);
            request.setMaterial(this.userMaterial);
            request.setLength(convertResearchDepth());
            return request;
        }

        private String convertResearchDepth() {
            return this.sections.size() > 5 ? "详细" : "标准";
        }
    }

