package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepo extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUserId(UUID userId);

    /** Ro'yxat uchun — har bir foydalanuvchiga alohida so'rov ketmasin (N+1). */
    List<UserAccount> findAllByUserIdIn(Collection<UUID> userIds);

    long countByPremiumUntilAfter(LocalDateTime moment);
}
