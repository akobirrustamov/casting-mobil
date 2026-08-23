package com.example.backend.Cms;

import com.example.backend.Admin.Dto.AdvertisementDto;
import com.example.backend.Admin.Dto.AdvertisementSaveRequest;
import com.example.backend.Admin.Dto.InternalLinkDto;
import com.example.backend.Cms.Entity.Advertisement;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.HomepageService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §27 — Reklama moduli.
 *
 * <h2>Asosiy talab</h2>
 * «Link optional. Button ham optional. {@code buttonEnabled = false}
 * bo'lsa tugma umuman chiqmaydi.»
 *
 * Ya'ni banner uchta shaklda bo'lishi mumkin: faqat rasm; rasm + tugma
 * tashqi havola bilan; rasm + tugma ilova ichidagi ekranga. Model
 * uchalasini ham qo'llab-quvvatlashi kerak va hech biri majburiy emas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdvertisementModuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private HomepageService homepageService;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private com.example.backend.Cms.Service.ContentService contentService;

    private MediaAsset image(String name) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/" + name + "-" + SEQ.incrementAndGet() + ".jpg")
                .originalFilename(name + ".jpg")
                .type(MediaType.IMAGE)
                .mimeType("image/jpeg")
                .sizeBytes(100L)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private AdvertisementDto.AdTextDto text(String title, String description) {
        return AdvertisementDto.AdTextDto.builder()
                .title(title).description(description).build();
    }

    private Map<Locale, AdvertisementDto.AdTextDto> titles(String base) {
        return Map.of(
                Locale.UZ, text(base + SEQ.incrementAndGet(), null),
                Locale.RU, text(base + " RU", null),
                Locale.EN, text(base + " EN", null));
    }

    /** Havola {@code InternalLinkDto} ichida — banner va premyera uchun umumiy. */
    private InternalLinkDto link(LinkType type, String url,
                                 InternalTargetType targetType, Long targetId) {
        InternalLinkDto l = new InternalLinkDto();
        l.setLinkType(type);
        l.setLinkUrl(url);
        l.setInternalTargetType(targetType);
        l.setInternalTargetId(targetId);
        return l;
    }

    private AdvertisementSaveRequest request(String title, MediaAsset img) {
        AdvertisementSaveRequest r = new AdvertisementSaveRequest();
        // Ichki nom - admin ro'yxatida ko'rinadi, tarjima qilinmaydi.
        r.setName(title + " " + SEQ.incrementAndGet());
        r.setImageMediaId(img.getId());
        r.setAudience(AdAudience.ADVERTISEMENT);
        r.setStatus(PublicationStatus.PUBLISHED);
        r.setTranslations(titles(title));
        return r;
    }

    // -------------------------------------------------------- ixtiyoriylik

    @Nested
    @DisplayName("Tugma va havola ixtiyoriy")
    class OptionalParts {

        @Test
        @DisplayName("Faqat rasm — tugma ham, havola ham yo'q")
        void imageOnlyBanner() {
            AdvertisementSaveRequest r = request("Faqat rasm", image("banner"));
            r.setButtonEnabled(false);

            Advertisement ad = homepageService.saveAdvertisement(null, null, r);

            assertThat(ad.getButtonEnabled()).isFalse();
            // buttonEnabled = false bo'lsa tugma umuman chiqmaydi (ТЗ §27),
            // shuning uchun havolaning bor-yo'qligi ahamiyatsiz.
            assertThat(ad.getLink() == null || !ad.getLink().isActionable()).isTrue();
        }

        @Test
        @DisplayName("Tugma yoqilgan, tashqi havola bilan")
        void buttonWithExternalLink() {
            AdvertisementSaveRequest r = request("Tashqi havola", image("banner"));
            r.setButtonEnabled(true);
            r.setLink(link(LinkType.EXTERNAL, "https://uzcasting.uz/aksiya", null, null));

            Advertisement ad = homepageService.saveAdvertisement(null, null, r);

            assertThat(ad.getButtonEnabled()).isTrue();
            assertThat(ad.getLink().getLinkType()).isEqualTo(LinkType.EXTERNAL);
            assertThat(ad.getLink().isActionable()).isTrue();
        }

        @Test
        @DisplayName("Tugma ilova ichidagi ekranga olib boradi")
        void buttonWithInternalTarget() {
            AdvertisementSaveRequest r = request("Ichki havola", image("banner"));
            r.setButtonEnabled(true);
            // Nishon HAQIQATDAN mavjud bo'lishi kerak — mavjud bo'lmagan ID
            // bilan banner saqlansa, mobil ilovada hech qayerga olib
            // bormaydigan tugma paydo bo'lardi.
            Long contentId = someContent().getId();
            r.setLink(link(LinkType.INTERNAL, null, InternalTargetType.CONTENT, contentId));

            Advertisement ad = homepageService.saveAdvertisement(null, null, r);

            assertThat(ad.getLink().getInternalTargetType())
                    .isEqualTo(InternalTargetType.CONTENT);
            assertThat(ad.getLink().getInternalTargetId()).isEqualTo(contentId);
        }

        @Test
        @DisplayName("Mavjud bo'lmagan kontentga havola rad etiladi")
        void deadInternalLinkIsRejected() {
            AdvertisementSaveRequest r = request("O'lik havola", image("banner"));
            r.setButtonEnabled(true);
            r.setLink(link(LinkType.INTERNAL, null, InternalTargetType.CONTENT, 999_999L));

            assertThatThrownBy(() -> homepageService.saveAdvertisement(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("topilmadi");
        }

        @Test
        @DisplayName("http/https bo'lmagan tashqi havola rad etiladi")
        void nonHttpExternalLinkIsRejected() {
            AdvertisementSaveRequest r = request("Xavfli sxema", image("banner"));
            r.setButtonEnabled(true);
            // Admin panelidagi matn maydoni orqali klientga `javascript:`
            // uzatilmasin.
            r.setLink(link(LinkType.EXTERNAL, "javascript:alert(1)", null, null));

            assertThatThrownBy(() -> homepageService.saveAdvertisement(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("http");
        }

        /** Havola nishoni sifatida ishlatish uchun haqiqiy kontent. */
        private com.example.backend.Cms.Entity.Content someContent() {
            com.example.backend.Admin.Dto.ContentSaveRequest req =
                    new com.example.backend.Admin.Dto.ContentSaveRequest();
            req.setContentType(com.example.backend.Cms.Enums.ContentType.MOVIE);
            req.setStructureType(com.example.backend.Cms.Enums.StructureType.SINGLE);
            req.setAccessPolicy(com.example.backend.Cms.Enums.AccessPolicy.FREE);
            req.setStatus(com.example.backend.Cms.Enums.PublicationStatus.DRAFT);
            req.setTranslations(com.example.backend.support.Translations.all("Havola nishoni"));
            return contentService.create(null, req);
        }

        @Test
        @DisplayName("Mobil rasm ixtiyoriy")
        void mobileImageIsOptional() {
            Advertisement ad = homepageService.saveAdvertisement(null, null,
                    request("Mobilsiz", image("banner")));

            assertThat(ad.getImage()).isNotNull();
            assertThat(ad.getMobileImage()).isNull();
        }
    }

    // ------------------------------------------------------------ maydonlar

    @Nested
    @DisplayName("Maydonlar (ТЗ §27)")
    class Fields {

        @Test
        @DisplayName("createdAt va updatedAt to'ldiriladi")
        void timestampsArePopulated() {
            Advertisement ad = homepageService.saveAdvertisement(null, null,
                    request("Vaqt", image("banner")));
            AdvertisementDto dto = AdvertisementDto.from(ad);

            assertThat(dto.getCreatedAt()).isNotNull();
            // ТЗ §27 talab qiladi va u yo'q edi.
            assertThat(dto.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Sarlavha, tavsif va tugma matni UCH TILDA")
        void textsAreTranslated() {
            AdvertisementSaveRequest r = request("Uch til", image("banner"));
            r.setButtonEnabled(true);
            r.setTranslations(Map.of(
                    Locale.UZ, text("Aksiya", "Chegirma"),
                    Locale.RU, text("Акция", "Скидка"),
                    Locale.EN, text("Sale", "Discount")));

            AdvertisementDto dto = AdvertisementDto.from(
                    homepageService.saveAdvertisement(null, null, r));

            assertThat(dto.getTranslations()).containsKeys(Locale.UZ, Locale.RU, Locale.EN);
            assertThat(dto.getTranslations().get(Locale.RU).getTitle()).isEqualTo("Акция");
        }

        @Test
        @DisplayName("Ko'rsatish oynasi — startAt va endAt")
        void displayWindowIsStored() {
            AdvertisementSaveRequest r = request("Oyna", image("banner"));
            LocalDateTime from = LocalDateTime.now().plusDays(1);
            LocalDateTime to = LocalDateTime.now().plusDays(30);
            r.setStartAt(from);
            r.setEndAt(to);

            Advertisement ad = homepageService.saveAdvertisement(null, null, r);

            assertThat(ad.getStartAt()).isEqualTo(from);
            assertThat(ad.getEndAt()).isEqualTo(to);
        }
    }

    // -------------------------------------------------------- auditoriya

    @Nested
    @DisplayName("Auditoriya (R5)")
    class Audience {

        @Test
        @DisplayName("Reklama va admin e'loni ajratiladi")
        void audienceTypesAreDistinct() {
            AdvertisementSaveRequest ad = request("Reklama", image("banner"));
            ad.setAudience(AdAudience.ADVERTISEMENT);

            AdvertisementSaveRequest announcement = request("E'lon", image("banner"));
            announcement.setAudience(AdAudience.ADMIN_ANNOUNCEMENT);

            // Reklama faqat obunasi yo'qlarga, e'lon esa hammaga ko'rsatiladi.
            // Ular bitta jadvalda, lekin auditoriyasi boshqa.
            assertThat(homepageService.saveAdvertisement(null, null, ad).getAudience())
                    .isEqualTo(AdAudience.ADVERTISEMENT);
            assertThat(homepageService.saveAdvertisement(null, null, announcement).getAudience())
                    .isEqualTo(AdAudience.ADMIN_ANNOUNCEMENT);
        }
    }

    // ------------------------------------------------- karusel tartibi

    @Nested
    @DisplayName("Karusel tartibi (ТЗ §81.4)")
    class CarouselOrder {

        /**
         * ⚠️ Bu band ТЗ da bor, lekin sinalmagan edi.
         *
         * Tartib bannerlar uchun bezak emas: birinchi o'rin eng ko'p
         * ko'riladi va reklama beruvchiga aynan shu sotiladi. Tartib
         * bazadagi qo'shilish ketma-ketligiga bog'lanib qolsa,
         * admin uni boshqara olmasdi.
         */
        @Test
        @DisplayName("Ro'yxat `sortOrder` bo'yicha keladi, qo'shilish tartibida emas")
        void listFollowsSortOrder() {
            MediaAsset img = image("karusel");

            AdvertisementSaveRequest third = request("Uchinchi", img);
            third.setSortOrder(30);
            Advertisement c = homepageService.saveAdvertisement(null, null, third);

            AdvertisementSaveRequest first = request("Birinchi", img);
            first.setSortOrder(10);
            Advertisement a = homepageService.saveAdvertisement(null, null, first);

            AdvertisementSaveRequest second = request("Ikkinchi", img);
            second.setSortOrder(20);
            Advertisement b = homepageService.saveAdvertisement(null, null, second);

            List<Long> ids = homepageService.advertisements().stream()
                    .map(Advertisement::getId)
                    .filter(id -> id.equals(a.getId()) || id.equals(b.getId())
                            || id.equals(c.getId()))
                    .toList();

            assertThat(ids)
                    .as("qo'shilish tartibi teskari edi - ro'yxat sortOrder ga bo'ysunsin")
                    .containsExactly(a.getId(), b.getId(), c.getId());
        }

        @Test
        @DisplayName("Tartibni o'zgartirish ro'yxatni qayta joylaydi")
        void changingOrderMovesTheBanner() {
            MediaAsset img = image("karusel-2");

            AdvertisementSaveRequest r1 = request("Oldingi", img);
            r1.setSortOrder(10);
            Advertisement first = homepageService.saveAdvertisement(null, null, r1);

            AdvertisementSaveRequest r2 = request("Keyingi", img);
            r2.setSortOrder(20);
            Advertisement second = homepageService.saveAdvertisement(null, null, r2);

            // Ikkinchisini birinchi o'ringa ko'chiramiz.
            AdvertisementSaveRequest move = request("Keyingi", img);
            move.setSortOrder(5);
            homepageService.saveAdvertisement(null, second.getId(), move);

            List<Long> ids = homepageService.advertisements().stream()
                    .map(Advertisement::getId)
                    .filter(id -> id.equals(first.getId()) || id.equals(second.getId()))
                    .toList();

            assertThat(ids).containsExactly(second.getId(), first.getId());
        }
    }
}
