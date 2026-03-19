package com.expensemanager.auth.util;

import com.expensemanager.auth.dto.response.UserResponse;
import com.expensemanager.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .employeeId(user.getEmployeeId())
                .role(user.getRole().name())
                .managerId(user.getManager() != null ? user.getManager().getId() : null)
                .managerName(user.getManager() != null ? user.getManager().getFullName() : null)
                .active(user.isActive())
                .verified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
