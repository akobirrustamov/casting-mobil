package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceRepo extends JpaRepository<UserDevice, Long> {

    /**
     * Hisob o'chirilganda qurilmalar ro'yxati ham ketadi (13.09.2026).
     *
     * Bu shaxsiy ma'lumot: u yerda odam telefoniga qo'ygan nom turadi
     * («Ali iPhone»). Hisob bilan birga o'chirilmasa, maxfiylik
     * siyosatidagi «shaxsiy ma'lumotlar o'chiriladi» degan va'da
     * yolg'on bo'lardi.
     */
    long deleteByUserId(java.util.UUID userId);

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
