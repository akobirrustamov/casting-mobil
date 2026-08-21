package com.example.backend.Cms.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * «Bu fayl qayerda ishlatilyapti».
 *
 * <h2>Nega kerak</h2>
 * Media 12 xil joydan havola qilinadi: kontent galereyasi, qism videosi,
 * qism eskizi, fasl afishasi, ijodkor surati va muqovasi, kategoriya
 * ikonkasi, reklama (ikki rasm), premyera (rasm va video), bildirishnoma
 * rasmi.
 *
 * Faylni ko'r-ko'rona o'chirish — sahifalarda sinib qolgan rasmlar va
 * o'ynamaydigan videolar demakdir. Foreign key xatosi esa foydalanuvchiga
 * hech narsa tushuntirmaydi.
 *
 * Shuning uchun o'chirishdan oldin shu yerda tekshiriladi va admin
 * AYNAN qayerda ishlatilayotganini ko'radi.
 *
 * <h2>Nega EntityManager</h2>
 * 12 ta jadval uchun 12 ta repozitoriyga metod qo'shish o'rniga, bitta
 * joyda ro'yxat sifatida saqlanadi. Yangi havola paydo bo'lsa — pastdagi
 * jadvalga bitta qator qo'shiladi, boshqa hech narsa o'zgarmaydi.
 */
@Service
public class MediaUsageService {

    /** entity nomi · maydon · foydalanuvchiga ko'rinadigan tavsif */
    private static final String[][] REFERENCES = {
            {"ContentMedia",  "media",     "Kontent galereyasi"},
            {"EpisodeVideo",  "media",     "Qism videosi"},
            {"Episode",       "thumbnail", "Qism eskizi"},
            {"Season",        "poster",    "Fasl afishasi"},
            {"Creator",       "photo",     "Ijodkor surati"},
            {"Creator",       "cover",     "Ijodkor muqovasi"},
            {"Category",      "icon",      "Kategoriya ikonkasi"},
            {"Advertisement", "image",     "Reklama rasmi"},
            {"Advertisement", "mobileImage", "Reklama (mobil) rasmi"},
            {"Premiere",      "image",     "Premyera rasmi"},
            {"Premiere",      "video",     "Premyera videosi"},
            {"Notification",  "image",     "Bildirishnoma rasmi"},
    };

    @PersistenceContext
    private EntityManager em;

    /** Bo'sh ro'yxat = fayl hech qayerda ishlatilmayapti, o'chirsa bo'ladi. */
    @Transactional(readOnly = true)
    public List<Usage> usages(Long mediaId) {
        List<Usage> found = new ArrayList<>();
        for (String[] ref : REFERENCES) {
            String jpql = "select count(e) from " + ref[0] + " e where e." + ref[1] + ".id = :id";
            long count = em.createQuery(jpql, Long.class)
                    .setParameter("id", mediaId)
                    .getSingleResult();
            if (count > 0) {
                found.add(new Usage(ref[2], count));
            }
        }
        return found;
    }

    @Data
    @AllArgsConstructor
    public static class Usage {
        /** Masalan «Qism videosi». */
        private String where;
        private long count;
    }
}
