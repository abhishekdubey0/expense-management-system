package com.expensemanager.expense.repository;

import com.expensemanager.expense.entity.Expense;
import com.expensemanager.expense.entity.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>,
        JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByUuidAndDeletedFalse(String uuid);

    Page<Expense> findBySubmitterIdAndDeletedFalse(String submitterId, Pageable pageable);

    Page<Expense> findBySubmitterIdAndStatusAndDeletedFalse(
            String submitterId, ExpenseStatus status, Pageable pageable);

    Page<Expense> findByStatusAndDeletedFalse(ExpenseStatus status, Pageable pageable);

    Page<Expense> findByDepartmentAndDeletedFalse(String department, Pageable pageable);

    // Manager: all pending expenses for their team
    @Query("""
        SELECT e FROM Expense e
        WHERE e.submitterId IN :submitterIds
          AND e.status = :status
          AND e.deleted = false
        ORDER BY e.submittedAt ASC
        """)
    Page<Expense> findBySubmitterIdsAndStatus(
            @Param("submitterIds") java.util.List<String> submitterIds,
            @Param("status") ExpenseStatus status,
            Pageable pageable);

    // Report queries
    @Query("""
        SELECT SUM(e.amount) FROM Expense e
        WHERE e.submitterId = :submitterId
          AND e.status IN ('APPROVED', 'REIMBURSED')
          AND e.expenseDate BETWEEN :from AND :to
          AND e.deleted = false
        """)
    BigDecimal sumApprovedBySubmitterAndDateRange(
            @Param("submitterId") String submitterId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.deleted = false
          AND (:status IS NULL OR e.status = :status)
          AND (:submitterId IS NULL OR e.submitterId = :submitterId)
          AND (:department IS NULL OR e.department = :department)
          AND (:categoryId IS NULL OR e.category.id = :categoryId)
          AND (:from IS NULL OR e.expenseDate >= :from)
          AND (:to IS NULL OR e.expenseDate <= :to)
        """)
    Page<Expense> findWithFilters(
            @Param("status") ExpenseStatus status,
            @Param("submitterId") String submitterId,
            @Param("department") String department,
            @Param("categoryId") Long categoryId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    // Full-text search
    @Query("""
        SELECT e FROM Expense e
        WHERE e.deleted = false
          AND e.submitterId = :submitterId
          AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(e.notes) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Expense> searchBySubmitter(
            @Param("submitterId") String submitterId,
            @Param("query") String query,
            Pageable pageable);

    boolean existsByUuidAndSubmitterIdAndDeletedFalse(String uuid, String submitterId);
}
