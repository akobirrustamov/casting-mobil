package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceRepo extends JpaRepository<UserDevice, Long> {

    List<UserDevice> findAllByUserIdOrderByLastActiveAtDesc(UUID userId);

    List<UserDevice> findAllByUserIdAndActiveTrueOrderByLastActiveAtAsc(UUID userId);

    Optional<UserDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);

    /**
     * Ro'yxatdagi foydalanuvchilarning faol qurilmalari — bitta so'rovda.
     *
     * Ilgari har bir foydalanuvchi uchun alohida so'rov ketardi: 50 kishilik
     * sahifa 50 ta qo'shimcha so'rov degani edi.
     */
    List<UserDevice> findAllByUserIdInAndActiveTrue(Collection<UUID> userIds);
}
