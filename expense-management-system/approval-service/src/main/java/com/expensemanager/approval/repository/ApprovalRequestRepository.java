package com.expensemanager.approval.repository;

import com.expensemanager.approval.entity.ApprovalRequest;
import com.expensemanager.approval.entity.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    Optional<ApprovalRequest> findByExpenseUuid(String expenseUuid);

    Optional<ApprovalRequest> findByUuid(String uuid);

    // All pending requests — for finance admins
    Page<ApprovalRequest> findByStatusOrderByCreatedAtAsc(ApprovalStatus status, Pageable pageable);

    // Pending for a specific approver's department
    @Query("""
        SELECT ar FROM ApprovalRequest ar
        WHERE ar.status = 'PENDING'
          AND ar.currentLevel = :level
          AND (:department IS NULL OR ar.department = :department)
        ORDER BY ar.createdAt ASC
        """)
    Page<ApprovalRequest> findPendingByLevelAndDepartment(
            @Param("level") int level,
            @Param("department") String department,
            Pageable pageable);

    // History for a submitter
    Page<ApprovalRequest> findBySubmitterIdOrderByCreatedAtDesc(String submitterId, Pageable pageable);

    boolean existsByExpenseUuidAndStatus(String expenseUuid, ApprovalStatus status);
}
