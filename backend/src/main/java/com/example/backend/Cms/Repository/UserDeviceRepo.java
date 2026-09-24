package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UserDevice;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * Tokenni boshqa qatorlardan olib tashlaydi.
     *
     * Bitta telefonda boshqa odam kirsa, token o'sha qurilmaning yangi
     * qatoriga o'tadi. Eski qatorda qolsa, avvalgi egasiga mo'ljallangan
     * xabar yangi odamning telefoniga chiqardi.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserDevice d set d.pushToken = null "
            + "where d.pushToken = :token and d.id <> :keepId")
    int detachPushToken(@Param("token") String token, @Param("keepId") Long keepId);

    /** Expo «DeviceNotRegistered» degan tokenlar — ilova o'chirilgan. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserDevice d set d.pushToken = null where d.pushToken in :tokens")
    int clearPushTokens(@Param("tokens") Collection<String> tokens);

    /**
     * Push oluvchilar: faol qurilma, token bor.
     *
     * Til va premium hisobdan ({@code UserAccount}) olinadi; hisobi yo'q
     * foydalanuvchi — UZ va premiumsiz. Auditoriya filtri servisda.
     */
    @Query("""
            select d.pushToken as pushToken, a.language as language,
                   a.premiumUntil as premiumUntil, a.status as status
            from UserDevice d
            left join UserAccount a on a.user.id = d.user.id
            where d.active = true and d.pushToken is not null
            """)
    List<PushTarget> findPushTargets();

    interface PushTarget {
        String getPushToken();

        Locale getLanguage();

        LocalDateTime getPremiumUntil();

        UserStatus getStatus();
    }
}
