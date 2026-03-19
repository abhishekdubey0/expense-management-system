package com.expensemanager.auth.service;

import com.expensemanager.auth.dto.response.UserResponse;
import com.expensemanager.auth.entity.Role;
import com.expensemanager.auth.entity.User;
import com.expensemanager.auth.exception.ResourceNotFoundException;
import com.expensemanager.auth.repository.UserRepository;
import com.expensemanager.auth.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUserByUuid(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uuid));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable).map(userMapper::toResponse);
    }

    public UserResponse updateRole(String uuid, Role newRole) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uuid));
        user.setRole(newRole);
        User saved = userRepository.save(user);
        log.info("Role updated for user {} to {}", uuid, newRole);
        return userMapper.toResponse(saved);
    }

    public UserResponse assignManager(String userUuid, String managerUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userUuid));
        User manager = userRepository.findByUuid(managerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found: " + managerUuid));

        user.setManager(manager);
        User saved = userRepository.save(user);
        log.info("Manager {} assigned to user {}", managerUuid, userUuid);
        return userMapper.toResponse(saved);
    }

    public void deactivateUser(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uuid));
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", uuid);
    }

    public void activateUser(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uuid));
        user.setActive(true);
        userRepository.save(user);
        log.info("User activated: {}", uuid);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return userMapper.toResponse(user);
    }
}
