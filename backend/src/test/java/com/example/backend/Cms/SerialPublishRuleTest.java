package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.ContentFixtures;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Serial ko'rsa bo'ladigan qismsiz nashr qilinmaydi (10.09.2026).
 *
 * <h2>Qanday xato edi</h2>
 * Serialda video kontentning o'ziga emas, QISMGA biriktiriladi. Qismsiz
 * serial nashr qilinardi, ilovada ochilardi, «Tomosha qilish» ham bor edi —
 * lekin u bo'sh ro'yxatga olib borardi. Tomoshabin uchun bu «video
 * yuklangan, ammo ochilmayapti» edi, admin esa hech qanday xato ko'rmasdi.
 *
 * <h2>⚠️ Bu yerda {@link ContentFixtures} ISHLATILMAYDI</h2>
 * U aynan shu qoidani chetlab o'tadi — boshqa testlar uchun. Bu yerda
 * esa har bir so'rov {@link ContentService} ga to'g'ridan-to'g'ri boradi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SerialPublishRuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private ContentRepo contentRepo;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private ContentFixtures contentFixtures;

    // ------------------------------------------------------------- yordamchi

    private ContentSaveRequest request(StructureType structure, PublicationStatus status) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(structure == StructureType.SINGLE ? ContentType.MOVIE : ContentType.SERIES);
        c.setStructureType(structure);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(status);
        c.setTranslations(Translations.all("Nashr qoidasi " + SEQ.incrementAndGet()));
        return c;
    }

    private Content draftSerial() {
        return contentService.create(null, request(StructureType.EPISODIC, PublicationStatus.DRAFT));
    }

    private MediaAsset video() {
        MediaAsset a = new MediaAsset();
        a.setStorageKey("test/serial-" + SEQ.incrementAndGet() + ".mp4");
        a.setOriginalFilename("qism.mp4");
        a.setType(MediaType.VIDEO);
        a.setMimeType("video/mp4");
        a.setSizeBytes(1024L);
        a.setStatus(MediaStatus.READY);
        a.setCreatedAt(LocalDateTime.now());
        return mediaAssetRepo.save(a);
    }

    private void episode(Content content, PublicationStatus status, boolean withVideo) {
        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setEpisodeNumber(1);
        e.setStatus(status);
        e.setTranslations(Translations.all("1-qism"));
        if (withVideo) {
            EpisodeSaveRequest.VideoLink link = new EpisodeSaveRequest.VideoLink();
            link.setMediaId(video().getId());
            link.setPartNumber(1);
            e.setVideos(List.of(link));
        }
        episodeService.saveEpisode(null, content.getId(), null, e);
    }

    /** Muharrirdagi «Saqlash» — joriy versiya bilan, admin qilganidek. */
    private Content save(Content content, StructureType structure, PublicationStatus status) {
        contentRepo.flush();
        ContentSaveRequest r = request(structure, status);
        r.setVersion(contentRepo.findById(content.getId()).orElseThrow().getVersion());
        return contentService.update(null, content.getId(), r);
    }

    // -------------------------------------------------------------- rad etish

    @Nested
    @DisplayName("Rad etiladi")
    class Rejected {

        @Test
        @DisplayName("Yaratilishdayoq nashr — yangi serialda qism bo'lishi mumkin emas")
        void cannotCreateAsPublished() {
            assertThatThrownBy(() -> contentService.create(null,
                    request(StructureType.EPISODIC, PublicationStatus.PUBLISHED)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("qism");
        }

        @Test
        @DisplayName("Qismsiz qoralamani nashr qilish")
        void noEpisodes() {
            Content serial = draftSerial();

            assertThatThrownBy(() -> save(serial, StructureType.EPISODIC, PublicationStatus.PUBLISHED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("qism");
        }

        @Test
        @DisplayName("Qism bor, lekin videosiz — tomoshabin uchun u yo'q bilan barobar")
        void episodeWithoutVideo() {
            Content serial = draftSerial();
            episode(serial, PublicationStatus.PUBLISHED, false);

            assertThatThrownBy(() -> save(serial, StructureType.EPISODIC, PublicationStatus.PUBLISHED))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Videoli qism hali qoralama — ilovada ko'rinmaydi")
        void onlyDraftEpisode() {
            Content serial = draftSerial();
            episode(serial, PublicationStatus.DRAFT, true);

            assertThatThrownBy(() -> save(serial, StructureType.EPISODIC, PublicationStatus.PUBLISHED))
                    .isInstanceOf(BusinessException.class);
        }

        /**
         * Aynan shu holatda film videosi yashirin qolib ketardi: panel
         * serialda asosiy video maydonini ko'rsatmaydi, lekin bog'lanish
         * saqlanaverardi — ilova esa uni hech qachon ochmasdi.
         */
        @Test
        @DisplayName("Nashr qilingan film qismsiz serialga aylantirilsa")
        void publishedFilmTurnedIntoSerial() {
            Content film = contentService.create(null,
                    request(StructureType.SINGLE, PublicationStatus.PUBLISHED));

            assertThatThrownBy(() -> save(film, StructureType.EPISODIC, PublicationStatus.PUBLISHED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("qism");
        }
    }

    // --------------------------------------------------------------- ruxsat

    @Nested
    @DisplayName("Ruxsat etiladi")
    class Allowed {

        @Test
        @DisplayName("Videoli nashr qilingan qism bo'lsa — nashr o'tadi")
        void publishedEpisodeWithVideo() {
            Content serial = draftSerial();
            episode(serial, PublicationStatus.PUBLISHED, true);

            Content saved = save(serial, StructureType.EPISODIC, PublicationStatus.PUBLISHED);

            assertThat(saved.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        }

        @Test
        @DisplayName("Rejalashtirilgan serial rejalashtirilgan qism bilan — birga chiqadi")
        void scheduledTogether() {
            Content serial = draftSerial();
            episode(serial, PublicationStatus.SCHEDULED, true);

            assertThatCode(() -> save(serial, StructureType.EPISODIC, PublicationStatus.SCHEDULED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Qoralama — tekshirilmaydi: qism keyin qo'shiladi")
        void draftIsNotChecked() {
            Content serial = draftSerial();

            assertThatCode(() -> save(serial, StructureType.EPISODIC, PublicationStatus.DRAFT))
                    .doesNotThrowAnyException();
        }

        /**
         * Qoida faqat kontent tomoshabinga KO'RINA BOSHLAGANDA ishlaydi.
         * Aks holda 10.09.2026 gacha qismsiz nashr qilingan serialning
         * sarlavhasidagi bitta harfni ham tuzatib bo'lmay qolardi.
         */
        @Test
        @DisplayName("Ilgari qismsiz nashr qilingan serialni tahrirlash to'xtamaydi")
        void legacyPublishedSerialStaysEditable() {
            Content legacy = contentFixtures.create(
                    request(StructureType.EPISODIC, PublicationStatus.PUBLISHED));

            assertThatCode(() -> save(legacy, StructureType.EPISODIC, PublicationStatus.PUBLISHED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Film qoidaga tushmaydi")
        void filmsAreUnaffected() {
            assertThatCode(() -> contentService.create(null,
                    request(StructureType.SINGLE, PublicationStatus.PUBLISHED)))
                    .doesNotThrowAnyException();
        }
    }
}
