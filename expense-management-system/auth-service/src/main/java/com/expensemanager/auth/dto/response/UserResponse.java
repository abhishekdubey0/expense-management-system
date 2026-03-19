package com.expensemanager.auth.dto.response;

import com.expensemanager.auth.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private String uuid;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String department;
    private String employeeId;
    private Role role;
    private boolean isActive;
    private LocalDateTime createdAt;
}
