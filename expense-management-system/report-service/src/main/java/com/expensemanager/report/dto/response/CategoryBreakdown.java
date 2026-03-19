package com.expensemanager.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CategoryBreakdown {
    private String     categoryName;
    private BigDecimal total;
    private long       count;
    private double     percentage;
}
