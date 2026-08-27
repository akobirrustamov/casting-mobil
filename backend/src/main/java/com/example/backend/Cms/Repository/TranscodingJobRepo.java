package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.TranscodingJob;
import com.example.backend.Cms.Enums.VideoProcessingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
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
     * <h2>⚠️ Nega {@code SKIP LOCKED}</h2>
     * Ikki instans (yoki bitta instansning ikki oqimi) bir vaqtda
     * navbatga qarasa, ikkalasi ham AYNI ishni olardi va bitta video
     * ikki marta transcoding qilinardi — protsessor ikki barobar
     * band, natija esa bir-birining ustiga yozilardi.
     *
     * {@code SKIP LOCKED} qulflangan qatorlarni o'tkazib yuboradi:
     * ikkinchi so'rovchi keyingi ishni oladi, kutib turmaydi.
     *
     * ⚠️ H2 (test profili) buni PostgreSQL rejimida qo'llab-quvvatlaydi.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select j from TranscodingJob j where j.status = :status order by j.createdAt asc")
    List<TranscodingJob> lockNextPending(@Param("status") VideoProcessingStatus status,
                                         Pageable pageable);

    /** Panel uchun: nechta ish navbatda yoki bajarilmoqda. */
    long countByStatusIn(List<VideoProcessingStatus> statuses);
}
