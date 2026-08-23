package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.SeasonSaveRequest;
import com.example.backend.Cms.Controller.ContentController;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.Purchase;
import com.example.backend.Cms.Entity.Season;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.PurchaseType;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.PurchaseRepo;
import com.example.backend.Cms.Service.AccessDecision;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.CapturingStatementInspector;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/v1/app/content/{id}/episodes} — qismlar ro'yxati.
 *
 * <h2>Nima uchun bu endpoint qo'shildi</h2>
 * {@code /watch/content/{id}} faqat YAXLIT kontentni ochadi, ko'p qismlisiga
 * «qaysi qism?» deb javob beradi. Ilovada esa qism identifikatorini oladigan
 * joy yo'q edi — serial, mini-serial va podkast UMUMAN ochilmasdi. Dev
 * bazasida bu 8 kartochkadan 4 tasi degani.
 *
 * <h2>Bu yerda nima qo'riqlanadi</h2>
 * <ul>
 *   <li>ro'yxat video manzilini BERMAYDI — aks holda pullik qismni ochish
 *       uchun ro'yxatning o'zi yetarli bo'lardi;</li>
 *   <li>ro'yxatdagi qulf va ochish sahifasidagi qulf BIR xil qoidadan
 *       chiqadi (ТЗ §37) — ular ajralib ketmasligi tekshiriladi;</li>
 *   <li>xaridlar bir marta o'qiladi, har qism uchun qaytadan emas.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EpisodeListTest {

    @Autowired private ContentController controller;
    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private AccessService accessService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private PurchaseRepo purchaseRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private MockMvc mockMvc;

    private User viewer;

    @BeforeEach
    void createViewer() {
        viewer = userRepo.save(User.builder()
                .phone("+99890" + System.nanoTime() % 10_000_000)
                .name("Qism ro'yxati sinovi")
                .roles(List.of())
                .build());
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------- yordamchi

    private Content series(StructureType structure, AccessPolicy policy) {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.SERIES);
        c.setStructureType(structure);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setAccessPolicy(policy);
        c.setPremierePrice(new BigDecimal("15000"));
        c.setTranslations(Translations.all("Serial " + System.nanoTime()));
        return contentService.create(null, c);
    }

    private Episode episode(Content content, Long seasonId, int number,
                            PublicationStatus status, AccessPolicy override) {
        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setSeasonId(seasonId);
        e.setEpisodeNumber(number);
        e.setStatus(status);
        e.setAccessPolicyOverride(override);
        e.setPrice(new BigDecimal("3000"));
        e.setSortOrder(number);
        e.setTranslations(Translations.all(number + "-qism"));
        return episodeService.saveEpisode(null, content.getId(), null, e);
    }

    private Season season(Content content, int number) {
        SeasonSaveRequest s = new SeasonSaveRequest();
        s.setSeasonNumber(number);
        s.setStatus(PublicationStatus.PUBLISHED);
        s.setSortOrder(number);
        s.setTranslations(Translations.all(number + "-mavsum"));
        return episodeService.saveSeason(null, content.getId(), null, s);
    }

    private void signIn(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private ContentController.EpisodeListResponse list(Content content) {
        return controller.episodes(content.getId(), Locale.UZ).getBody();
    }

    // ------------------------------------------------------------------ tarkib

    @Nested
    @DisplayName("Ro'yxat tarkibi")
    class Contents {

        @Test
        @DisplayName("Nashr qilinmagan qism ro'yxatga kirmaydi")
        void draftEpisodesAreHidden() {
            Content series = series(StructureType.EPISODIC, AccessPolicy.FREE);
            episode(series, null, 1, PublicationStatus.PUBLISHED, null);
            episode(series, null, 2, PublicationStatus.DRAFT, null);

            // Qulf bilan ko'rsatish "tez orada 2-qism chiqadi" degan va'da
            // bo'lardi - buni muharrir aytmagan.
            assertThat(list(series).getEpisodes()).hasSize(1);
            assertThat(list(series).getEpisodes().get(0).getEpisodeNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("Tartib: mavsum, keyin qism raqami")
        void orderedBySeasonThenNumber() {
            Content series = series(StructureType.SEASONAL, AccessPolicy.FREE);
            Season first = season(series, 1);
            Season second = season(series, 2);

            // Ataylab teskari tartibda yaratamiz.
            episode(series, second.getId(), 1, PublicationStatus.PUBLISHED, null);
            episode(series, first.getId(), 2, PublicationStatus.PUBLISHED, null);
            episode(series, first.getId(), 1, PublicationStatus.PUBLISHED, null);

            List<ContentController.EpisodeCard> cards = list(series).getEpisodes();

            assertThat(cards).extracting(
                            c -> c.getSeasonNumber() + "." + c.getEpisodeNumber())
                    .containsExactly("1.1", "1.2", "2.1");
        }

        @Test
        @DisplayName("SEASONAL — mavsumlar nomlari bilan qaytadi")
        void seasonsAreListed() {
            Content series = series(StructureType.SEASONAL, AccessPolicy.FREE);
            Season one = season(series, 1);
            episode(series, one.getId(), 1, PublicationStatus.PUBLISHED, null);

            var response = list(series);
            assertThat(response.getSeasons()).hasSize(1);
            assertThat(response.getSeasons().get(0).getTitle()).isEqualTo("1-mavsum");
            assertThat(response.getEpisodes().get(0).getSeasonId()).isEqualTo(one.getId());
        }

        @Test
        @DisplayName("EPISODIC — mavsumlar bo'sh, soxta «0-mavsum» o'ylab topilmaydi")
        void episodicHasNoSeasons() {
            Content series = series(StructureType.EPISODIC, AccessPolicy.FREE);
            episode(series, null, 1, PublicationStatus.PUBLISHED, null);

            var response = list(series);
            assertThat(response.getSeasons()).isEmpty();
            assertThat(response.getEpisodes().get(0).getSeasonId()).isNull();
            assertThat(response.getStructureType()).isEqualTo("EPISODIC");
        }
    }

    // ------------------------------------------------------------ sizib chiqish

    @Nested
    @DisplayName("Ro'yxat fayl bermaydi")
    class NoLeak {

        @Test
        @DisplayName("Javobda video manzili yo'q — hatto bepul qismda ham")
        void responseHasNoVideoUrls() throws Exception {
            Content series = series(StructureType.EPISODIC, AccessPolicy.FREE);
            episode(series, null, 1, PublicationStatus.PUBLISHED, null);

            String body = mockMvc.perform(
                            get("/api/v1/app/content/" + series.getId() + "/episodes"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Ro'yxat "nima bor" degan savolga javob beradi, "faylni ber"
            // degan savolga emas. Manzil faqat /watch dan chiqadi.
            assertThat(body).doesNotContain("/api/v1/app/media/");
            assertThat(body).doesNotContain("sources");
        }
    }

    // ------------------------------------------------------------------ huquq

    @Nested
    @DisplayName("Kim nimani ko'ra oladi")
    class Access {

        @Test
        @DisplayName("Mehmon: bepul qism ochiq, pullik yopiq va narxi ko'rinadi")
        void guestSeesLocks() {
            Content series = series(StructureType.EPISODIC, AccessPolicy.PREMIUM_OR_PURCHASE);
            episode(series, null, 1, PublicationStatus.PUBLISHED, AccessPolicy.FREE);
            episode(series, null, 2, PublicationStatus.PUBLISHED, null);

            List<ContentController.EpisodeCard> cards = list(series).getEpisodes();

            assertThat(cards.get(0).isAllowed()).isTrue();
            assertThat(cards.get(0).getReason()).isEqualTo("FREE");

            assertThat(cards.get(1).isAllowed()).isFalse();
            assertThat(cards.get(1).getRequiredAction()).isEqualTo("SIGN_IN");
            assertThat(cards.get(1).getEpisodePrice()).isEqualByComparingTo("3000");
        }

        @Test
        @DisplayName("Sotib olingan qism ochiq, qo'shnisi yopiq qoladi")
        void purchasedEpisodeIsOpen() {
            Content series = series(StructureType.EPISODIC, AccessPolicy.PURCHASE_ONLY);
            Episode first = episode(series, null, 1, PublicationStatus.PUBLISHED, null);
            episode(series, null, 2, PublicationStatus.PUBLISHED, null);

            purchaseRepo.save(Purchase.builder()
                    .user(viewer).type(PurchaseType.EPISODE).targetId(first.getId())
                    .amount(new BigDecimal("3000"))
                    .build());
            signIn(viewer);

            List<ContentController.EpisodeCard> cards = list(series).getEpisodes();

            // Aynan shu nuqta ilgari klientda ham buzilgan edi: sotib olingan
            // qism ro'yxatda yopiq ko'rinsa, odam ikkinchi marta to'laydi.
            assertThat(cards.get(0).isAllowed()).isTrue();
            assertThat(cards.get(0).getReason()).isEqualTo("EPISODE_PURCHASE");
            assertThat(cards.get(1).isAllowed()).isFalse();
        }

        @Test
        @DisplayName("Premyera xaridi butun serialni ochadi")
        void premierePurchaseOpensEverything() {
            Content series = series(StructureType.EPISODIC, AccessPolicy.PURCHASE_ONLY);
            episode(series, null, 1, PublicationStatus.PUBLISHED, null);
            episode(series, null, 2, PublicationStatus.PUBLISHED, null);

            purchaseRepo.save(Purchase.builder()
                    .user(viewer).type(PurchaseType.PREMIERE).targetId(series.getId())
                    .amount(new BigDecimal("15000"))
                    .build());
            signIn(viewer);

            assertThat(list(series).getEpisodes())
                    .allMatch(ContentController.EpisodeCard::isAllowed)
                    .allMatch(c -> "PREMIERE_PURCHASE".equals(c.getReason()));
        }

        @Test
        @DisplayName("Premium obuna hammasini ochadi")
        void premiumOpensEverything() {
            Content series = series(StructureType.EPISODIC, AccessPolicy.PREMIUM_ONLY);
            episode(series, null, 1, PublicationStatus.PUBLISHED, null);
            episode(series, null, 2, PublicationStatus.PUBLISHED, null);

            userAdminService.grantPremium(null, viewer.getId(), 1, null);
            signIn(viewer);

            assertThat(list(series).getEpisodes())
                    .allMatch(ContentController.EpisodeCard::isAllowed)
                    .allMatch(c -> "PREMIUM".equals(c.getReason()));
        }

        @Test
        @DisplayName("Ro'yxatdagi qaror /watch dagi qaror bilan bir xil (ТЗ §37)")
        void listAgreesWithWatch() {
            Content series = series(StructureType.EPISODIC, AccessPolicy.PREMIUM_OR_PURCHASE);
            Episode free = episode(series, null, 1, PublicationStatus.PUBLISHED,
                    AccessPolicy.FREE);
            Episode paid = episode(series, null, 2, PublicationStatus.PUBLISHED, null);
            Episode bought = episode(series, null, 3, PublicationStatus.PUBLISHED, null);

            purchaseRepo.save(Purchase.builder()
                    .user(viewer).type(PurchaseType.EPISODE).targetId(bought.getId())
                    .amount(new BigDecimal("3000"))
                    .build());
            signIn(viewer);

            List<ContentController.EpisodeCard> cards = list(series).getEpisodes();

            // Ikki yo'l bitta qoidadan chiqishi kerak. Ajralib ketsa, odam
            // ro'yxatda ochiq ko'rgan qismni bosib "yopiq" degan javob oladi.
            for (Episode e : List.of(free, paid, bought)) {
                AccessDecision single = accessService.canWatch(viewer, e);
                ContentController.EpisodeCard card = cards.stream()
                        .filter(c -> c.getId().equals(e.getId()))
                        .findFirst().orElseThrow();

                assertThat(card.isAllowed()).isEqualTo(single.isAllowed());
                assertThat(card.getReason()).isEqualTo(single.getReason().name());
                assertThat(card.getRequiredAction())
                        .isEqualTo(single.getRequiredAction().name());
            }
        }
    }

    // ------------------------------------------------------------- topilmadi

    @Nested
    @DisplayName("Yo'q kontent")
    class Missing {

        @Test
        @DisplayName("Begona id — 404")
        void unknownContent() {
            assertThatThrownBy(() -> controller.episodes(999_999L, Locale.UZ))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Nashr qilinmagan kontent — 404, mavjudligi oshkor qilinmaydi")
        void draftContentIsInvisible() {
            ContentSaveRequest c = new ContentSaveRequest();
            c.setContentType(ContentType.SERIES);
            c.setStructureType(StructureType.EPISODIC);
            c.setStatus(PublicationStatus.DRAFT);
            c.setAccessPolicy(AccessPolicy.FREE);
            c.setTranslations(Translations.all("Yashirin " + System.nanoTime()));
            Content hidden = contentService.create(null, c);

            // "Bor, lekin yopiq" javobi ham ma'lumot: tayyorlanayotgan
            // serialning mavjudligini bildirardi.
            assertThatThrownBy(() -> controller.episodes(hidden.getId(), Locale.UZ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("topilmadi");
        }
    }

    // ------------------------------------------------------------------ N+1

    @Nested
    @DisplayName("So'rovlar soni")
    class Queries {

        @Test
        @DisplayName("Xaridlar bir marta o'qiladi, har qism uchun qaytadan emas")
        void purchasesAreLoadedOnce() {
            Content series = series(StructureType.EPISODIC, AccessPolicy.PURCHASE_ONLY);
            for (int i = 1; i <= 10; i++) {
                episode(series, null, i, PublicationStatus.PUBLISHED, null);
            }
            signIn(viewer);

            CapturingStatementInspector.clear();
            List<ContentController.EpisodeCard> cards = list(series).getEpisodes();
            List<String> purchaseSelects =
                    CapturingStatementInspector.selectsFrom("cms_purchase");

            assertThat(cards).hasSize(10);
            // Har qism uchun alohida so'rov 10 ta bo'lardi. Ikkitasi: qismlar
            // bo'yicha va butun premyera bo'yicha.
            assertThat(purchaseSelects).hasSizeLessThanOrEqualTo(2);
        }
    }
}
