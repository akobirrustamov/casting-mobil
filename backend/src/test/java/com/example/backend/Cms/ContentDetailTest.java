package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.CreatorSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Dto.ContentDetailDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Entity.GenreTranslation;
import com.example.backend.Cms.Entity.UserBalance;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.CreatorProfession;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.GenreRepo;
import com.example.backend.Cms.Repository.UserBalanceRepo;
import com.example.backend.Cms.Service.ContentDetailService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.DonationService;
import com.example.backend.Cms.Service.TaxonomyService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Enums.UserRoles;
import com.example.backend.support.ContentFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/v1/app/content/{id}} va {@code .../donors} — ilovaning
 * kontent sahifasi uchun kartochka (ТЗ §14, §46).
 *
 * <h2>Nima uchun bu endpoint qo'shildi</h2>
 * Ilovada kontent sahifasi ma'lumotni {@code /watch/**} dan va BOSH SAHIFA
 * KESHIDAN yig'ardi. Keshda esa qator kartochkasi yotadi — afisha va qisqa
 * tavsifdan boshqa hech narsa yo'q. Ya'ni yil, janrlar, to'liq tavsif va
 * aktyorlar bazada bo'lsa ham, ilovaga umuman yetib bormasdi; to'g'ridan
 * havola bilan kirilganda esa kesh bo'sh bo'lib, sahifa afishasiz ochilardi.
 *
 * <h2>Bu yerda nima qo'riqlanadi</h2>
 * <ul>
 *   <li>kartochka video manzilini BERMAYDI — u mehmonga ham ochiq;</li>
 *   <li>nashr qilinmagan kontent «bor, lekin yopiq» emas, YO'Q;</li>
 *   <li>reyting yulduzlar bo'yicha kamayish tartibida, o'rinlar server
 *       tomonda qo'yiladi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContentDetailTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ContentService contentService;
    @Autowired private ContentFixtures contentFixtures;
    @Autowired private ContentDetailService detailService;
    @Autowired private TaxonomyService taxonomyService;
    @Autowired private DonationService donationService;
    @Autowired private GenreRepo genreRepo;
    @Autowired private UserBalanceRepo balanceRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private MockMvc mockMvc;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------- kartochka

    @Nested
    @DisplayName("Kontent kartochkasi")
    class Card {

        @Test
        @DisplayName("Sahifa uchun kerakli hamma narsa bitta javobda keladi")
        void returnsEverythingThePageNeeds() {
            Genre drama = genre("Drama", "Драма");
            Genre melodrama = genre("Melodrama", "Мелодрама");
            Creator actor = creator("Zarina", "Yoqubova");

            Content content = movie(c -> {
                c.setGenreIds(Set.of(drama.getId(), melodrama.getId()));
                c.setAgeRating("16+");
                c.setLanguage("uz");
                c.setDurationMinutes(96);
                c.setPremiereDate(LocalDateTime.of(2024, 3, 1, 0, 0));
                c.setCredits(List.of(credit(actor.getId(), CreatorProfession.ACTRESS,
                        "Dilnoza", 0)));
            });

            ContentDetailDto dto = detailService.build(content, null, Locale.UZ);

            assertThat(dto.getYear())
                    .as("yil premyera sanasidan olinadi, nashr sanasidan emas")
                    .isEqualTo(2024);
            assertThat(dto.getAgeRating()).isEqualTo("16+");
            assertThat(dto.getLanguage()).isEqualTo("uz");
            assertThat(dto.getDurationSeconds()).isEqualTo(96 * 60);
            assertThat(dto.getGenres()).containsExactlyInAnyOrder("Drama", "Melodrama");
            assertThat(dto.getDescription()).isEqualTo("To'liq tavsif");

            assertThat(dto.getCast()).hasSize(1);
            ContentDetailDto.CastMember member = dto.getCast().get(0);
            assertThat(member.getCharacterName())
                    .as("maketda aktyor ismi ostida QAHRAMON ismi turadi")
                    .isEqualTo("Dilnoza");
            assertThat(member.getProfession()).isEqualTo("ACTRESS");
            assertThat(member.getName()).contains("Zarina");
        }

        @Test
        @DisplayName("Sanoqlar nol bo'lsa ham keladi — ilova «server aytmadi» dan ajratadi")
        void countersAreAlwaysPresent() {
            ContentDetailDto dto = detailService.build(movie(c -> {
            }), null, Locale.UZ);

            assertThat(dto.getViewCount()).isZero();
            assertThat(dto.getLikeCount()).isZero();
            assertThat(dto.getCommentCount()).isZero();
            assertThat(dto.getStarsReceived()).isZero();
            assertThat(dto.isLiked())
                    .as("mehmon hech narsa bosmagan")
                    .isFalse();
        }

        @Test
        @DisplayName("Ko'p qismli kontentda davomiylik o'rniga qismlar soni")
        void multiPartHasEpisodeCountInsteadOfDuration() {
            Content series = content(c -> {
                c.setContentType(ContentType.SERIES);
                c.setStructureType(StructureType.EPISODIC);
                c.setDurationMinutes(45);
            });

            ContentDetailDto dto = detailService.build(series, null, Locale.UZ);

            assertThat(dto.getStructureType()).isEqualTo("EPISODIC");
            assertThat(dto.getDurationSeconds())
                    .as("serialning o'z davomiyligi yo'q — bu bitta qismniki bo'lardi")
                    .isNull();
            assertThat(dto.getEpisodeCount()).isNotNull();
        }
    }

    // ------------------------------------------------------------------- HTTP

    @Nested
    @DisplayName("HTTP")
    class Http {

        @Test
        @DisplayName("Mehmon kartochkani ochadi, lekin video havolasi berilmaydi")
        void guestGetsCardWithoutVideoLinks() throws Exception {
            Content content = movie(c -> c.setAccessPolicy(AccessPolicy.PREMIUM_ONLY));

            String body = mockMvc.perform(get("/api/v1/app/content/" + content.getId())
                            .param("locale", "UZ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(content.getId()))
                    .andExpect(jsonPath("$.title").isNotEmpty())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body)
                    .as("kartochka katalog ma'lumoti: fayl manzili faqat /watch dan chiqadi")
                    .doesNotContain("hlsUrl")
                    .doesNotContain("sources")
                    .doesNotContain("/raw");
        }

        @Test
        @DisplayName("Nashr qilinmagan kontent — 404, «yopiq» emas")
        void unpublishedContentIsNotFound() throws Exception {
            Content draft = content(c -> c.setStatus(PublicationStatus.DRAFT));

            mockMvc.perform(get("/api/v1/app/content/" + draft.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Yo'q kontent — 404")
        void missingContentIsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/app/content/999999999"))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------- reyting

    @Nested
    @DisplayName("Donat reytingi")
    class Donors {

        @Test
        @DisplayName("Ko'p yuborgan yuqorida, o'rinlar birdan boshlanadi")
        void ranksByStarsDescending() {
            Content content = movie(c -> {
            });

            User big = userWith(1_000L);
            User small = userWith(1_000L);

            donationService.donate(small, DonationTargetType.CONTENT, content.getId(),
                    CurrencyKind.STARS, 50L);
            donationService.donate(big, DonationTargetType.CONTENT, content.getId(),
                    CurrencyKind.STARS, 700L);
            // Ikkinchi yuborish QO'SHILADI: reyting odam bo'yicha jamlanma,
            // oxirgi tranzaksiya bo'yicha emas.
            donationService.donate(small, DonationTargetType.CONTENT, content.getId(),
                    CurrencyKind.STARS, 20L);

            List<ContentDetailDto.Donor> donors =
                    detailService.topDonors(content.getId(), 10, CurrencyKind.STARS);

            assertThat(donors).hasSize(2);
            assertThat(donors.get(0).getRank()).isEqualTo(1);
            assertThat(donors.get(0).getStars()).isEqualTo(700L);
            assertThat(donors.get(1).getRank()).isEqualTo(2);
            assertThat(donors.get(1).getStars())
                    .as("bitta odamning ikki yuborishi qo'shiladi")
                    .isEqualTo(70L);
        }

        @Test
        @DisplayName("Tanga bilan yuborilgani yulduz reytingiga tushmaydi")
        void coinsDoNotEnterTheStarBoard() {
            Content content = movie(c -> {
            });
            User user = userWith(1_000L);

            donationService.donate(user, DonationTargetType.CONTENT, content.getId(),
                    CurrencyKind.UZCASTING_COIN, 900L);

            assertThat(detailService.topDonors(content.getId(), 10, CurrencyKind.STARS))
                    .as("valyutalar aralashsa, 900 tanga 700 yulduzdan yuqori turardi")
                    .isEmpty();
        }

        /**
         * Ilovada ikkita reyting bor: «Yulduzlar» va «Uzcasting» plitkalari
         * har biri o'z oynasini ochadi (10.09.2026).
         *
         * ⚠️ {@code starsReceived} tanga so'ralganda BO'SH qoladi: eski
         * ilova uni yulduz deb o'qiydi, va unga tanga yig'indisi yozilsa,
         * yulduz belgisi ostida tangalar chiqib qolardi.
         */
        @Test
        @DisplayName("Tanga reytingi alohida so'raladi va o'z yig'indisini qaytaradi")
        void coinBoardIsServedSeparately() throws Exception {
            Content content = movie(c -> {
            });
            User user = userWith(1_000L);

            donationService.donate(user, DonationTargetType.CONTENT, content.getId(),
                    CurrencyKind.UZCASTING_COIN, 900L);

            mockMvc.perform(get("/api/v1/app/content/" + content.getId() + "/donors")
                            .param("currency", "UZCASTING_COIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("UZCASTING_COIN"))
                    .andExpect(jsonPath("$.total").value(900))
                    .andExpect(jsonPath("$.starsReceived").doesNotExist())
                    .andExpect(jsonPath("$.donors.length()").value(1))
                    .andExpect(jsonPath("$.donors[0].stars").value(900));

            // Standart — yulduzlar: eski ilova parametrsiz so'raydi.
            mockMvc.perform(get("/api/v1/app/content/" + content.getId() + "/donors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("STARS"))
                    .andExpect(jsonPath("$.donors").isEmpty());
        }

        @Test
        @DisplayName("Hech kim yubormagan bo'lsa — bo'sh ro'yxat, xato emas")
        void emptyBoardIsNotAnError() throws Exception {
            Content content = movie(c -> {
            });

            mockMvc.perform(get("/api/v1/app/content/" + content.getId() + "/donors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.donors").isArray())
                    .andExpect(jsonPath("$.donors").isEmpty())
                    .andExpect(jsonPath("$.starsReceived").value(0));
        }
    }

    // ------------------------------------------------------------- yordamchi

    private Content movie(java.util.function.Consumer<ContentSaveRequest> tune) {
        return content(c -> {
            c.setContentType(ContentType.MOVIE);
            c.setStructureType(StructureType.SINGLE);
            tune.accept(c);
        });
    }

    private Content content(java.util.function.Consumer<ContentSaveRequest> tune) {
        int n = SEQ.incrementAndGet();

        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setPremierePrice(new BigDecimal("15000"));
        // Uchala til ham majburiy — nashr qilingan kontent uchun
        // `ContentService` shuni talab qiladi.
        c.setTranslations(Map.of(
                Locale.UZ, translation("Qalbing egasi " + n),
                Locale.RU, translation("Фильм " + n),
                Locale.EN, translation("Movie " + n)));

        tune.accept(c);
        return contentFixtures.create(c);
    }

    private TranslationDto translation(String title) {
        TranslationDto t = new TranslationDto();
        t.setTitle(title);
        t.setShortDescription("Qisqa tavsif");
        t.setDescription("To'liq tavsif");
        return t;
    }

    private ContentSaveRequest.CreditLink credit(Long creatorId, CreatorProfession profession,
                                                 String character, int order) {
        ContentSaveRequest.CreditLink link = new ContentSaveRequest.CreditLink();
        link.setCreatorId(creatorId);
        link.setProfession(profession);
        link.setCharacterName(character);
        link.setSortOrder(order);
        return link;
    }

    private Genre genre(String uz, String ru) {
        int n = SEQ.incrementAndGet();
        Genre g = Genre.builder()
                .slug("janr-detail-" + n)
                .sortOrder(n)
                .active(true)
                .build();
        g.addTranslation(GenreTranslation.builder().locale(Locale.UZ).name(uz).build());
        g.addTranslation(GenreTranslation.builder().locale(Locale.RU).name(ru).build());
        g.addTranslation(GenreTranslation.builder().locale(Locale.EN).name(uz).build());
        return genreRepo.save(g);
    }

    private Creator creator(String first, String last) {
        int n = SEQ.incrementAndGet();

        CreatorSaveRequest req = new CreatorSaveRequest();
        req.setActive(true);
        req.setBirthDate(LocalDate.of(1990, 5, 17));
        req.setTranslations(Map.of(
                Locale.UZ, name(first + " " + n, last),
                Locale.RU, name(first + " " + n, last),
                Locale.EN, name(first + " " + n, last)));

        return taxonomyService.saveCreator(null, null, req);
    }

    private CreatorSaveRequest.NameDto name(String first, String last) {
        CreatorSaveRequest.NameDto n = new CreatorSaveRequest.NameDto();
        n.setFirstName(first);
        n.setLastName(last);
        n.setBio("Biografiya");
        return n;
    }

    private User userWith(long stars) {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }

        User u = new User();
        u.setPhone("+99891" + (8100000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Donatchi " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        u = userRepo.save(u);

        balanceRepo.save(UserBalance.builder()
                .user(u)
                .starsBalance(stars)
                .coinBalance(stars)
                .moneyBalance(BigDecimal.ZERO)
                .build());
        return u;
    }
}
