package com.expensemanager.report.controller;

import com.expensemanager.report.dto.response.ApiResponse;
import com.expensemanager.report.dto.response.ReportResponse;
import com.expensemanager.report.service.ReportService;
import com.expensemanager.report.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Analytics & reporting endpoints (Redis cached)")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/v1/reports/my-summary
     * Employee's own monthly summary — cached 30 min in Redis.
     */
    @GetMapping("/my-summary")
    @Operation(summary = "Get personal expense summary (cached)")
    public ResponseEntity<ApiResponse<ReportResponse>> getMySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate to) {

        // Default: current month
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = to   != null ? to   : LocalDate.now();

        SecurityUtils.UserContext ctx = SecurityUtils.buildContext();
        ReportResponse report = reportService.getMySummary(ctx.uuid(), start, end);
        return ResponseEntity.ok(ApiResponse.success("Summary generated", report));
    }

    /**
     * GET /api/v1/reports/department
     * Department-level spend breakdown — Finance/Admin only.
     */
    @GetMapping("/department")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Department spend breakdown (Finance Admin only)")
    public ResponseEntity<ApiResponse<ReportResponse>> getDepartmentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate to) {

        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = to   != null ? to   : LocalDate.now();

        ReportResponse report = reportService.getDepartmentReport(start, end);
        return ResponseEntity.ok(ApiResponse.success("Department report generated", report));
    }

    /**
     * GET /api/v1/reports/top-spenders
     * Top spenders across the org — Finance/Admin only.
     */
    @GetMapping("/top-spenders")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Top spenders report (Finance Admin only)")
    public ResponseEntity<ApiResponse<ReportResponse>> getTopSpenders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate to,
            @RequestParam(required = false) String department) {

        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = to   != null ? to   : LocalDate.now();

        ReportResponse report = reportService.getTopSpenders(start, end, department);
        return ResponseEntity.ok(ApiResponse.success("Top spenders report generated", report));
    }

    /**
     * GET /api/v1/reports/export?format=csv
     * CSV export — downloads directly to browser.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN','SUPER_ADMIN','MANAGER')")
    @Operation(summary = "Export expenses as CSV")
    public void exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate to,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String submitterId,
            @RequestParam(required = false) String status,
            HttpServletResponse response) throws IOException {

        LocalDate start = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate end   = to   != null ? to   : LocalDate.now();

        String filename = "expenses_" + start + "_to_" + end + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding("UTF-8");

        reportService.exportToCsv(response.getWriter(), start, end,
                department, submitterId, status);
    }
}
