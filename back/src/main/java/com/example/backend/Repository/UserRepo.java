package com.example.backend.Repository;

import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {
    @Query(value = "select * from users where phone=:phone", nativeQuery = true)
    Optional<User> findByPhone(String phone);
}
