package com.expensemanager.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportResponse {
    private SummaryResponse summary;
    private List<MonthlyBreakdown> monthlyBreakdown;
    private List<CategoryBreakdown> categoryBreakdown;
    private List<DepartmentBreakdown> departmentBreakdown;
    private List<TopSpender> topSpenders;
    private String generatedAt;
    private String cacheStatus;   // "HIT" or "MISS"
}
