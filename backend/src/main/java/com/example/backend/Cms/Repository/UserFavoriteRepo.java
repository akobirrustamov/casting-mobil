package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UserFavorite;
import com.example.backend.Cms.Enums.FavoriteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserFavoriteRepo extends JpaRepository<UserFavorite, Long> {

    /**
     * Bitta foydalanuvchining shu turdagi sevimlilari.
     *
     * <h2>⚠️ Faqat identifikatorlar</h2>
     * Klientga ham faqat shular kerak: ekran ijodkorlar ro'yxatini
     * allaqachon boshqa so'rovdan oladi, bu yerdagi vazifa esa
     * «qaysilari belgilangan» degan savolga javob berish.
     *
     * <h2>⚠️ Yangisi YUQORIDA</h2>
     * Odam ro'yxatni ochganda oxirgi qo'shganini qidiradi. Eskisi
     * birinchi bo'lsa uni har safar pastga aylantirishga to'g'ri
     * kelardi.
     *
     * {@code id desc} — ikkinchi mezon: bir necha yozuv bir soniyada
     * qo'shilsa ({@code merge}), tartib tasodifiy bo'lib qolardi va
     * ro'yxat har so'rovda boshqacha ko'rinardi.
     */
    @Query("select f.targetId from UserFavorite f "
            + "where f.user.id = :userId and f.type = :type "
            + "order by f.createdAt desc, f.id desc")
    List<Long> findTargetIds(@Param("userId") UUID userId,
                             @Param("type") FavoriteType type);

    /**
     * Bitta yozuvni o'chiradi.
     *
     * ⚠️ {@code clearAutomatically}: ommaviy {@code delete} Hibernate
     * keshini chetlab o'tadi va o'chirilgan qator ayni tranzaksiyada
     * hali ham mavjud bo'lib ko'rinardi — ro'yxat esa o'chirishdan
     * keyin DARHOL o'qiladi va klientga qaytariladi.
     *
     * @return o'chirilgan qatorlar soni; 0 — bunday sevimli yo'q edi
     */
    @Modifying(clearAutomatically = true)
    @Query("delete from UserFavorite f "
            + "where f.user.id = :userId and f.type = :type and f.targetId = :targetId")
    int deleteOne(@Param("userId") UUID userId,
                  @Param("type") FavoriteType type,
                  @Param("targetId") Long targetId);
}
