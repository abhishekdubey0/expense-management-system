package com.expensemanager.auth.repository;

import com.expensemanager.auth.entity.Role;
import com.expensemanager.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(String uuid);

    boolean existsByEmail(String email);

    boolean existsByEmployeeId(String employeeId);

    List<User> findByManagerId(Long managerId);

    Page<User> findByRoleAndIsActive(Role role, boolean isActive, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.department = :dept AND u.isActive = true")
    List<User> findActiveUsersByDepartment(@Param("dept") String department);

    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND u.isActive = true")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
}
