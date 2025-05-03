package com.textouput.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SearchResult {
    private String query;
    private String url;
    private String content;
    private LocalDateTime searchTime;

    public SearchResult(String query, String url, String content) {
        this.query = query;
        this.url = url;
        this.content = content;
    }


}
