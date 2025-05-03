package com.textouput.backend.utils;

import com.textouput.backend.entity.ResearchContext;
import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class ReportFormatter {
    private static final String TEMPLATE = """
        # ${title}
        **行业**: ${industry}  
        **生成时间**: ${time}
        
        ${sections}
        
        ## 参考资料
        ${references}
        """;

    private String sanitizeContent(String content) {
        return content.replaceAll("<script>.*?</script>", "")
                .replaceAll("\\b(eval|alert)\\b", "");
    }

    public String formatReport(ResearchContext context) {
        String sections = context.getSections().stream()
                .map(s -> "### " + s.getSectionTitle() + "\n" + s.getGeneratedContent())
                .collect(Collectors.joining("\n\n"));

        String references = context.getSections().stream()
                .flatMap(s -> s.getSearchResults().stream())
                .map(r -> {
                    // 正确使用变量名r
                    String url = (r.getUrl() != null) ? r.getUrl() : "未知链接";
                    return "- [" + url + "](" + url + ")";
                })
                .distinct()
                .collect(Collectors.joining("\n"));

        return TEMPLATE
                .replace("${title}", context.getTitle())
                .replace("${industry}", context.getIndustry())
                .replace("${time}", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(context.getCreateTime()))
                .replace("${sections}", sections)
                .replace("${references}", references);
    }
}

