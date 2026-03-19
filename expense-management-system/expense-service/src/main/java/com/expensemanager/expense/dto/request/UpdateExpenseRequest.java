package com.expensemanager.expense.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateExpenseRequest {

    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Size(max = 1000)
    private String description;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "9999999.99", message = "Amount exceeds maximum allowed")
    private BigDecimal amount;

    private String currency;

    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate;

    @Positive(message = "Invalid category ID")
    private Long categoryId;

    private String department;
    private String projectCode;

    @Size(max = 2000)
    private String notes;
}
