package com.example.backend.Cms;

import com.example.backend.Admin.Dto.AdvertisementDto;
import com.example.backend.Admin.Dto.AdvertisementSaveRequest;
import com.example.backend.Cms.Entity.Advertisement;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.AdAudience;
import com.example.backend.Cms.Enums.AdPlacement;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Cms.Service.HomepageService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Butun ekranni yopadigan banner — «Majburiy reklama» (maket, 09.09.2026).
 *
 * <h2>Bu yerda nima jimgina buziladi</h2>
 * Ikkita qoida bir-biriga o'xshaydi va ularni chalkashtirish oson:
 *
 * <ul>
 *   <li>KIMGA ko'rinadi — {@code AdAudience}. Tijorat reklamasi faqat
 *       obunasi YO'Q odamga; admin e'loni hammaga. Buzilsa, pul to'lagan
 *       odam «reklamasiz tomosha» o'rniga butun ekranni yopadigan
 *       reklamani oladi — bu to'g'ridan-to'g'ri va'daning buzilishi;</li>
 *   <li>QAYERDA ko'rinadi — {@code AdPlacement}. To'liq ekranga
 *       qo'yilgani karuselda ham chiqmasligi kerak, aks holda odam
 *       bitta reklamani ketma-ket ikki marta ko'radi.</li>
 * </ul>
 *
 * Ikkalasi ham ekranda «shunchaki reklama ko'rindi» kabi ko'rinadi,
 * shuning uchun ular testda ajratilgan.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InterstitialAdTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private HomepageService homepageService;
    @Autowired private HomeFeedService homeFeedService;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private UserAdminService userAdminService;

    // ------------------------------------------------------------- yordamchi

    private MediaAsset image() {
        int n = SEQ.incrementAndGet();
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/banner-" + n + ".jpg")
                .originalFilename("banner.jpg")
                .type(MediaType.IMAGE)
                .mimeType("image/jpeg")
                .sizeBytes(1024L)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Advertisement banner(AdAudience audience, AdPlacement placement) {
        AdvertisementSaveRequest r = new AdvertisementSaveRequest();
        r.setName("Banner " + SEQ.incrementAndGet());
        r.setImageMediaId(image().getId());
        r.setAudience(audience);
        r.setPlacement(placement);
        r.setStatus(PublicationStatus.PUBLISHED);
        // ⚠️ Uchala til ham majburiy: foydalanuvchiga ko'rinadigan
        // matn bir tilda qolib ketmasligi kerak (TranslationRules).
        r.setTranslations(titles("Reklama"));
        return homepageService.saveAdvertisement(null, null, r);
    }

    /** Uchala tilda sarlavha — TranslationRules shuni talab qiladi. */
    private Map<Locale, AdvertisementDto.AdTextDto> titles(String base) {
        int n = SEQ.incrementAndGet();
        return Map.of(
                Locale.UZ, AdvertisementDto.AdTextDto.builder().title(base + ' ' + n).build(),
                Locale.RU, AdvertisementDto.AdTextDto.builder().title(base + " RU " + n).build(),
                Locale.EN, AdvertisementDto.AdTextDto.builder().title(base + " EN " + n).build());
    }

    private User person() {
        return userRepo.save(User.builder()
                .phone("+99890" + (4_000_000 + SEQ.incrementAndGet()))
                .name("Tomoshabin")
                .roles(List.of())
                .build());
    }

    @Nested
    @DisplayName("Kimga ko'rinadi")
    class Audience {

        @Test
        @DisplayName("Mehmon tijorat reklamasini ko'radi")
        void guestSeesAd() {
            banner(AdAudience.ADVERTISEMENT, AdPlacement.INTERSTITIAL);

            assertThat(homeFeedService.interstitial(null, Locale.UZ)).isNotNull();
        }

        /**
         * ⚠️ Eng muhim tekshiruv: Premium — bu «reklamasiz tomosha».
         *
         * Buzilsa, pul to'lagan odam butun ekranni yopadigan reklamani
         * oladi. Bu xato ekranda «reklama ishlayapti» deb ko'rinadi.
         */
        @Test
        @DisplayName("Obunachi tijorat reklamasini KO'RMAYDI")
        void subscriberSeesNoAd() {
            banner(AdAudience.ADVERTISEMENT, AdPlacement.INTERSTITIAL);

            User subscriber = person();
            userAdminService.grantPremium(null, subscriber.getId(), 1, null);

            assertThat(homeFeedService.interstitial(subscriber, Locale.UZ)).isNull();
        }

        @Test
        @DisplayName("Admin e'loni obunachiga ham ko'rinadi")
        void announcementReachesEveryone() {
            banner(AdAudience.ADMIN_ANNOUNCEMENT, AdPlacement.INTERSTITIAL);

            User subscriber = person();
            userAdminService.grantPremium(null, subscriber.getId(), 1, null);

            assertThat(homeFeedService.interstitial(subscriber, Locale.UZ)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Qayerda ko'rinadi")
    class Placement {

        @Test
        @DisplayName("Karusel uchun qo'yilgani to'liq ekranga chiqmaydi")
        void feedBannerIsNotInterstitial() {
            banner(AdAudience.ADVERTISEMENT, AdPlacement.FEED);

            assertThat(homeFeedService.interstitial(null, Locale.UZ)).isNull();
        }

        /**
         * ⚠️ Teskari tomoni: to'liq ekranga qo'yilgani karuselda ham
         * chiqmasligi kerak. Aks holda odam bitta reklamani ketma-ket
         * ikki marta ko'radi — avval ekran ustidan, keyin qatorda.
         */
        @Test
        @DisplayName("To'liq ekranga qo'yilgani karuselda chiqmaydi")
        void interstitialIsNotInFeed() {
            Advertisement popup = banner(AdAudience.ADVERTISEMENT, AdPlacement.INTERSTITIAL);

            boolean inCarousel = homeFeedService.build(null, Locale.UZ).getSections().stream()
                    .flatMap(s -> s.getBanners().stream())
                    .anyMatch(b -> b.getId().equals(popup.getId()));

            assertThat(inCarousel).isFalse();
        }

        @Test
        @DisplayName("Sukut bo'yicha — karusel")
        void defaultIsFeed() {
            AdvertisementSaveRequest r = new AdvertisementSaveRequest();
            r.setName("Eski klient " + SEQ.incrementAndGet());
            r.setImageMediaId(image().getId());
            r.setAudience(AdAudience.ADVERTISEMENT);
            r.setStatus(PublicationStatus.PUBLISHED);
            r.setTranslations(titles("Eski"));

            // ⚠️ `placement` UMUMAN berilmagan — eski admin panel uni
            // yubormaydi. Uning bannerlari karuselda qolishi kerak.
            Advertisement ad = homepageService.saveAdvertisement(null, null, r);

            assertThat(ad.getPlacement()).isEqualTo(AdPlacement.FEED);
        }
    }

    @Nested
    @DisplayName("Manzil")
    class Endpoint {

        @Test
        @DisplayName("Ko'rsatadigan narsa yo'q — 204, bo'sh tana emas")
        void noContentWhenNothingToShow() throws Exception {
            mockMvc.perform(get("/api/v1/app/ads/interstitial"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Manzil mehmonga ochiq")
        void openForGuests() throws Exception {
            banner(AdAudience.ADVERTISEMENT, AdPlacement.INTERSTITIAL);

            // ⚠️ Aynan mehmon reklamani ko'radi. Manzil yopiq bo'lsa,
            // reklama faqat kirganlarga chiqardi — ya'ni teskarisi.
            mockMvc.perform(get("/api/v1/app/ads/interstitial"))
                    .andExpect(status().isOk());
        }
    }
}
