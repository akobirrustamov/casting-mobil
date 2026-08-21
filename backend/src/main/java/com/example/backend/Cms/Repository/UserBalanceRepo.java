package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserBalanceRepo extends JpaRepository<UserBalance, Long> {
    Optional<UserBalance> findByUserId(UUID userId);
}
