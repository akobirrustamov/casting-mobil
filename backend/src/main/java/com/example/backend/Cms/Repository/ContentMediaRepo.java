package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.ContentMedia;
import com.example.backend.Cms.Enums.MediaRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

/**
 * Kontent media bog'lanishlari.
 *
 * Asosiy ehtiyoj — TESKARI savol: «bu media fayl qaysi kontentga tegishli
 * va foydalanuvchi uni olishga haqlimi»
 * ({@link com.example.backend.Cms.Service.AccessService#canReadMedia}).
 */
public interface ContentMediaRepo extends JpaRepository<ContentMedia, Long> {

    /**
     * Shu media faylni ASOSIY video sifatida ishlatadigan birinchi bog'lanish.
     *
     * Rol bo'yicha filtr SHART: bitta fayl afisha (POSTER) sifatida ham
     * ishlatilishi mumkin, u esa ochiq. Faqat {@code VIDEO} roli
     * entitlement talab qiladi.
     */
    Optional<ContentMedia> findFirstByMediaIdAndRole(Long mediaId, MediaRole role);

    /**
     * Shu faylni REKLAMA roligi sifatida ishlatadigan birinchi bog'lanish.
     *
     * TRAILER va TEASER birga so'raladi: ular bir xil maqsadda —
     * sotib olmagan odamga ko'rsatish uchun. Ikkita alohida so'rov
     * o'rniga bittasi, chunki chaqiruvchi uchun ular ajratilmaydi.
     */
    Optional<ContentMedia> findFirstByMediaIdAndRoleIn(Long mediaId,
                                                       Collection<MediaRole> roles);
}
