package com.example.backend.Cms;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.Storage.StorageStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Yetim faylni o'chirish — XAVFSIZLIK CHEGARASI.
 *
 * <h2>⚠️ Nima uchun bu eng muhim test</h2>
 * Ombordan o'chirilgan fayl QAYTARIB BO'LMAYDI. Panel esa
 * hisobotdagi ro'yxatga qarab o'chirish tugmasini ko'rsatadi — va
 * o'sha hisobot bir necha soat oldin olingan bo'lishi mumkin.
 *
 * Shu vaqt ichida boshqa admin faylni kutubxonadan tanlab, kontentga
 * biriktirgan bo'lishi mumkin. Server keshdagi ro'yxatga ishonsa,
 * ISHLAB TURGAN videoni o'chirardi va buni hech kim sezmasdi —
 * nosozlik tomoshabin qora ekran ko'rganda, ancha keyin chiqardi.
 *
 * Shuning uchun server har o'chirishdan oldin bazadan QAYTA
 * hisoblaydi. Quyidagi testlar aynan shuni qo'riqlaydi.
 *
 * ⚠️ S3 rejimi majburiy: `StorageStatsService` faqat o'shanda bin
 * bo'ladi. Haqiqiy S3 chaqiruvi bo'lmaydi — tekshiruv baza
 * darajasida, o'chirishgacha yiqiladi.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.storage.provider=s3",
        "app.storage.s3.bucket=sinov",
        "app.storage.s3.access-key=sinov",
        "app.storage.s3.secret-key=sinov",
})
class OrphanDeleteSafetyTest {

    @Autowired private StorageStatsService statsService;
    @Autowired private MediaAssetRepo mediaAssetRepo;

    /**
     * ⚠️ ENG MUHIM TEKSHIRUV.
     *
     * Kalit bazada BOR — demak fayl yetim EMAS va o'chirilmasligi
     * kerak, hisobot nima deyishidan qat'i nazar.
     */
    @Test
    @DisplayName("Bazada yozuvi BOR fayl o'chirilmaydi")
    void referencedKeyIsRefused() {
        MediaAsset asset = mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/content/hali-ishlatilmoqda.mp4")
                .originalFilename("hali-ishlatilmoqda.mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(100L)
                .status(MediaStatus.READY)
                .build());

        assertThatThrownBy(() ->
                statsService.deleteOrphan("content/hali-ishlatilmoqda.mp4"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ishlatilmoqda");

        // Yozuv ham tegilmagan bo'lishi kerak.
        assertThat(mediaAssetRepo.findById(asset.getId())).isPresent();
    }

    /**
     * ⚠️ Boshidagi qiyshiq chiziq FARQ QILMAYDI.
     *
     * Baza `/content/x.mp4`, S3 esa `content/x.mp4` deb yozadi.
     * Tenglashtirilmasa tekshiruv «mos kelmadi» deb, ishlab turgan
     * faylni o'chirishga ruxsat berardi — ya'ni himoya jimgina
     * ochilardi.
     */
    @Test
    @DisplayName("Qiyshiq chiziqli shakl ham HIMOYALANADI")
    void slashVariantAlsoRefused() {
        mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/content/qiyshiq.mp4")
                .originalFilename("qiyshiq.mp4")
                .type(MediaType.IMAGE)
                .mimeType("image/png")
                .sizeBytes(10L)
                .status(MediaStatus.READY)
                .build());

        assertThatThrownBy(() -> statsService.deleteOrphan("/content/qiyshiq.mp4"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> statsService.deleteOrphan("content/qiyshiq.mp4"))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * ⚠️ HLS segmenti — media'ning bir qismi, mustaqil fayl EMAS.
     *
     * `videos/{id}/...` shaklidagi kalit o'sha media'ga tegishli.
     * Tekshirilmasa, transkodlangan videoning yuzlab segmenti
     * «yetim» deb o'chirilardi va video buzilardi.
     */
    @Test
    @DisplayName("HLS segmenti o'chirilmaydi — media mavjud")
    void hlsSegmentOfExistingMediaIsRefused() {
        MediaAsset asset = mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/content/hls-egasi.mp4")
                .originalFilename("hls-egasi.mp4")
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(100L)
                .status(MediaStatus.READY)
                .build());

        assertThatThrownBy(() -> statsService.deleteOrphan(
                "videos/" + asset.getId() + "/hls/480p/segment_00001.m4s"))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Haqiqatan yetim fayl tekshiruvdan O'TADI.
     *
     * ⚠️ Bu ham kerak: himoya hamma narsani rad etsa, xususiyat
     * umuman ishlamasdi va buni sezmay qolish oson edi.
     *
     * O'chirishning o'zi bu yerda S3 ga boradi va yiqiladi — bizni
     * faqat TEKSHIRUV qiziqtiradi, ya'ni `IllegalStateException`
     * chiqmasligi.
     */
    @Test
    @DisplayName("Haqiqiy yetim fayl tekshiruvdan o'tadi")
    void genuineOrphanPassesTheCheck() {
        assertThatCode(() -> {
            try {
                statsService.deleteOrphan("content/hech-kimniki-emas.png");
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception ignored) {
                // S3 ga chiqib yiqildi — tekshiruvdan O'TGAN degani.
            }
        }).doesNotThrowAnyException();
    }
}
