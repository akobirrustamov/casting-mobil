package com.example.backend.Cms.Service.Storage;

import com.example.backend.Cms.Service.LocalStorageService;
import com.example.backend.Cms.Service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ikki ombor orasida yo'naltiruvchi.
 *
 * <h2>⚠️ Nega bu kerak — jimgina yo'qolgan fayllar</h2>
 * S3 yoqilganda barcha murojaatlar unga ketsa, ILGARI yuklangan fayllar
 * ochilmay qolardi: ular lokal diskda yotibdi, S3 da esa umuman yo'q.
 * Baza yozuvlari joyida, afishalar va videolar esa «topilmadi» beradi —
 * ya'ni ma'lumot yo'qolmaydi, lekin butun kutubxona ishlamay qoladi.
 *
 * Shuning uchun:
 *
 * <pre>
 *   YOZISH  →  har doim S3        (yangi fayllar faqat u yerda)
 *   O'QISH  →  lokalda bormi?  ha → lokal
 *                              yo'q → S3
 * </pre>
 *
 * <h2>Nega lokal AVVAL tekshiriladi</h2>
 * Lokal tekshiruv — fayl tizimining {@code stat} chaqiruvi, mikrosoniya.
 * S3 tekshiruvi esa tarmoq murojaati, o'nlab millisoniya. Eski fayllar
 * bir marta ko'chirilgach lokal tekshiruv har doim «yo'q» deb tez
 * javob beradi va ustama sezilmaydi.
 *
 * <h2>Bu vaqtinchalik emas</h2>
 * Migratsiya bo'lsa ham, buyurtmachi eski fayllarni ko'chirmaslikni
 * tanlashi mumkin (roadmap §6, 4-savol). Yo'naltiruvchi ikkala holatda
 * ham to'g'ri ishlaydi.
 */
/*
 * ⚠️ `@ConditionalOnBean` ISHLATILMAYDI.
 *
 * U faqat avtokonfiguratsiya klasslari uchun ishonchli: oddiy
 * `@Service` da shart komponent SKANERLASH TARTIBIDA baholanadi va
 * kerakli bin hali ro'yxatga olinmagan bo'lishi mumkin. Aynan shu
 * sodir bo'ldi — bin yaratilib, bog'liqligi topilmadi va butun
 * kontekst ko'tarilmadi (213 test yiqildi).
 *
 * Xususiyat sharti esa tartibga bog'liq emas.
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class RoutingStorageService implements StorageService {

    private final S3StorageService s3;
    private final LocalStorageService local;

    public RoutingStorageService(S3StorageService s3, LocalStorageService local) {
        this.s3 = s3;
        this.local = local;
        log.info("Saqlash: yangi fayllar S3 ga, eski fayllar lokal diskdan o'qiladi");
    }

    // --------------------------------------------------------- yozish → S3

    @Override
    public String store(MultipartFile file, String folder) {
        return s3.store(file, folder);
    }

    @Override
    public String store(java.io.InputStream in, String originalFilename, String folder) {
        return s3.store(in, originalFilename, folder);
    }

    /** Kengaytma qoidasi ikkala omborda bir xil — {@link StorageKeys}. */
    @Override
    public boolean accepts(String originalFilename) {
        return s3.accepts(originalFilename);
    }

    // ------------------------------------------------- o'qish → qayerda bo'lsa

    @Override
    public Resource load(String storageKey) {
        return local.exists(storageKey) ? local.load(storageKey) : s3.load(storageKey);
    }

    @Override
    public boolean exists(String storageKey) {
        return local.exists(storageKey) || s3.exists(storageKey);
    }

    /**
     * ⚠️ IKKALA omborda ham o'chiriladi.
     *
     * Faqat bittasida o'chirish egasiz fayl qoldirardi: baza yozuvi yo'q,
     * fayl esa diskda yoki bucketda joy egallab turadi va uni hech kim
     * topa olmaydi.
     */
    @Override
    public void delete(String storageKey) {
        if (local.exists(storageKey)) {
            local.delete(storageKey);
        }
        s3.delete(storageKey);
    }
}
