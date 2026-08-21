package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.*;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Repository.PurchaseRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Service.*;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §37 — entitlement mantiqi.
 *
 * Bu loyihaning eng nozik qismi: xato bo'lsa pullik kontent bepulga
 * ochilib ketadi yoki haq to'lagan foydalanuvchi ko'ra olmaydi.
 * Shuning uchun to'rtala manba ham, rad etish yo'llari ham qamrab olingan.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccessServiceTest {

    @Autowired private AccessService accessService;
    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private UserRepo userRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private PurchaseRepo purchaseRepo;

    private User viewer;

    @BeforeEach
    void createViewer() {
        viewer = userRepo.save(User.builder()
                .phone("+99890" + System.nanoTime() % 10_000_000)
                .name("Sinov tomoshabini")
                .roles(List.of())
                .build());
    }

    // ------------------------------------------------------------- yordamchi

    private Episode episodeWith(AccessPolicy contentPolicy, AccessPolicy episodeOverride,
                                PublicationStatus status) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.SERIES);
        c.setStructureType(StructureType.EPISODIC);
        c.setStatus(status);
        c.setAccessPolicy(contentPolicy);
        c.setPremierePrice(new BigDecimal("15000"));
        c.setTranslations(Translations.all("Kirish sinovi " + System.nanoTime()));
        Content content = contentService.create(null, c);

        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setEpisodeNumber(1);
        e.setStatus(status);
        e.setAccessPolicyOverride(episodeOverride);
        e.setPrice(new BigDecimal("3000"));
        e.setTranslations(Translations.all("1-qism"));
        return episodeService.saveEpisode(null, content.getId(), null, e);
    }

    private void givePremium(int months) {
        userAdminService.grantPremium(null, viewer.getId(), months, null);
    }

    private void buy(PurchaseType type, Long targetId) {
        purchaseRepo.save(Purchase.builder()
                .user(viewer).type(type).targetId(targetId)
                .amount(new BigDecimal("3000"))
                .build());
    }

    // ------------------------------------------------------------------ bepul

    @Nested
    @DisplayName("Bepul kontent")
    class Free {

        @Test
        @DisplayName("Anonim foydalanuvchi ham ko'ra oladi")
        void anonymousCanWatchFree() {
            Episode e = episodeWith(AccessPolicy.FREE, null, PublicationStatus.PUBLISHED);
            var d = accessService.canWatch(null, e);

            assertThat(d.isAllowed()).isTrue();
            assertThat(d.getReason()).isEqualTo(AccessDecision.Reason.FREE);
        }

        @Test
        @DisplayName("Qism darajasidagi FREE kontent siyosatini bekor qiladi")
        void episodeOverrideMakesItFree() {
            // Serial pullik, lekin 1-qism reklama uchun bepul
            Episode e = episodeWith(AccessPolicy.PREMIUM_OR_PURCHASE,
                    AccessPolicy.FREE, PublicationStatus.PUBLISHED);

            assertThat(accessService.canWatch(null, e).isAllowed())
                    .as("Reklama uchun 1-qism bepul bo'lishi kerak")
                    .isTrue();
        }
    }

    // ---------------------------------------------------------------- premium

    @Nested
    @DisplayName("Premium obuna")
    class Premium {

        @Test
        @DisplayName("Faol obuna PREMIUM_ONLY kontentni ochadi")
        void activePremiumOpensContent() {
            Episode e = episodeWith(AccessPolicy.PREMIUM_ONLY, null, PublicationStatus.PUBLISHED);
            assertThat(accessService.canWatch(viewer, e).isAllowed()).isFalse();

            givePremium(1);
            var d = accessService.canWatch(viewer, e);

            assertThat(d.isAllowed()).isTrue();
            assertThat(d.getReason()).isEqualTo(AccessDecision.Reason.PREMIUM);
        }

        @Test
        @DisplayName("Muddati o'tgan obuna ochmaydi")
        void expiredPremiumDoesNotOpen() {
            Episode e = episodeWith(AccessPolicy.PREMIUM_ONLY, null, PublicationStatus.PUBLISHED);
            givePremium(1);

            // Muddatni o'tmishga suramiz
            UserAccount account = accountRepo.findByUserId(viewer.getId()).orElseThrow();
            account.setPremiumUntil(LocalDateTime.now().minusDays(1));
            accountRepo.save(account);

            assertThat(accessService.canWatch(viewer, e).isAllowed()).isFalse();
        }

        @Test
        @DisplayName("PURCHASE_ONLY kontentni Premium OCHMAYDI")
        void premiumDoesNotOpenPurchaseOnly() {
            Episode e = episodeWith(AccessPolicy.PURCHASE_ONLY, null, PublicationStatus.PUBLISHED);
            givePremium(12);

            var d = accessService.canWatch(viewer, e);
            assertThat(d.isAllowed())
                    .as("PURCHASE_ONLY aynan sotib olishni talab qiladi")
                    .isFalse();
            assertThat(d.getRequiredAction()).isEqualTo(AccessDecision.RequiredAction.BUY_EPISODE);
        }
    }

    // ----------------------------------------------------------------- xarid

    @Nested
    @DisplayName("Bir martalik xarid")
    class OneTimePurchase {

        @Test
        @DisplayName("Qism xaridi aynan shu qismni ochadi")
        void episodePurchaseOpensEpisode() {
            Episode e = episodeWith(AccessPolicy.PURCHASE_ONLY, null, PublicationStatus.PUBLISHED);
            assertThat(accessService.canWatch(viewer, e).isAllowed()).isFalse();

            buy(PurchaseType.EPISODE, e.getId());
            var d = accessService.canWatch(viewer, e);

            assertThat(d.isAllowed()).isTrue();
            assertThat(d.getReason()).isEqualTo(AccessDecision.Reason.EPISODE_PURCHASE);
        }

        @Test
        @DisplayName("Premyera xaridi butun serialni ochadi")
        void premierePurchaseOpensWholeSeries() {
            Episode e = episodeWith(AccessPolicy.PURCHASE_ONLY, null, PublicationStatus.PUBLISHED);

            buy(PurchaseType.PREMIERE, e.getContent().getId());
            var d = accessService.canWatch(viewer, e);

            assertThat(d.isAllowed()).isTrue();
            assertThat(d.getReason()).isEqualTo(AccessDecision.Reason.PREMIERE_PURCHASE);
        }

        @Test
        @DisplayName("Boshqa qismning xaridi bu qismni OCHMAYDI")
        void purchaseOfAnotherEpisodeDoesNotOpen() {
            Episode e = episodeWith(AccessPolicy.PURCHASE_ONLY, null, PublicationStatus.PUBLISHED);

            buy(PurchaseType.EPISODE, e.getId() + 9999);

            assertThat(accessService.canWatch(viewer, e).isAllowed()).isFalse();
        }

        @Test
        @DisplayName("Qaytarilgan xarid ochmaydi")
        void refundedPurchaseDoesNotOpen() {
            Episode e = episodeWith(AccessPolicy.PURCHASE_ONLY, null, PublicationStatus.PUBLISHED);

            Purchase p = purchaseRepo.save(Purchase.builder()
                    .user(viewer).type(PurchaseType.EPISODE).targetId(e.getId())
                    .amount(new BigDecimal("3000"))
                    .refundedAt(LocalDateTime.now())
                    .build());

            assertThat(p.isValid()).isFalse();
            assertThat(accessService.canWatch(viewer, e).isAllowed()).isFalse();
        }
    }

    // ------------------------------------------------------------ rad etish

    @Nested
    @DisplayName("Rad etish holatlari")
    class Denials {

        @Test
        @DisplayName("Nashr qilinmagan kontentni sotib olgan ham ko'rmaydi")
        void unpublishedIsNeverWatchable() {
            Episode e = episodeWith(AccessPolicy.PURCHASE_ONLY, null, PublicationStatus.DRAFT);
            buy(PurchaseType.PREMIERE, e.getContent().getId());
            givePremium(12);

            var d = accessService.canWatch(viewer, e);
            assertThat(d.isAllowed()).isFalse();
            assertThat(d.getReason()).isEqualTo(AccessDecision.Reason.NOT_PUBLISHED);
        }

        @Test
        @DisplayName("Bloklangan foydalanuvchi Premium bilan ham ko'rmaydi")
        void blockedUserCannotWatch() {
            Episode e = episodeWith(AccessPolicy.PREMIUM_ONLY, null, PublicationStatus.PUBLISHED);
            givePremium(12);
            userAdminService.setBlocked(null, viewer.getId(), true, "sinov");

            var d = accessService.canWatch(viewer, e);
            assertThat(d.isAllowed()).isFalse();
            assertThat(d.getReason()).isEqualTo(AccessDecision.Reason.USER_BLOCKED);
        }

        @Test
        @DisplayName("Anonim foydalanuvchiga kirish taklif qilinadi, narxlar bilan")
        void anonymousGetsSignInAndPrices() {
            Episode e = episodeWith(AccessPolicy.PREMIUM_OR_PURCHASE, null, PublicationStatus.PUBLISHED);

            var d = accessService.canWatch(null, e);
            assertThat(d.isAllowed()).isFalse();
            assertThat(d.getReason()).isEqualTo(AccessDecision.Reason.NOT_AUTHENTICATED);
            assertThat(d.getRequiredAction()).isEqualTo(AccessDecision.RequiredAction.SIGN_IN);
            assertThat(d.getEpisodePrice()).isEqualByComparingTo("3000");
            assertThat(d.getPremierePrice()).isEqualByComparingTo("15000");
        }

        @Test
        @DisplayName("PREMIUM_OR_PURCHASE da ikkala yo'l ham taklif qilinadi")
        void bothOptionsOffered() {
            Episode e = episodeWith(AccessPolicy.PREMIUM_OR_PURCHASE, null, PublicationStatus.PUBLISHED);

            var d = accessService.canWatch(viewer, e);
            assertThat(d.getRequiredAction())
                    .isEqualTo(AccessDecision.RequiredAction.BUY_OR_SUBSCRIBE);
        }
    }

    // -------------------------------------------------- casting va reklama

    @Nested
    @DisplayName("Casting loyihasi va reklama")
    class CastingAndAds {

        @Test
        @DisplayName("Bir martalik xarid casting huquqini BERMAYDI")
        void purchaseDoesNotGrantCasting() {
            Episode e = episodeWith(AccessPolicy.PURCHASE_ONLY, null, PublicationStatus.PUBLISHED);
            buy(PurchaseType.PREMIERE, e.getContent().getId());

            assertThat(accessService.canAccessCasting(viewer))
                    .as("Buyurtmachi: xarid casting huquqini bermaydi")
                    .isFalse();
        }

        @Test
        @DisplayName("Premium casting huquqini beradi")
        void premiumGrantsCasting() {
            givePremium(1);
            assertThat(accessService.canAccessCasting(viewer)).isTrue();
        }

        @Test
        @DisplayName("Bloklangan foydalanuvchida casting yo'q")
        void blockedHasNoCasting() {
            givePremium(1);
            userAdminService.setBlocked(null, viewer.getId(), true, "sinov");
            assertThat(accessService.canAccessCasting(viewer)).isFalse();
        }

        @Test
        @DisplayName("Reklama: Premium'da yo'q, qolganlarda bor")
        void adsOnlyForNonPremium() {
            assertThat(accessService.shouldShowAds(null))
                    .as("Anonimga reklama ko'rsatiladi").isTrue();
            assertThat(accessService.shouldShowAds(viewer)).isTrue();

            givePremium(1);
            assertThat(accessService.shouldShowAds(viewer))
                    .as("Premium — «reklamasiz tomosha»").isFalse();
        }
    }
}
