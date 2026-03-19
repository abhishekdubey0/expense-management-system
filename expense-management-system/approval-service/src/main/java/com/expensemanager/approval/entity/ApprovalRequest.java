package com.expensemanager.approval.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "approval_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "expense_uuid", nullable = false, length = 36)
    private String expenseUuid;

    @Column(name = "expense_title", nullable = false)
    private String expenseTitle;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "submitter_id",    nullable = false, length = 36)  private String submitterId;
    @Column(name = "submitter_email", nullable = false)                private String submitterEmail;
    @Column(name = "submitter_name",  nullable = false, length = 200)  private String submitterName;

    @Column(length = 100)
    private String department;

    @Column(name = "current_level", nullable = false)
    @Builder.Default
    private Integer currentLevel = 1;  // 1 = Manager, 2 = Finance Admin

    @Column(name = "total_levels", nullable = false)
    @Builder.Default
    private Integer totalLevels = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApprovalAction> actions = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null) this.uuid = UUID.randomUUID().toString();
    }
}
