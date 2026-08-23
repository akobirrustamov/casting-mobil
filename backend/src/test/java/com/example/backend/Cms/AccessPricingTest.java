package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Service.AccessDecision;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.Cms.Service.SettingKeys;
import com.example.backend.Cms.Service.SettingsService;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §23 — kirish siyosati va narx.
 *
 * <h2>Asosiy talab</h2>
 * «Bitta seriyani sotib olish default narxi 3 000 UZS. Lekin bu kod ichida
 * hardcoded bo'lmasin. Admin settings orqali o'zgartirish mumkin bo'lsin.»
 *
 * <h2>Nima tekshiriladi</h2>
 * Narx haqiqatan sozlamadan olinishi — ya'ni sozlama o'zgarsa javob ham
 * o'zgarishi. Agar qiymat kodda qotirilgan yoki ishga tushganda bir marta
 * o'qib qo'yilgan bo'lsa, bu test yiqiladi.
 *
 * ⚠️ Qismning O'Z narxi bo'lsa u ustun turadi — sozlama faqat zaxira.
 * Buni ham alohida tekshiramiz, chunki ikkalasini chalkashtirish oson.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccessPricingTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private AccessService accessService;
    @Autowired private SettingsService settingsService;

    /** Pullik serial + bitta qism. Qism narxi ATAYLAB berilmaydi. */
    private Episode paidEpisode(BigDecimal episodePrice) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.SERIES);
        c.setStructureType(StructureType.EPISODIC);
        c.setAccessPolicy(AccessPolicy.PREMIUM_OR_PURCHASE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setPremierePrice(new BigDecimal("50000"));
        c.setTranslations(Translations.all("Narx sinovi " + SEQ.incrementAndGet()));
        Content series = contentService.create(null, c);

        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setEpisodeNumber(1);
        e.setStatus(PublicationStatus.PUBLISHED);
        e.setPrice(episodePrice);
        e.setTranslations(Translations.all("Qism"));
        return episodeService.saveEpisode(null, series.getId(), null, e);
    }

    // ------------------------------------------------------------ narx

    @Nested
    @DisplayName("Narx sozlamadan olinadi")
    class PriceFromSettings {

        @Test
        @DisplayName("Standart qiymat — 3000 so'm (ТЗ §23)")
        void defaultIsThreeThousand() {
            assertThat(settingsService.getMoney(SettingKeys.EPISODE_PRICE))
                    .isEqualByComparingTo("3000");
        }

        @Test
        @DisplayName("Qism narxi berilmasa — sozlamadagi qiymat ishlatiladi")
        void fallsBackToSetting() {
            Episode episode = paidEpisode(null);

            AccessDecision decision = accessService.canWatch(null, episode);

            assertThat(decision.isAllowed()).isFalse();
            assertThat(decision.getEpisodePrice())
                    .as("Narx sozlamadan olinishi kerak, kodda qotirilmagan")
                    .isEqualByComparingTo("3000");
        }

        @Test
        @DisplayName("Sozlama o'zgarsa narx ham DARHOL o'zgaradi")
        void settingChangeTakesEffectImmediately() {
            Episode episode = paidEpisode(null);

            assertThat(accessService.canWatch(null, episode).getEpisodePrice())
                    .isEqualByComparingTo("3000");

            // Admin narxni oshirdi.
            settingsService.update(null, SettingKeys.EPISODE_PRICE, "7500");

            // ⚠️ Ilova QAYTA ISHGA TUSHIRILMADI. Agar qiymat ishga tushganda
            // bir marta o'qilgan yoki keshlangan bo'lsa, bu yerda hali ham
            // 3000 qaytardi — ya'ni "admin settings orqali o'zgartirish
            // mumkin" degan talab bajarilmagan bo'lardi.
            assertThat(accessService.canWatch(null, episode).getEpisodePrice())
                    .as("Sozlama o'zgargach narx darhol yangilanishi kerak")
                    .isEqualByComparingTo("7500");
        }

        /**
         * ⚠️ SERIAL ustida sinaladi, film ustida emas.
         *
         * Pullik SINGLE kontent uchun narx ATAYLAB majburiy
         * ({@code ContentService.validate}): film — sotiladigan yagona
         * narsa, uning narxi juda xilma-xil va uni global standartga
         * qoldirish xato bo'lardi.
         *
         * Serialda esa premyera narxi ixtiyoriy: asosiy savdo qismlar
         * bo'yicha ketadi, «butun premyerani sotib olish» esa qo'shimcha
         * imkoniyat. Shuning uchun zaxira aynan shu yerda ma'noga ega.
         */
        @Test
        @DisplayName("Premyera narxi ham sozlamadan (serial uchun)")
        void premierePriceAlsoFromSetting() {
            settingsService.update(null, SettingKeys.PREMIERE_PRICE, "99000");

            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.SERIES);
            c.setStructureType(StructureType.EPISODIC);
            c.setAccessPolicy(AccessPolicy.PURCHASE_ONLY);
            c.setStatus(PublicationStatus.PUBLISHED);
            // premierePrice ATAYLAB berilmaydi
            c.setTranslations(Translations.all("Narxsiz serial " + SEQ.incrementAndGet()));
            Content series = contentService.create(null, c);

            EpisodeSaveRequest e = new EpisodeSaveRequest();
            e.setEpisodeNumber(1);
            e.setStatus(PublicationStatus.PUBLISHED);
            e.setTranslations(Translations.all("Qism"));
            Episode episode = episodeService.saveEpisode(null, series.getId(), null, e);

            assertThat(accessService.canWatch(null, episode).getPremierePrice())
                    .isEqualByComparingTo("99000");
        }

        @Test
        @DisplayName("Pullik FILM uchun narx majburiy — sozlamaga qoldirilmaydi")
        void paidSingleContentRequiresExplicitPrice() {
            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.MOVIE);
            c.setStructureType(StructureType.SINGLE);
            c.setAccessPolicy(AccessPolicy.PURCHASE_ONLY);
            c.setStatus(PublicationStatus.PUBLISHED);
            c.setTranslations(Translations.all("Narxsiz film " + SEQ.incrementAndGet()));

            // Film — sotiladigan yagona narsa. Uni global standart narxga
            // qoldirish jimgina noto'g'ri narx qo'yishga olib kelardi.
            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() -> contentService.create(null, c))
                    .hasMessageContaining("narx");
        }
    }

    // -------------------------------------------------------- ustunlik

    @Nested
    @DisplayName("Qismning o'z narxi ustun")
    class EpisodeOwnPrice {

        @Test
        @DisplayName("O'z narxi bo'lsa sozlama e'tiborga olinmaydi")
        void ownPriceWinsOverSetting() {
            Episode episode = paidEpisode(new BigDecimal("12000"));

            settingsService.update(null, SettingKeys.EPISODE_PRICE, "3000");

            assertThat(accessService.canWatch(null, episode).getEpisodePrice())
                    .as("Admin qismga alohida narx qo'ygan — sozlama uni bosmasin")
                    .isEqualByComparingTo("12000");
        }
    }

    // -------------------------------------------------------- siyosat

    @Nested
    @DisplayName("Kirish siyosati (ТЗ §23)")
    class Policies {

        @Test
        @DisplayName("To'rtala qiymat ham mavjud")
        void allFourPoliciesExist() {
            assertThat(AccessPolicy.values()).containsExactlyInAnyOrder(
                    AccessPolicy.FREE, AccessPolicy.PREMIUM_ONLY,
                    AccessPolicy.PURCHASE_ONLY, AccessPolicy.PREMIUM_OR_PURCHASE);
        }

        @Test
        @DisplayName("Qism darajasidagi override kontentnikini bosadi")
        void episodeOverrideWins() {
            // Kontent pullik, lekin 1-qism bepul «ilinma» sifatida ochiq.
            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.SERIES);
            c.setStructureType(StructureType.EPISODIC);
            c.setAccessPolicy(AccessPolicy.PREMIUM_ONLY);
            c.setStatus(PublicationStatus.PUBLISHED);
            c.setTranslations(Translations.all("Ilinma " + SEQ.incrementAndGet()));
            Content series = contentService.create(null, c);

            EpisodeSaveRequest e = new EpisodeSaveRequest();
            e.setEpisodeNumber(1);
            e.setStatus(PublicationStatus.PUBLISHED);
            e.setAccessPolicyOverride(AccessPolicy.FREE);
            e.setTranslations(Translations.all("1-qism"));
            Episode episode = episodeService.saveEpisode(null, series.getId(), null, e);

            assertThat(episode.effectiveAccessPolicy()).isEqualTo(AccessPolicy.FREE);
            assertThat(accessService.canWatch(null, episode).isAllowed()).isTrue();
        }

        @Test
        @DisplayName("Override bo'lmasa kontentnikini meros oladi")
        void inheritsFromContentWithoutOverride() {
            Episode episode = paidEpisode(null);

            assertThat(episode.getAccessPolicyOverride()).isNull();
            assertThat(episode.effectiveAccessPolicy())
                    .isEqualTo(AccessPolicy.PREMIUM_OR_PURCHASE);
        }
    }
}
