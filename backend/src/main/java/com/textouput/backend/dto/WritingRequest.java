package com.textouput.backend.dto;

import lombok.Data;

@Data
public class WritingRequest {
    private String theme;
    private String material;
    private String length;
    private int creativity;
    private String industry;
}