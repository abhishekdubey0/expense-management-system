package com.expensemanager.expense.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateExpenseRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "9999999.99", message = "Amount exceeds maximum allowed")
    @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency = "INR";

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Invalid category ID")
    private Long categoryId;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @Size(max = 50, message = "Project code cannot exceed 50 characters")
    private String projectCode;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
}
