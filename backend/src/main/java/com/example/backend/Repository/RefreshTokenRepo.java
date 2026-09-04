package com.example.backend.Repository;

import com.example.backend.Entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, UUID> {

    List<RefreshToken> findAllByUserId(UUID userId);

    /**
     * Foydalanuvchining barcha faol tokenlarini bekor qiladi.
     *
     * Ikki holatda ishlatiladi: o'g'irlik aniqlanganda va xodim
     * bloklanganda — aks holda bloklangan admin qo'lidagi token
     * muddati tugaguncha ishlayverardi.
     *
     * <h2>⚠️ {@code clearAutomatically} nima uchun</h2>
     * Bu ommaviy {@code update} — u to'g'ridan-to'g'ri bazaga boradi
     * va Hibernate'ning birinchi darajali keshini CHETLAB o'tadi.
     * Kesh tozalanmasa, AYNI tranzaksiyada oldin o'qilgan token
     * obyekti hali ham «bekor qilinmagan» bo'lib ko'rinadi.
     *
     * Hozirgi chaqiruvchilar bekor qilishdan keyin darhol xato
     * tashlaydi, ya'ni bu jimgina o'tib ketardi. Lekin kafolat
     * chaqiruvchining tranzaksiya chegarasiga BOG'LIQ bo'lmasligi
     * kerak: bu xavfsizlik tekshiruvi, va uni tasodifga qoldirish
     * mumkin emas.
     */
    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now "
            + "where t.userId = :userId and t.revokedAt is null")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Bitta QURILMANING faol tokenlarini bekor qiladi.
     *
     * <h2>⚠️ Nima uchun alohida so'rov</h2>
     * {@code revokeAllForUser} bu yerda yaramaydi: odam o'zining eski
     * telefonini chiqarganda hozir turgan qurilmasidan ham chiqib
     * ketardi. Ya'ni «eskisini o'chiraman» degan harakat o'zini
     * jazolash bo'lib chiqardi.
     *
     * ⚠️ {@code deviceId} bo'yicha tenglik — {@code null} tokenlarga
     * TEGMAYDI. V32 dan oldin berilgan tokenlarda qurilma noma'lum va
     * ularni bu yerda yopib bo'lmaydi; ular {@code rotate} paytida
     * tekshiriladi.
     *
     * {@code clearAutomatically} — {@code revokeAllForUser} dagi bilan
     * bir xil sabab: ommaviy {@code update} birinchi darajali keshni
     * chetlab o'tadi.
     */
    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now "
            + "where t.userId = :userId and t.deviceId = :deviceId and t.revokedAt is null")
    int revokeAllForDevice(@Param("userId") UUID userId,
                           @Param("deviceId") String deviceId,
                           @Param("now") LocalDateTime now);

    /**
     * Muddati o'tganlarni tozalash — jadval cheksiz o'smasin.
     *
     * ⚠️ {@code clearAutomatically} — {@code revokeAllForUser} dagi
     * bilan bir xil sabab: ommaviy {@code delete} keshni chetlab
     * o'tadi va o'chirilgan qator ayni tranzaksiyada hali ham
     * mavjud bo'lib ko'rinadi.
     */
    @Modifying(clearAutomatically = true)
    @Query("delete from RefreshToken t where t.expiresAt < :before")
    int deleteExpired(@Param("before") LocalDateTime before);
}
