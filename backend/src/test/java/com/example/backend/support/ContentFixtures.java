package com.example.backend.support;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Testlar uchun kontent — «tayyor holatda».
 *
 * <h2>Nima uchun kerak (10.09.2026)</h2>
 * {@link ContentService} endi ko'p qismli kontentni ko'rsa bo'ladigan
 * qismsiz NASHR QILMAYDI ({@code requirePlayableEpisode}). Kirish, narx,
 * lenta va like testlari esa aynan «nashr qilingan serial» dan boshlanadi
 * va qismni KEYIN qo'shadi — ular nashr qoidasini emas, boshqa narsani
 * sinaydi.
 *
 * Ular uchun serial avval QORALAMA sifatida servis orqali yaratiladi —
 * tarjima, narx, media qoidalari odatdagidek ishlaydi — va keyin holati
 * to'g'ridan-to'g'ri qo'yiladi. {@code DevDataSeeder} ham serialni xuddi
 * shunday, servisni chetlab yozadi.
 *
 * <h2>⚠️ Nashr qoidasining O'ZI bu yerda sinalmaydi</h2>
 * Uni {@code SerialPublishRuleTest} servisga to'g'ridan-to'g'ri murojaat
 * qilib tekshiradi. Bu yordamchini u yerda ishlatish qoidani chetlab o'tib,
 * testni ma'nosiz qilardi.
 *
 * Film va qoralama so'rovlari servisga O'ZGARISHSIZ o'tadi.
 */
@Component
@RequiredArgsConstructor
public class ContentFixtures {

    private final ContentService contentService;
    private final ContentRepo contentRepo;

    public Content create(ContentSaveRequest request) {
        PublicationStatus wanted = request.getStatus();

        boolean visible = wanted == PublicationStatus.PUBLISHED
                || wanted == PublicationStatus.SCHEDULED;
        boolean multiPart = request.getStructureType() != null
                && request.getStructureType() != StructureType.SINGLE;

        if (!visible || !multiPart) {
            return contentService.create(null, request);
        }

        request.setStatus(PublicationStatus.DRAFT);
        try {
            Content content = contentService.create(null, request);

            content.setStatus(wanted);
            // Servis PUBLISHED da sanani o'zi qo'yardi — lenta va kirish
            // qoidalari unga tayanadi, shuning uchun bu yerda ham qo'yiladi.
            if (wanted == PublicationStatus.PUBLISHED && content.getPublicationDate() == null) {
                content.setPublicationDate(LocalDateTime.now());
            }
            return contentRepo.saveAndFlush(content);
        } finally {
            // So'rov obyekti chaqiruvchiga qaytadi — ba'zi testlar uni
            // keyin qayta ishlatadi.
            request.setStatus(wanted);
        }
    }
}
