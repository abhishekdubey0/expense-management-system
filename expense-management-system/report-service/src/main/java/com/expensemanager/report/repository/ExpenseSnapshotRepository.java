package com.expensemanager.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseSnapshotRepository extends JpaRepository<ExpenseSnapshot, Long> {

    Optional<ExpenseSnapshot> findByExpenseUuid(String expenseUuid);

    // ─── Summary: total spend by user in date range ───────────
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM ExpenseSnapshot e
        WHERE e.submitterId = :uid
          AND e.status IN ('APPROVED','REIMBURSED')
          AND e.expenseDate BETWEEN :from AND :to
        """)
    BigDecimal sumApprovedByUserAndDateRange(
            @Param("uid") String submitterId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // ─── Monthly breakdown per user ───────────────────────────
    @Query(value = """
        SELECT YEAR(expense_date)  AS year,
               MONTH(expense_date) AS month,
               SUM(amount)         AS total,
               COUNT(*)            AS count
        FROM expense_snapshots
        WHERE submitter_id = :uid
          AND status IN ('APPROVED','REIMBURSED')
          AND expense_date BETWEEN :from AND :to
        GROUP BY YEAR(expense_date), MONTH(expense_date)
        ORDER BY year, month
        """, nativeQuery = true)
    List<Object[]> monthlyBreakdownByUser(
            @Param("uid") String submitterId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // ─── Spending by category ─────────────────────────────────
    @Query("""
        SELECT e.categoryName, SUM(e.amount), COUNT(e)
        FROM ExpenseSnapshot e
        WHERE e.submitterId = :uid
          AND e.status IN ('APPROVED','REIMBURSED')
          AND e.expenseDate BETWEEN :from AND :to
        GROUP BY e.categoryName
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> spendByCategoryForUser(
            @Param("uid") String submitterId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // ─── Department-level report ──────────────────────────────
    @Query("""
        SELECT e.department, SUM(e.amount), COUNT(e)
        FROM ExpenseSnapshot e
        WHERE e.status IN ('APPROVED','REIMBURSED')
          AND e.expenseDate BETWEEN :from AND :to
        GROUP BY e.department
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> spendByDepartment(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // ─── Top spenders ─────────────────────────────────────────
    @Query("""
        SELECT e.submitterName, e.submitterEmail, SUM(e.amount), COUNT(e)
        FROM ExpenseSnapshot e
        WHERE e.status IN ('APPROVED','REIMBURSED')
          AND e.expenseDate BETWEEN :from AND :to
          AND (:dept IS NULL OR e.department = :dept)
        GROUP BY e.submitterId, e.submitterName, e.submitterEmail
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> topSpenders(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("dept") String department);

    // ─── Raw list for CSV export ──────────────────────────────
    @Query("""
        SELECT e FROM ExpenseSnapshot e
        WHERE e.expenseDate BETWEEN :from AND :to
          AND (:dept IS NULL OR e.department = :dept)
          AND (:submitterId IS NULL OR e.submitterId = :submitterId)
          AND (:status IS NULL OR e.status = :status)
        ORDER BY e.expenseDate DESC
        """)
    List<ExpenseSnapshot> findForExport(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("dept") String department,
            @Param("submitterId") String submitterId,
            @Param("status") String status);
}
