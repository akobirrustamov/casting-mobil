package com.example.backend.Repository;

import com.example.backend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);

    @Query(value = "SELECT u.* FROM users u JOIN users_roles ur ON u.id = ur.user_id JOIN role r ON ur.roles_id = r.id WHERE r.name = 'ROLE_ADMIN'", nativeQuery = true)
    List<User> findAllAdminsByRole();

    @Query(value = "DELETE FROM users_roles WHERE user_id = :id", nativeQuery = true)
    void deleteRole(UUID id);


    @Query(value = "SELECT u.* FROM users u JOIN users_roles ur ON u.id = ur.user_id JOIN role r ON ur.roles_id = r.id WHERE r.name = 'ROLE_DEKAN'", nativeQuery = true)
    List<User> findAllDeanByRoleId();
}
