package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepo extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUserId(UUID userId);

    long countByPremiumUntilAfter(LocalDateTime moment);
}
