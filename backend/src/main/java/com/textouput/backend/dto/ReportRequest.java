package com.textouput.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReportRequest {
    @NotBlank
    private String theme;

    @NotBlank
    private String industry;

    @Min(1) @Max(5)
    private int creativity = 3;

    @Pattern(regexp = "简洁|标准|详细")
    private String length = "标准";

    @NotBlank
    private String material;

    public String getResearchDepth() {
        return switch(this.length) {
            case "简洁" -> "concise";
            case "标准" -> "standard";
            case "详细" -> "detailed";
            default -> "standard";
        };
    }
}
