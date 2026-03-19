package com.expensemanager.report.repository;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_snapshots")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_uuid", nullable = false, unique = true, length = 36)
    private String expenseUuid;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "submitter_id",    nullable = false, length = 36)  private String submitterId;
    @Column(name = "submitter_email", nullable = false)                private String submitterEmail;
    @Column(name = "submitter_name",  nullable = false, length = 200)  private String submitterName;

    @Column(length = 100) private String department;
    @Column(name = "project_code", length = 50) private String projectCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
