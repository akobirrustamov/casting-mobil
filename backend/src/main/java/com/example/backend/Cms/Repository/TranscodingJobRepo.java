package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.TranscodingJob;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TranscodingJobRepo extends JpaRepository<TranscodingJob, Long> {

    /** Har media uchun bitta ish — {@code unique(media_id)}. */
    Optional<TranscodingJob> findByMediaId(Long mediaId);

    /**
     * Sahifadagi barcha medialar uchun ishlar — BIR so'rovda.
     *
     * ⚠️ Har media uchun alohida so'rov N+1 bo'lardi: kutubxona 40 ta
     * element ko'rsatadi, ya'ni 40 ta ortiqcha murojaat.
     */
    List<TranscodingJob> findAllByMediaIdIn(List<Long> mediaIds);

    /**
     * Navbatdagi keyingi ishlar.
     *
     * <h2>⚠️ Nega qulf kerak</h2>
     * Ikki instans (yoki bitta instansning ikki oqimi) bir vaqtda
     * navbatga qarasa, ikkalasi ham AYNI ishni olardi va bitta video
     * ikki marta transcoding qilinardi — protsessor ikki barobar
     * band, natija esa bir-birining ustiga yozilardi.
     *
     * <h2>⚠️ Nega {@code SKIP LOCKED} EMAS</h2>
     * Dastlab {@code SKIP LOCKED} ishlatilgan edi. U H2 tomonidan
     * QO'LLAB-QUVVATLANMAYDI — hatto PostgreSQL rejimida ham — va
     * butun test to'plami sintaksis xatosi bilan yiqilardi.
     *
     * Nosozlik uzoq vaqt YASHIRIN qoldi: sozlamadagi buzuq
     * {@code ${...}} havolasi tufayli {@code PostgreSQLDialect}
     * qo'llanmayotgan edi va Hibernate H2 dialektiga tushib, bu
     * bayroqni umuman chiqarmasdi. Sozlama tuzatilgach xato darhol
     * ko'rindi.
     *
     * <h2>Oddiy {@code FOR UPDATE} yetarli</h2>
     * {@code READ COMMITTED} da PostgreSQL qulfni olgandan KEYIN
     * {@code where} shartini qayta tekshiradi. Ya'ni ikkinchi
     * so'rovchi kutadi, keyin qatorni {@code QUEUED} emas deb ko'radi
     * va uni olmaydi — natija {@code SKIP LOCKED} bilan bir xil.
     *
     * Farqi faqat qisqa kutishda, va {@code max-concurrent-jobs}
     * odatda 1–3 bo'lgani uchun u sezilmaydi.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from TranscodingJob j where j.status = :status order by j.createdAt asc")
    List<TranscodingJob> lockNextPending(@Param("status") VideoProcessingStatus status,
                                         Pageable pageable);

    /** Panel uchun: nechta ish navbatda yoki bajarilmoqda. */
    long countByStatusIn(List<VideoProcessingStatus> statuses);
}
