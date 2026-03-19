package com.expensemanager.expense.repository;

import com.expensemanager.expense.entity.ExpenseAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseAuditLogRepository extends JpaRepository<ExpenseAuditLog, Long> {
    List<ExpenseAuditLog> findByExpenseIdOrderByCreatedAtAsc(Long expenseId);
}
