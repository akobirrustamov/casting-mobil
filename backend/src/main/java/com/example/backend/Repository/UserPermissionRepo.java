package com.example.backend.Repository;

import com.example.backend.Entity.UserPermission;
import com.example.backend.Enums.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface UserPermissionRepo extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findAllByUserId(UUID userId);

    boolean existsByUserIdAndPermission(UUID userId, Permission permission);

    @Transactional
    void deleteAllByUserId(UUID userId);

    @Transactional
    void deleteByUserIdAndPermission(UUID userId, Permission permission);
}
