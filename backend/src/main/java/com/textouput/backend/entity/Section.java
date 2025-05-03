package com.textouput.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data // Lombok注解
@JsonIgnoreProperties(ignoreUnknown = true)
public class Section {
    private String sectionTitle;
    private String prompt;

    // 防御性空值处理
    public String getSectionTitle() {
        return sectionTitle != null ? sectionTitle : "";
    }
}