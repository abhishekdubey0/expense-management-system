package com.expensemanager.approval.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_actions")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @Column(nullable = false)
    private Integer level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionType action;

    @Column(name = "approver_id",    nullable = false, length = 36)  private String approverId;
    @Column(name = "approver_email", nullable = false)                private String approverEmail;
    @Column(name = "approver_name",  nullable = false, length = 200)  private String approverName;
    @Column(name = "approver_role",  nullable = false, length = 50)   private String approverRole;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreatedDate
    @Column(name = "acted_at", nullable = false, updatable = false)
    private LocalDateTime actedAt;

    public enum ActionType {
        APPROVED, REJECTED, DELEGATED
    }
}
