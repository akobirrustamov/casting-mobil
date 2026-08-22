package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentListDto;
import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.Translations;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §60 — bir vaqtda tahrirlash.
 *
 * <h2>Nega {@code @Version} o'zi yetarli emas</h2>
 * {@code @Version} bitta tranzaksiya ichidagi to'qnashuvni ushlaydi.
 * Panel tahriri esa boshqacha ketma-ketlik:
 *
 * <pre>
 *   09:55  admin A formani ochadi        (versiya 3)
 *   09:57  admin B formani ochadi        (versiya 3)
 *   10:00  B saqlaydi                    (versiya 3 → 4)
 *   10:01  A saqlaydi                    ← bu yerda to'xtatilishi kerak
 * </pre>
 *
 * A ning so'rovi qatorni bazadan YANGI holida o'qiydi (versiya 4),
 * ustiga eskirgan forma ma'lumotini yozadi va muvaffaqiyatli saqlaydi.
 * Hibernate uchun to'qnashuv yo'q — B ning ishi shunchaki yo'qoladi.
 * Shuning uchun klient formani ochgandagi versiyani qaytarib yuborishi
 * shart.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConcurrentEditTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private ContentRepo contentRepo;
    @Autowired private EntityManager em;

    // -------------------------------------------------------------- kontent

    @Nested
    @DisplayName("Kontent")
    class ContentEdits {

        @Test
        @DisplayName("Eskirgan versiya bilan saqlash to'xtatiladi")
        void staleVersionIsRejected() {
            Content content = content();
            Long openedAt = content.getVersion();   // ikkala admin ham shuni ko'rdi

            // Admin B saqladi
            contentService.update(null, content.getId(),
                    request("B yozgan sarlavha", openedAt));
            flush();

            // Admin A eskirgan versiya bilan saqlamoqchi
            assertThatThrownBy(() -> contentService.update(null, content.getId(),
                    request("A yozgan sarlavha", openedAt)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT)
                    .hasMessageContaining("boshqa foydalanuvchi");
        }

        @Test
        @DisplayName("To'xtatilgach B ning ishi joyida qoladi")
        void firstWriterSurvives() {
            Content content = content();
            Long openedAt = content.getVersion();

            contentService.update(null, content.getId(), request("B yozgan sarlavha", openedAt));
            flush();

            try {
                contentService.update(null, content.getId(), request("A yozgan sarlavha", openedAt));
            } catch (BusinessException expected) {
                // kutilgan
            }
            flush();

            Content after = contentRepo.findById(content.getId()).orElseThrow();
            assertThat(title(after))
                    .as("indamay bosib ketilmasligi kerak")
                    .isEqualTo("B yozgan sarlavha");
        }

        @Test
        @DisplayName("To'g'ri versiya bilan saqlanadi")
        void currentVersionIsAccepted() {
            Content content = content();

            Content saved = contentService.update(null, content.getId(),
                    request("Yangi sarlavha", content.getVersion()));
            flush();

            assertThat(title(saved)).isEqualTo("Yangi sarlavha");
        }

        @Test
        @DisplayName("Versiyasiz saqlash rad etiladi")
        void missingVersionIsRejected() {
            Content content = content();

            // Tekshiruv «versiya kelgan bo'lsa» shartiga bog'langanida
            // himoya ixtiyoriy bo'lardi: versiyani yubormagan klient
            // hech qanday ogohlantirish olmay bosib ketaverardi.
            assertThatThrownBy(() -> contentService.update(null, content.getId(),
                    request("Versiyasiz", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Test
        @DisplayName("Har saqlashda versiya oshadi")
        void versionIncrements() {
            Content content = content();
            Long first = content.getVersion();

            contentService.update(null, content.getId(), request("Ikkinchi", first));
            flush();

            Long second = contentRepo.findById(content.getId()).orElseThrow().getVersion();
            assertThat(second)
                    .as("versiya oshmasa eskirgan forma hech qachon aniqlanmasdi")
                    .isGreaterThan(first);
        }

        @Test
        @DisplayName("Faqat tarjima o'zgarganda ham versiya oshadi")
        void translationOnlyChangeBumpsVersion() {
            // ⚠️ Tarjimalar — bola jadval. Agar egasining versiyasi
            // oshmasa, ikki admin ikki xil tilni tahrirlab, bir-birining
            // matnini indamay bosib ketardi: ikkalasining ham versiyasi
            // «to'g'ri» bo'lib qolaverardi.
            Content content = content();

            // Avval bir marta saqlaymiz — shundan keyin bazadagi holat
            // so'rov bilan AYNAN bir xil bo'ladi. Aks holda ikkinchi
            // saqlashda boshqa maydonlar ham o'zgarib, versiya ular
            // tufayli oshardi va test hech narsani isbotlamasdi.
            ContentSaveRequest baseline = request("Bir xil sarlavha", content.getVersion());
            contentService.update(null, content.getId(), baseline);
            flush();
            Long before = contentRepo.findById(content.getId()).orElseThrow().getVersion();

            ContentSaveRequest onlyText = request("Bir xil sarlavha", before);
            onlyText.getTranslations().get(Locale.RU).setDescription("Faqat ruscha tavsif o'zgardi");
            contentService.update(null, content.getId(), onlyText);
            flush();

            assertThat(contentRepo.findById(content.getId()).orElseThrow().getVersion())
                    .isGreaterThan(before);
        }

        @Test
        @DisplayName("Faqat tarjima o'zgarganda ham `updatedAt` yangilanadi")
        void translationOnlyChangeUpdatesTimestamp() {
            // Bu test XUSUSIYATNI qo'riqlaydi, uni ta'minlayotgan aniq
            // qatorni emas. Bugun `updatedAt` ikki sababdan yangilanadi:
            // `touch()` va `apply()` dagi `media.clear()` (kolleksiya
            // o'zgarishi egasining qatorini yangilashga majbur qiladi).
            // Mutatsiya buni ko'rsatdi: `touch()` ni olib tashlasam ham
            // test o'tib ketdi.
            //
            // Test baribir kerak: ikkala mexanizm ham yo'qolsa —
            // masalan `apply()` optimallashtirilsa — u yiqiladi va ikki
            // admin bir-birini indamay bosib ketishidan xabar beradi.
            Content content = content();
            ContentSaveRequest baseline = request("Bir xil sarlavha", content.getVersion());
            contentService.update(null, content.getId(), baseline);
            flush();

            Content afterFirst = contentRepo.findById(content.getId()).orElseThrow();
            java.time.LocalDateTime before = afterFirst.getUpdatedAt();

            ContentSaveRequest onlyText = request("Bir xil sarlavha", afterFirst.getVersion());
            onlyText.getTranslations().get(Locale.RU).setDescription("Boshqa tavsif");
            contentService.update(null, content.getId(), onlyText);
            flush();

            assertThat(contentRepo.findById(content.getId()).orElseThrow().getUpdatedAt())
                    .as("tahrir vaqti yangilanmasa, qator toza qolgan degani")
                    .isAfter(before);
        }

        @Test
        @DisplayName("API versiyani qaytaradi")
        void apiExposesVersion() {
            Content content = content();

            // Bu maydonsiz butun himoya o'lik edi: panel formasi doim
            // `null` yuborar va tekshiruv har safar o'tkazib yuborilardi.
            assertThat(ContentListDto.from(content).getVersion())
                    .as("panel versiyani API'dan oladi")
                    .isEqualTo(content.getVersion());
        }
    }

    // ----------------------------------------------------------------- qism

    @Nested
    @DisplayName("Qism")
    class EpisodeEdits {

        @Test
        @DisplayName("Eskirgan versiya bilan saqlash to'xtatiladi")
        void staleVersionIsRejected() {
            Content series = series();
            Episode ep = episode(series, null, "Birinchi", 1);
            Long openedAt = ep.getVersion();

            episodeService.saveEpisode(null, series.getId(), ep.getId(),
                    episodeRequest("B yozgan", 1, openedAt));
            flush();

            assertThatThrownBy(() -> episodeService.saveEpisode(null, series.getId(), ep.getId(),
                    episodeRequest("A yozgan", 1, openedAt)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Versiyasiz saqlash rad etiladi")
        void missingVersionIsRejected() {
            Content series = series();
            Episode ep = episode(series, null, "Birinchi", 1);

            assertThatThrownBy(() -> episodeService.saveEpisode(null, series.getId(), ep.getId(),
                    episodeRequest("Versiyasiz", 1, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        @Test
        @DisplayName("Yangi qism yaratishda versiya so'ralmaydi")
        void createNeedsNoVersion() {
            Content series = series();

            // Yaratishda to'qnashadigan «oldingi holat» yo'q — versiya
            // talab qilish shunchaki yangi qism qo'shishni buzardi.
            Episode created = episode(series, null, "Yangi", 7);

            assertThat(created.getId()).isNotNull();
        }
    }

    // ------------------------------------------------------------ yordamchi

    /**
     * Testda entity'lar bitta tranzaksiyada yaratiladi va persistence
     * context'da qoladi. Versiya esa {@code flush} paytida oshadi —
     * shusiz test o'zgarishni umuman ko'rmasdi.
     */
    private void flush() {
        em.flush();
        em.clear();
    }

    private String title(Content c) {
        return contentRepo.findById(c.getId()).orElseThrow().getTranslations().stream()
                .filter(t -> t.getLocale() == Locale.UZ)
                .map(t -> t.getTitle())
                .findFirst().orElse(null);
    }

    private ContentSaveRequest request(String title, Long version) {
        ContentSaveRequest r = new ContentSaveRequest();
        r.setContentType(ContentType.MOVIE);
        r.setStructureType(StructureType.SINGLE);
        r.setAccessPolicy(AccessPolicy.FREE);
        r.setStatus(PublicationStatus.DRAFT);
        r.setTranslations(Translations.all(title));
        r.setVersion(version);
        return r;
    }

    private Content content() {
        Content c = contentService.create(null, request("Boshlang'ich " + SEQ.incrementAndGet(), null));
        flush();
        return contentRepo.findById(c.getId()).orElseThrow();
    }

    private Content series() {
        ContentSaveRequest r = request("Serial " + SEQ.incrementAndGet(), null);
        r.setContentType(ContentType.SERIES);
        r.setStructureType(StructureType.EPISODIC);
        Content c = contentService.create(null, r);
        flush();
        return contentRepo.findById(c.getId()).orElseThrow();
    }

    private EpisodeSaveRequest episodeRequest(String title, int number, Long version) {
        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setEpisodeNumber(number);
        e.setStatus(PublicationStatus.DRAFT);
        e.setTranslations(Translations.all(title));
        e.setVersion(version);
        return e;
    }

    private Episode episode(Content series, Long id, String title, int number) {
        Episode e = episodeService.saveEpisode(null, series.getId(), id,
                episodeRequest(title, number, null));
        flush();
        return e;
    }
}
