package com.expensemanager.approval.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "approvals")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_uuid", nullable = false, length = 36)
    private String expenseUuid;

    @Column(name = "expense_title", nullable = false)
    private String expenseTitle;

    @Column(name = "expense_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expenseAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    // Who submitted the expense
    @Column(name = "submitter_id", nullable = false, length = 36)
    private String submitterId;

    @Column(name = "submitter_email", nullable = false)
    private String submitterEmail;

    @Column(name = "submitter_name", nullable = false)
    private String submitterName;

    @Column(name = "department")
    private String department;

    // Who is assigned to approve
    @Column(name = "approver_id", length = 36)
    private String approverId;

    @Column(name = "approver_email")
    private String approverEmail;

    @Column(name = "approver_name")
    private String approverName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "approval_level", nullable = false)
    @Builder.Default
    private Integer approvalLevel = 1;  // 1=Manager, 2=Finance

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
