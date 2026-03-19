package com.expensemanager.report.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class SummaryResponse {
    private String     submitterName;
    private String     submitterEmail;
    private BigDecimal totalApproved;
    private BigDecimal totalPending;
    private BigDecimal totalRejected;
    private long       totalCount;
    private String     period;
}
