package com.expensemanager.expense.controller;

import com.expensemanager.expense.dto.response.ApiResponse;
import com.expensemanager.expense.dto.response.CategoryResponse;
import com.expensemanager.expense.entity.Category;
import com.expensemanager.expense.exception.ExpenseException;
import com.expensemanager.expense.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Expense category endpoints")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    @Operation(summary = "Get all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> categories = categoryRepository.findByActiveTrue()
                .stream()
                .map(c -> CategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .icon(c.getIcon())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", categories));
    }

    @PostMapping
    @Operation(summary = "Create a new category (SUPER_ADMIN only)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestParam @NotBlank String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon) {

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ExpenseException("Category already exists: " + name);
        }
        Category category = Category.builder()
                .name(name).description(description).icon(icon).build();
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success("Category created",
                CategoryResponse.builder()
                        .id(saved.getId()).name(saved.getName())
                        .description(saved.getDescription()).icon(saved.getIcon())
                        .build()));
    }
}
