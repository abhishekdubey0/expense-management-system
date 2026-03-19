package com.expensemanager.approval.repository;

import com.expensemanager.approval.entity.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    List<ApprovalAction> findByApprovalRequestIdOrderByActedAtAsc(Long approvalRequestId);
}
