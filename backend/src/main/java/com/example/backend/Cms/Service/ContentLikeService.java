package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.ContentLike;
import com.example.backend.Cms.Repository.ContentLikeRepo;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * «Yoqdi» — qo'yish, olib tashlash, sanash.
 *
 * <h2>Nima uchun PUT va DELETE, «toggle» emas</h2>
 * Toggle — bu «bor bo'lsa o'chir, yo'q bo'lsa qo'y». Bitta so'rovda u
 * qulay ko'rinadi, lekin so'rov TAKRORLANSA teskarisiga aylanadi. Bu
 * yerda aloqa uzilib qayta yuborilishi odatiy hol: telefon tarmog'i
 * javobni yo'qotadi, klient qayta yuboradi — va odam bosgan «yoqdi»
 * o'z-o'zidan yechilib qoladi.
 *
 * PUT va DELETE esa idempotent: necha marta yuborilsa ham natija bir xil.
 *
 * <h2>Hisoblagich qayerda</h2>
 * Haqiqat manbai — {@code cms_content_like} jadvali. {@code likeCount}
 * o'sha yerdan olingan nusxa: kontent sahifasi har ochilganda
 * {@code count(*)} qilmaslik uchun. Ikkalasi ham bitta tranzaksiyada
 * o'zgaradi, qo'shish esa bazaning o'zida ({@code addLikes}) — aks holda
 * bir vaqtda kelgan ikki so'rovdan bittasi yo'qolardi.
 */
@Service
@RequiredArgsConstructor
public class ContentLikeService {

    private final ContentLikeRepo likeRepo;
    private final ContentRepo contentRepo;

    public record LikeState(boolean liked, long likeCount) {
    }

    /**
     * «Yoqdi» qo'yish. Allaqachon qo'yilgan bo'lsa — hech narsa
     * o'zgarmaydi va bu xato emas.
     */
    @Transactional
    public LikeState like(User user, Long contentId) {
        Content content = alive(contentId);

        if (likeRepo.existsByContentIdAndUserId(contentId, user.getId())) {
            return state(contentId, true);
        }

        try {
            likeRepo.save(ContentLike.builder().content(content).user(user).build());
        } catch (DataIntegrityViolationException duplicate) {
            // ⚠️ Ikki so'rov bir vaqtda keldi: ikkalasi ham yuqorida «yo'q
            // ekan» deb ko'rdi. Unikal cheklov ikkinchisini to'xtatdi —
            // va bu aynan kutilgan natija, xato emas. Sanoq oshirilmaydi.
            return state(contentId, true);
        }

        contentRepo.addLikes(contentId, 1);
        return state(contentId, true);
    }

    /** «Yoqdi» ni olib tashlash. Qo'yilmagan bo'lsa — shunchaki holat. */
    @Transactional
    public LikeState unlike(User user, Long contentId) {
        alive(contentId);

        long removed = likeRepo.deleteByContentIdAndUserId(contentId, user.getId());
        if (removed > 0) {
            contentRepo.addLikes(contentId, -removed);
        }
        return state(contentId, false);
    }

    /**
     * Shu odam bosganmi.
     *
     * ⚠️ Sanoq bu yerda qaytarilmaydi: uni chaqiruvchi allaqachon
     * yuklangan {@code Content} dan oladi. Aks holda kontent sahifasi
     * bitta ekran uchun bazaga ikkinchi marta borardi.
     */
    @Transactional(readOnly = true)
    public boolean isLiked(UUID userId, Long contentId) {
        return userId != null
                && contentId != null
                && likeRepo.existsByContentIdAndUserId(contentId, userId);
    }

    private LikeState state(Long contentId, boolean liked) {
        long count = contentRepo.findById(contentId)
                .map(c -> c.getLikeCount() == null ? 0L : c.getLikeCount())
                .orElse(0L);
        return new LikeState(liked, count);
    }

    /**
     * ⚠️ O'chirilgan kontentga «yoqdi» qo'yib bo'lmaydi.
     *
     * Soft delete bilan yashirilgan kontent ilovada ko'rinmaydi, lekin
     * uning id si eski ekranda qolib ketishi mumkin — masalan odam
     * sahifani ochib turganda admin uni arxivlasa.
     */
    private Content alive(Long contentId) {
        return contentRepo.findById(contentId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Kontent", contentId));
    }
}
