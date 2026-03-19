package com.expensemanager.report.service;

import com.expensemanager.report.dto.response.*;
import com.expensemanager.report.repository.ExpenseSnapshot;
import com.expensemanager.report.repository.ExpenseSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final ExpenseSnapshotRepository snapshotRepo;

    // ─── Personal summary (with Redis cache) ─────────────────

    @Cacheable(value = "reports-summary", key = "#submitterId + ':' + #from + ':' + #to")
    public ReportResponse getMySummary(String submitterId,
                                        LocalDate from, LocalDate to) {
        log.debug("Cache MISS – generating summary for user {} [{} to {}]",
                submitterId, from, to);

        BigDecimal totalApproved = snapshotRepo.sumApprovedByUserAndDateRange(submitterId, from, to);

        List<MonthlyBreakdown> monthly = snapshotRepo
                .monthlyBreakdownByUser(submitterId, from, to)
                .stream()
                .map(row -> MonthlyBreakdown.builder()
                        .year(((Number) row[0]).intValue())
                        .month(((Number) row[1]).intValue())
                        .monthName(Month.of(((Number) row[1]).intValue())
                                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                        .total((BigDecimal) row[2])
                        .count(((Number) row[3]).longValue())
                        .build())
                .toList();

        List<CategoryBreakdown> categories = buildCategoryBreakdown(
                snapshotRepo.spendByCategoryForUser(submitterId, from, to), totalApproved);

        return ReportResponse.builder()
                .summary(SummaryResponse.builder()
                        .totalApproved(totalApproved)
                        .totalCount(monthly.stream().mapToLong(MonthlyBreakdown::getCount).sum())
                        .period(from + " to " + to)
                        .build())
                .monthlyBreakdown(monthly)
                .categoryBreakdown(categories)
                .generatedAt(LocalDateTime.now().toString())
                .cacheStatus("MISS")
                .build();
    }

    // ─── Department report (Finance/Admin only) ───────────────

    @Cacheable(value = "reports-department", key = "#from + ':' + #to")
    public ReportResponse getDepartmentReport(LocalDate from, LocalDate to) {
        log.debug("Cache MISS – generating department report [{} to {}]", from, to);

        List<DepartmentBreakdown> departments = snapshotRepo.spendByDepartment(from, to)
                .stream()
                .map(row -> DepartmentBreakdown.builder()
                        .department((String) row[0])
                        .total((BigDecimal) row[1])
                        .count(((Number) row[2]).longValue())
                        .build())
                .toList();

        return ReportResponse.builder()
                .departmentBreakdown(departments)
                .generatedAt(LocalDateTime.now().toString())
                .cacheStatus("MISS")
                .build();
    }

    // ─── Top spenders (Finance/Admin only) ───────────────────

    @Cacheable(value = "reports-top-spenders", key = "#from + ':' + #to + ':' + #department")
    public ReportResponse getTopSpenders(LocalDate from, LocalDate to, String department) {
        log.debug("Cache MISS – generating top spenders report");

        List<TopSpender> spenders = snapshotRepo.topSpenders(from, to, department)
                .stream()
                .map(row -> TopSpender.builder()
                        .name((String) row[0])
                        .email((String) row[1])
                        .total((BigDecimal) row[2])
                        .count(((Number) row[3]).longValue())
                        .build())
                .toList();

        return ReportResponse.builder()
                .topSpenders(spenders)
                .generatedAt(LocalDateTime.now().toString())
                .cacheStatus("MISS")
                .build();
    }

    // ─── CSV Export ───────────────────────────────────────────

    public void exportToCsv(PrintWriter writer, LocalDate from, LocalDate to,
                             String department, String submitterId, String status) {
        // CSV header
        writer.println("Expense UUID,Title,Amount,Currency,Date,Category," +
                        "Status,Submitter Name,Submitter Email,Department,Project Code");

        List<ExpenseSnapshot> rows = snapshotRepo.findForExport(from, to, department, submitterId, status);
        for (ExpenseSnapshot s : rows) {
            writer.printf("%s,\"%s\",%s,%s,%s,\"%s\",%s,\"%s\",%s,%s,%s%n",
                    s.getExpenseUuid(),
                    escapeCsv(s.getTitle()),
                    s.getAmount().toPlainString(),
                    s.getCurrency(),
                    s.getExpenseDate(),
                    escapeCsv(s.getCategoryName()),
                    s.getStatus(),
                    escapeCsv(s.getSubmitterName()),
                    s.getSubmitterEmail(),
                    s.getDepartment() != null ? s.getDepartment() : "",
                    s.getProjectCode() != null ? s.getProjectCode() : ""
            );
        }
        log.info("CSV export generated: {} rows", rows.size());
    }

    // ─── Snapshot sync (called via Kafka consumer or internal API) ──

    @Transactional
    @CacheEvict(value = {"reports-summary","reports-department",
                          "reports-category","reports-top-spenders"}, allEntries = true)
    public void upsertSnapshot(ExpenseSnapshot snapshot) {
        snapshotRepo.findByExpenseUuid(snapshot.getExpenseUuid())
                .ifPresentOrElse(
                        existing -> {
                            existing.setStatus(snapshot.getStatus());
                            existing.setApprovedAt(snapshot.getApprovedAt());
                            existing.setAmount(snapshot.getAmount());
                            snapshotRepo.save(existing);
                        },
                        () -> snapshotRepo.save(snapshot)
                );
        log.debug("Snapshot upserted for expense {}", snapshot.getExpenseUuid());
    }

    // ─── Helpers ─────────────────────────────────────────────

    private List<CategoryBreakdown> buildCategoryBreakdown(List<Object[]> rows,
                                                            BigDecimal grandTotal) {
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return rows.stream()
                    .map(r -> CategoryBreakdown.builder()
                            .categoryName((String) r[0])
                            .total((BigDecimal) r[1])
                            .count(((Number) r[2]).longValue())
                            .percentage(0.0)
                            .build())
                    .toList();
        }
        return rows.stream()
                .map(r -> {
                    BigDecimal total = (BigDecimal) r[1];
                    double pct = total.divide(grandTotal, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
                    return CategoryBreakdown.builder()
                            .categoryName((String) r[0])
                            .total(total)
                            .count(((Number) r[2]).longValue())
                            .percentage(pct)
                            .build();
                })
                .toList();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
