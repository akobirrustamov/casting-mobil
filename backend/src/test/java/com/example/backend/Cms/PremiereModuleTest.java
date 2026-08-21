package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.InternalLinkDto;
import com.example.backend.Admin.Dto.PremiereDto;
import com.example.backend.Admin.Dto.PremiereSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.Premiere;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.InternalTargetType;
import com.example.backend.Cms.Enums.LinkType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.ContentService;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §30 — «Yangi premyeralar» moduli.
 *
 * <h2>Reklamadan farqi</h2>
 * Premyera — kontent haqidagi e'lon («tez kunda chiqadi», «treylerni
 * ko'ring»), reklama esa tijorat banneri. Ikkalasi ham bosh sahifada,
 * lekin alohida bo'limlarda. Havola mexanizmi esa umumiy (§28).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PremiereModuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private HomepageService homepageService;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private ContentService contentService;

    // ------------------------------------------------------------ maydonlar

    @Nested
    @DisplayName("Kartochka maydonlari")
    class Fields {

        @Test
        @DisplayName("ТЗ dagi barcha maydonlar saqlanadi")
        void allFieldsArePersisted() {
            MediaAsset poster = media("poster", MediaType.IMAGE);
            MediaAsset trailer = media("trailer", MediaType.VIDEO);
            Content film = content();

            PremiereSaveRequest r = request("Qalbing egasi");
            r.setImageMediaId(poster.getId());
            r.setVideoMediaId(trailer.getId());
            r.setContentId(film.getId());
            r.setSortOrder(7);
            r.setStartAt(LocalDateTime.now().minusDays(1));
            r.setEndAt(LocalDateTime.now().plusDays(30));

            Premiere p = homepageService.savePremiere(null, null, r);

            assertThat(p.getImage()).isNotNull();
            assertThat(p.getVideo()).isNotNull();
            assertThat(p.getContent().getId()).isEqualTo(film.getId());
            assertThat(p.getSortOrder()).isEqualTo(7);
            assertThat(p.getStartAt()).isNotNull();
            assertThat(p.getEndAt()).isNotNull();
            assertThat(p.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        }

        @Test
        @DisplayName("Uch qatorli matn: sarlavha, ustki matn, tavsif")
        void threeTextLines() {
            PremiereSaveRequest r = request("Uch qator");
            PremiereDto.PremiereTextDto uz = r.getTranslations().get(Locale.UZ);
            uz.setSubtitle("Tez kunda");
            uz.setDescription("Treylerni ko'rish");

            Premiere p = homepageService.savePremiere(null, null, r);

            assertThat(p.getTranslations()).anySatisfy(t -> {
                assertThat(t.getSubtitle()).isEqualTo("Tez kunda");
                assertThat(t.getDescription()).isEqualTo("Treylerni ko'rish");
            });
        }

        @Test
        @DisplayName("Rasm ham, video ham ixtiyoriy")
        void mediaIsOptional() {
            Premiere p = homepageService.savePremiere(null, null, request("Mediasiz"));

            assertThat(p.getImage()).isNull();
            assertThat(p.getVideo()).isNull();
        }

        @Test
        @DisplayName("Tugash sanasi boshlanishdan oldin bo'lolmaydi")
        void windowIsValidated() {
            PremiereSaveRequest r = request("Teskari oyna");
            r.setStartAt(LocalDateTime.now().plusDays(10));
            r.setEndAt(LocalDateTime.now().plusDays(2));

            assertThatThrownBy(() -> homepageService.savePremiere(null, null, r))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // --------------------------------------------------------------- havola

    @Nested
    @DisplayName("Havola (§28 bilan umumiy mexanizm)")
    class Links {

        @Test
        @DisplayName("Ilova ichidagi kontentga o'tadi")
        void internalContentLink() {
            Content film = content();
            PremiereSaveRequest r = request("Filmga havola");
            r.setLink(link(LinkType.INTERNAL, null, InternalTargetType.CONTENT, film.getId()));

            Premiere p = homepageService.savePremiere(null, null, r);

            assertThat(p.getLink().isActionable()).isTrue();
            assertThat(p.getLink().getInternalTargetId()).isEqualTo(film.getId());
        }

        @Test
        @DisplayName("Boshqa saytga o'tadi")
        void externalLink() {
            PremiereSaveRequest r = request("Tashqi havola");
            r.setLink(link(LinkType.EXTERNAL, "https://uzcasting.uz/premyera", null, null));

            assertThat(homepageService.savePremiere(null, null, r)
                    .getLink().isActionable()).isTrue();
        }

        @Test
        @DisplayName("Mavjud bo'lmagan nishon rad etiladi")
        void deadLinkIsRejected() {
            PremiereSaveRequest r = request("O'lik havola");
            r.setLink(link(LinkType.INTERNAL, null, InternalTargetType.CONTENT, 999_999L));

            // Aks holda mobil ilovada hech qayerga olib bormaydigan
            // kartochka paydo bo'lardi va buni faqat foydalanuvchi bilardi.
            assertThatThrownBy(() -> homepageService.savePremiere(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("topilmadi");
        }

        @Test
        @DisplayName("ТЗ §30 dagi barcha nishon turlari mavjud")
        void allTargetTypesFromSpecExist() {
            // ТЗ: film · serial · episode · creator · casting.
            // Film va serial — ikkalasi ham CONTENT (§13: tur ≠ kategoriya).
            assertThat(InternalTargetType.values())
                    .contains(InternalTargetType.CONTENT,
                            InternalTargetType.EPISODE,
                            InternalTargetType.CREATOR,
                            InternalTargetType.CASTING);
        }

        @Test
        @DisplayName("Havolasiz kartochka ham to'g'ri")
        void linkIsOptional() {
            Premiere p = homepageService.savePremiere(null, null, request("Havolasiz"));

            assertThat(p.getLink() == null || !p.getLink().isActionable()).isTrue();
        }
    }

    // ------------------------------------------------------------- 3 til

    @Nested
    @DisplayName("Uch til qoidasi")
    class ThreeLanguages {

        @Test
        @DisplayName("Qoralama uchun o'zbekcha yetarli")
        void draftNeedsOnlyBaseLanguage() {
            Premiere p = homepageService.savePremiere(null, null, request("Qoralama"));

            assertThat(p.getTranslations()).hasSize(1);
        }

        @Test
        @DisplayName("Nashrda uchala til majburiy")
        void publishRequiresAllThree() {
            PremiereSaveRequest r = request("Yetim premyera");
            r.setStatus(PublicationStatus.PUBLISHED);

            assertThatThrownBy(() -> homepageService.savePremiere(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Uchala til to'ldirilgan bo'lsa nashr o'tadi")
        void publishSucceedsWithAllThree() {
            PremiereSaveRequest r = request("To'liq premyera");
            r.setStatus(PublicationStatus.PUBLISHED);
            fillAllLocales(r, "To'liq premyera");

            Premiere p = homepageService.savePremiere(null, null, r);

            assertThat(p.getTranslations()).hasSize(3);
            assertThat(p.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        }

        @Test
        @DisplayName("SCHEDULED ham nashr hisoblanadi")
        void scheduledCountsAsUserVisible() {
            PremiereSaveRequest r = request("Rejalashtirilgan");
            r.setStatus(PublicationStatus.SCHEDULED);

            // Belgilangan vaqtda o'zi PUBLISHED bo'ladi — tarjima yo'qligi
            // o'shanda bilinsa, kech bo'lardi.
            assertThatThrownBy(() -> homepageService.savePremiere(null, null, r))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Tugma yoqilgan bo'lsa uning matni ham 3 tilda")
        void buttonTextNeedsAllThree() {
            PremiereSaveRequest r = request("Tugmali premyera");
            r.setStatus(PublicationStatus.PUBLISHED);
            fillAllLocales(r, "Tugmali premyera");
            r.setButtonEnabled(true);
            // Faqat o'zbekchada tugma matni — rus tilidagi ekranda
            // o'zbekcha tugma turardi.
            r.getTranslations().get(Locale.UZ).setButtonText("Treylerni ko'rish");

            assertThatThrownBy(() -> homepageService.savePremiere(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tugma matni");
        }

        @Test
        @DisplayName("Ustki matn («Tez kunda») ham uch tilda")
        void subtitleNeedsAllThree() {
            PremiereSaveRequest r = request("Ustki matnli");
            r.setStatus(PublicationStatus.PUBLISHED);
            fillAllLocales(r, "Ustki matnli");
            // Faqat o'zbekchada — rus tilidagi ekranda o'zbekcha qator
            // turardi va kartochka yarim tarjima bo'lib chiqardi.
            r.getTranslations().get(Locale.UZ).setSubtitle("Tez kunda");

            assertThatThrownBy(() -> homepageService.savePremiere(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ustki matn");
        }

        @Test
        @DisplayName("Tavsif ham uch tilda")
        void descriptionNeedsAllThree() {
            PremiereSaveRequest r = request("Tavsifli");
            r.setStatus(PublicationStatus.PUBLISHED);
            fillAllLocales(r, "Tavsifli");
            r.getTranslations().get(Locale.UZ).setDescription("Treylerni ko'rish");

            assertThatThrownBy(() -> homepageService.savePremiere(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tavsif");
        }

        @Test
        @DisplayName("Uch qatorli kartochka uchala tilda to'liq — nashr o'tadi")
        void fullyTranslatedCardPublishes() {
            PremiereSaveRequest r = request("Qalbing egasi");
            r.setStatus(PublicationStatus.PUBLISHED);
            r.setButtonEnabled(true);

            // ТЗ §30 misolidagi kartochka, uchala tilda:
            //   Qalbing egasi / Tez kunda / Treylerni ko'rish
            fill(r, Locale.UZ, "Qalbing egasi", "Tez kunda", "Treylerni ko'rish");
            fill(r, Locale.RU, "Владелец сердца", "Скоро", "Смотреть трейлер");
            fill(r, Locale.EN, "Owner of the heart", "Coming soon", "Watch trailer");

            Premiere p = homepageService.savePremiere(null, null, r);

            assertThat(p.getTranslations()).hasSize(3);
            assertThat(p.getTranslations()).allSatisfy(t -> {
                assertThat(t.getTitle()).isNotBlank();
                assertThat(t.getSubtitle()).isNotBlank();
                assertThat(t.getDescription()).isNotBlank();
                assertThat(t.getButtonText()).isNotBlank();
            });
        }

        @Test
        @DisplayName("Matn bo'sh bo'lsa tarjima talab qilinmaydi")
        void emptyOptionalTextsAreFine() {
            PremiereSaveRequest r = request("Faqat sarlavha");
            r.setStatus(PublicationStatus.PUBLISHED);
            fillAllLocales(r, "Faqat sarlavha");
            r.setButtonEnabled(false);

            // Kartochka faqat sarlavhadan iborat bo'lishi mumkin.
            Premiere p = homepageService.savePremiere(null, null, r);

            assertThat(p.getTranslations()).hasSize(3);
        }

        @Test
        @DisplayName("⚠️ Sarlavhasiz matn JIMGINA yo'qolmaydi")
        void textWithoutTitleIsNotSilentlyDropped() {
            PremiereSaveRequest r = request("Sarlavhasiz qator");
            // Admin rus tabida ustki matnni yozdi, sarlavhani unutdi.
            r.getTranslations().put(Locale.RU,
                    PremiereDto.PremiereTextDto.builder().subtitle("Скоро").build());

            // Ilgari butun RU qatori o'tkazib yuborilardi: saqlash
            // muvaffaqiyatli ko'rinardi, matn esa izsiz yo'qolardi.
            assertThatThrownBy(() -> homepageService.savePremiere(null, null, r))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Tugma o'chirilgan bo'lsa matn talab qilinmaydi")
        void disabledButtonNeedsNoText() {
            PremiereSaveRequest r = request("Tugmasiz premyera");
            r.setStatus(PublicationStatus.PUBLISHED);
            fillAllLocales(r, "Tugmasiz premyera");
            r.setButtonEnabled(false);
            r.getTranslations().get(Locale.UZ).setButtonText("Faqat o'zbekcha");

            assertThat(homepageService.savePremiere(null, null, r)
                    .getButtonEnabled()).isFalse();
        }
    }

    // ------------------------------------------------------------- yordamchi

    private PremiereSaveRequest request(String title) {
        PremiereSaveRequest r = new PremiereSaveRequest();
        r.setName(title + " #" + SEQ.incrementAndGet());
        r.setStatus(PublicationStatus.DRAFT);
        Map<Locale, PremiereDto.PremiereTextDto> tr = new LinkedHashMap<>();
        tr.put(Locale.UZ, PremiereDto.PremiereTextDto.builder().title(title).build());
        r.setTranslations(tr);
        return r;
    }

    /** Bitta tilning barcha matnlarini to'ldiradi. */
    private void fill(PremiereSaveRequest r, Locale locale,
                      String title, String subtitle, String buttonText) {
        r.getTranslations().put(locale, PremiereDto.PremiereTextDto.builder()
                .title(title)
                .subtitle(subtitle)
                .description(subtitle + " — " + title)
                .buttonText(buttonText)
                .build());
    }

    private void fillAllLocales(PremiereSaveRequest r, String title) {
        r.getTranslations().put(Locale.RU,
                PremiereDto.PremiereTextDto.builder().title(title + " RU").build());
        r.getTranslations().put(Locale.EN,
                PremiereDto.PremiereTextDto.builder().title(title + " EN").build());
    }

    private InternalLinkDto link(LinkType type, String url,
                                 InternalTargetType targetType, Long targetId) {
        InternalLinkDto l = new InternalLinkDto();
        l.setLinkType(type);
        l.setLinkUrl(url);
        l.setInternalTargetType(targetType);
        l.setInternalTargetId(targetId);
        return l;
    }

    private MediaAsset media(String name, MediaType type) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/" + name + "-" + SEQ.incrementAndGet())
                .originalFilename(name)
                .type(type)
                .mimeType(type == MediaType.IMAGE ? "image/jpeg" : "video/mp4")
                .sizeBytes(100L)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Content content() {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.DRAFT);
        req.setTranslations(com.example.backend.support.Translations.all(
                "Premyera filmi " + SEQ.incrementAndGet()));
        return contentService.create(null, req);
    }
}
