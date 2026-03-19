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
public class MonthlyBreakdown {
    private int        year;
    private int        month;
    private String     monthName;
    private BigDecimal total;
    private long       count;
}
