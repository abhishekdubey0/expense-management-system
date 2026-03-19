package com.expensemanager.auth.controller;

import com.expensemanager.auth.dto.response.ApiResponse;
import com.expensemanager.auth.dto.response.UserResponse;
import com.expensemanager.auth.entity.Role;
import com.expensemanager.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserResponse user = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE_ADMIN')")
    @Operation(summary = "Get user by UUID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUuid(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByUuid(uuid)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE_ADMIN')")
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String search) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        Page<UserResponse> users = (search != null && !search.isBlank())
                ? userService.searchUsers(search, pageable)
                : userService.getAllUsers(pageable);

        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/{uuid}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update user role (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable String uuid,
            @RequestParam Role role) {

        return ResponseEntity.ok(ApiResponse.success(
                "Role updated", userService.updateRole(uuid, role)));
    }

    @PatchMapping("/{uuid}/manager")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE_ADMIN')")
    @Operation(summary = "Assign manager to a user")
    public ResponseEntity<ApiResponse<UserResponse>> assignManager(
            @PathVariable String uuid,
            @RequestParam String managerUuid) {

        return ResponseEntity.ok(ApiResponse.success(
                "Manager assigned", userService.assignManager(uuid, managerUuid)));
    }

    @PatchMapping("/{uuid}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String uuid) {
        userService.deactivateUser(uuid);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", null));
    }

    @PatchMapping("/{uuid}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Activate a user account")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable String uuid) {
        userService.activateUser(uuid);
        return ResponseEntity.ok(ApiResponse.success("User activated", null));
    }
}
