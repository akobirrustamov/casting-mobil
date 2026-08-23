package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.StaffProfile;
import com.example.backend.Cms.Enums.StaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffProfileRepo extends JpaRepository<StaffProfile, Long> {

    Optional<StaffProfile> findByUserId(UUID userId);

    /** Ro'yxat uchun: bir so'rovda hamma profil — N+1 bo'lmasin. */
    List<StaffProfile> findAllByUserIdIn(Collection<UUID> userIds);

    List<StaffProfile> findAllByStatus(StaffStatus status);
}
