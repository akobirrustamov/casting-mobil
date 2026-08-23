package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.EpisodeVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Video fayl qaysi qismga tegishli ekanini bilish uchun.
 *
 * Qismlar odatda Episode orqali cascade bilan boshqariladi, lekin bu yerda
 * teskari savol kerak: "shu media faylni kim so'rayapti va u haqli mi?"
 * ({@link com.example.backend.Cms.Service.AccessService#canReadMedia}).
 */
public interface EpisodeVideoRepo extends JpaRepository<EpisodeVideo, Long> {

    /**
     * Shu media faylga bog'langan birinchi yozuv.
     *
     * Bitta fayl nazariy jihatdan bir nechta qismda ishlatilishi mumkin.
     * Ruxsat uchun bittasi yetarli: fayl mazmuni bir xil, va agar foydalanuvchi
     * hech bo'lmasa bitta qismni ko'ra olsa — faylni olishga haqli.
     */
    Optional<EpisodeVideo> findFirstByMediaId(Long mediaId);
}
