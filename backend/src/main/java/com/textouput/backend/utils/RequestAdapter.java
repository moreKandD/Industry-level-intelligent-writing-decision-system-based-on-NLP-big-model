package com.textouput.backend.utils;

import com.textouput.backend.dto.ReportRequest;
import com.textouput.backend.dto.WritingRequest;

public class RequestAdapter {
    public static ReportRequest convert(WritingRequest request) {
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setTheme(request.getTheme());
        reportRequest.setIndustry(request.getIndustry());
        reportRequest.setCreativity(request.getCreativity());
        reportRequest.setLength(convertLength(request.getLength()));
        reportRequest.setMaterial(request.getMaterial()); // 确保传递素材字段
        return reportRequest;
    }

    private static String convertLength(String length) {
        return switch(length) {
            case "简洁模式" -> "简洁";
            case "标准模式" -> "标准";
            case "详细模式" -> "详细";
            default -> "标准";
        };
    }
}
