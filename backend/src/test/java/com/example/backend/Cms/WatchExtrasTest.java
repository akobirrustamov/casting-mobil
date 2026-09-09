package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Comment;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.ContentCredit;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.CreatorTranslation;
import com.example.backend.Cms.Entity.DonationTransaction;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.CreatorProfession;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.CommentRepo;
import com.example.backend.Cms.Repository.ContentCreditRepo;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.CreatorRepo;
import com.example.backend.Cms.Repository.DonationRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.support.Translations;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kontent ekrani uchun qo'shimcha ma'lumot: qatnashganlar, yulduzlar, izohlar.
 *
 * <h2>Nima uchun bu kerak bo'ldi</h2>
 * Buyurtmachi (09.09.2026) referens ekranni yubordi: afisha, tugma, keyin
 * to'rtta ko'rsatkich va «Aktyorlar» qatori. Aktyorlar admin panelda
 * ALLAQACHON to'ldirilardi — lekin ilovaga hech qachon chiqmagan: ma'lumot
 * bazada yotardi va uni hech kim ko'rmasdi.
 *
 * <h2>Bu yerda nima jimgina buziladi</h2>
 * Izohlar soni. Moderator yashirgan izoh sanoqqa tushib qolsa, odam
 * «16 ta izoh» ni ko'rib, ochganda o'n to'rttasini topadi — va bu xato
 * ekranda umuman ko'rinmaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WatchExtrasTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private ContentService contentService;
    @Autowired private ContentRepo contentRepo;
    @Autowired private ContentCreditRepo creditRepo;
    @Autowired private CreatorRepo creatorRepo;
    @Autowired private CommentRepo commentRepo;
    @Autowired private DonationRepo donationRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private EntityManager entityManager;

    // ------------------------------------------------------------- yordamchi

    private Content movie() {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setDurationMinutes(90);
        c.setPremierePrice(new BigDecimal("5000"));
        c.setTranslations(Translations.all("Film " + SEQ.incrementAndGet()));
        return contentService.create(null, c);
    }

    private Creator creator(String name) {
        Creator person = creatorRepo.save(Creator.builder()
                .slug("ijodkor-" + SEQ.incrementAndGet())
                .active(true)
                .build());

        person.getTranslations().add(CreatorTranslation.builder()
                .creator(person)
                .locale(Locale.UZ)
                .displayName(name)
                .build());
        return creatorRepo.save(person);
    }

    private void cast(Content content, Creator person, CreatorProfession role,
                      String character, int order) {
        creditRepo.save(ContentCredit.builder()
                .content(content)
                .creator(person)
                .profession(role)
                .characterName(character)
                .sortOrder(order)
                .build());
    }

    private void comment(Content content, CommentStatus status) {
        User author = userRepo.save(User.builder()
                .phone("+99890" + (2_000_000 + SEQ.incrementAndGet()))
                .name("Izoh muallifi")
                .roles(List.of())
                .build());

        commentRepo.save(Comment.builder()
                .author(author)
                .content(content)
                .text("Yaxshi kino")
                .status(status)
                .build());
    }

    @Nested
    @DisplayName("Qatnashganlar")
    class Credits {

        @Test
        @DisplayName("Ro'yxat javobda bor, tartib admin bergani bo'yicha")
        void creditsAreReturnedInAdminOrder() throws Exception {
            Content film = movie();
            cast(film, creator("Ikkinchi"), CreatorProfession.ACTOR, "Jamshid", 2);
            cast(film, creator("Birinchi"), CreatorProfession.ACTRESS, "Sevinch", 1);
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credits.length()").value(2))
                    // ⚠️ Tartib — bu admin qarori, alifbo emas.
                    .andExpect(jsonPath("$.credits[0].name").value("Birinchi"))
                    .andExpect(jsonPath("$.credits[0].characterName").value("Sevinch"))
                    .andExpect(jsonPath("$.credits[0].profession").value("ACTRESS"))
                    .andExpect(jsonPath("$.credits[1].name").value("Ikkinchi"));
        }

        @Test
        @DisplayName("Hech kim qo'shilmagan bo'lsa — bo'sh ro'yxat, null emas")
        void emptyListInsteadOfNull() throws Exception {
            Content film = movie();
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credits").isArray())
                    .andExpect(jsonPath("$.credits.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Izohlar soni")
    class Comments {

        @Test
        @DisplayName("Yashirilgan izoh sanoqqa tushmaydi")
        void hiddenCommentsAreNotCounted() throws Exception {
            Content film = movie();
            comment(film, CommentStatus.VISIBLE);
            comment(film, CommentStatus.VISIBLE);
            comment(film, CommentStatus.HIDDEN);
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.commentCount").value(2));
        }

        @Test
        @DisplayName("Boshqa kontentning izohi bu yerga qo'shilmaydi")
        void commentsDoNotLeakBetweenContents() throws Exception {
            Content film = movie();
            Content other = movie();
            comment(other, CommentStatus.VISIBLE);
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.commentCount").value(0));
        }
    }

    @Nested
    @DisplayName("UZCASTING Coin")
    class Coins {

        private void donate(Content content, CurrencyKind kind, long amount) {
            User sender = userRepo.save(User.builder()
                    .phone("+99890" + (3_000_000 + SEQ.incrementAndGet()))
                    .name("Donat qiluvchi")
                    .roles(List.of())
                    .build());

            donationRepo.save(DonationTransaction.builder()
                    .sender(sender)
                    .targetType(DonationTargetType.CONTENT)
                    .targetId(content.getId())
                    .kind(kind)
                    .amount(amount)
                    .build());
        }

        @Test
        @DisplayName("Tangalar qo'shilib javobga tushadi")
        void coinsAreSummed() throws Exception {
            Content film = movie();
            donate(film, CurrencyKind.UZCASTING_COIN, 20);
            donate(film, CurrencyKind.UZCASTING_COIN, 36);
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coinsReceived").value(56));
        }

        /**
         * ⚠️ Bu faylning eng muhim tekshiruvi.
         *
         * Yulduz va tanga — boshqa-boshqa birliklar. Ular bitta songa
         * qo'shilib ketsa, ekranda ma'nosiz raqam turadi, va buni
         * payqash deyarli imkonsiz: son o'sadi, ya'ni «ishlayotgandek»
         * ko'rinadi.
         */
        @Test
        @DisplayName("Yulduzlar tangalarga qo'shilmaydi")
        void starsDoNotLeakIntoCoins() throws Exception {
            Content film = movie();
            donate(film, CurrencyKind.UZCASTING_COIN, 10);
            donate(film, CurrencyKind.STARS, 500);
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coinsReceived").value(10));
        }

        @Test
        @DisplayName("Hech kim donat qilmagan bo'lsa — nol, null emas")
        void zeroWhenNobodyDonated() throws Exception {
            Content film = movie();
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coinsReceived").value(0));
        }
    }

    @Nested
    @DisplayName("Yulduzlar")
    class Stars {

        @Test
        @DisplayName("Kontentga tushgan yulduzlar javobda")
        void starsAreReturned() throws Exception {
            Content film = movie();
            film.setStarsReceived(586L);
            contentRepo.save(film);
            entityManager.flush();

            mockMvc.perform(get("/api/v1/app/watch/content/" + film.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.starsReceived").value(586));
        }
    }
}
