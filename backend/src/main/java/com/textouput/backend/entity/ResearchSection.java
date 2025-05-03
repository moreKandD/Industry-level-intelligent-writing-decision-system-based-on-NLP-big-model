package com.textouput.backend.entity;

import lombok.Data;


import java.util.ArrayList;
import java.util.List;

@Data
public class ResearchSection extends Section{
    private String sectionTitle;
    private String originalPrompt;
    private List<SearchResult> searchResults = new ArrayList<>();
    private String generatedContent;
    private int revisionCount;
    private ResearchContext researchContext;
    private List<String> keywords;

    public ResearchContext getResearchContext() {
        if (researchContext == null) {
            throw new IllegalStateException("ResearchSection未关联到上下文");
        }
        return researchContext;
    }

    // 版本控制字段
    private int searchDepth = 1;


}