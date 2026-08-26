package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.DonationTransaction;
import com.example.backend.Cms.Entity.UserBalance;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Repository.CreatorRepo;
import com.example.backend.Cms.Repository.DonationRepo;
import com.example.backend.Cms.Repository.UserBalanceRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.DonationService;
import com.example.backend.Cms.Service.MonetizationService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §39 — donat yuborish.
 *
 * <h2>Nima yetishmasdi</h2>
 * Ma'lumot modeli va admin hisoboti bor edi, lekin foydalanuvchi donat
 * YUBORADIGAN yo'l yo'q edi: {@code DonationTransaction} satrlari faqat
 * dev ma'lumotlari bilan to'lardi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DonationFlowTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private DonationService donationService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private ContentService contentService;
    @Autowired private MonetizationService monetizationService;
    @Autowired private DonationRepo donationRepo;
    @Autowired private com.example.backend.Cms.Service.DonationTargetNames targetNames;
    @Autowired private UserBalanceRepo balanceRepo;
    @Autowired private CreatorRepo creatorRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    // ------------------------------------------------------------ yuborish

    @Nested
    @DisplayName("Donat yuborish")
    class Sending {

        @Test
        @DisplayName("Ijodkorni qo'llab-quvvatlash balansdan yechadi")
        void donatingToCreatorDeductsBalance() {
            User sender = userWith(100L, 0L);
            Creator creator = creator();

            DonationTransaction d = donationService.donate(sender,
                    DonationTargetType.CREATOR, creator.getId(), CurrencyKind.STARS, 30L);

            assertThat(d.getId()).isNotNull();
            assertThat(d.getAmount()).isEqualTo(30L);
            assertThat(balanceOf(sender).getStarsBalance()).isEqualTo(70L);
        }

        @Test
        @DisplayName("Kontentni qo'llab-quvvatlash ham mumkin")
        void donatingToContent() {
            User sender = userWith(0L, 50L);
            Content film = publishedContent();

            DonationTransaction d = donationService.donate(sender,
                    DonationTargetType.CONTENT, film.getId(), CurrencyKind.UZCASTING_COIN, 20L);

            assertThat(d.getTargetType()).isEqualTo(DonationTargetType.CONTENT);
            assertThat(balanceOf(sender).getCoinBalance()).isEqualTo(30L);
        }

        @Test
        @DisplayName("⚠️ Ikki valyuta ARALASHMAYDI")
        void currenciesAreIndependent() {
            User sender = userWith(100L, 100L);
            Creator creator = creator();

            donationService.donate(sender, DonationTargetType.CREATOR,
                    creator.getId(), CurrencyKind.STARS, 40L);

            UserBalance balance = balanceOf(sender);
            assertThat(balance.getStarsBalance()).isEqualTo(60L);
            // Kurslari alohida belgilanadi (§40, §41) — ular bir-biriga
            // almashtirilmaydi.
            assertThat(balance.getCoinBalance()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Balans yetmasa rad etiladi")
        void insufficientBalanceIsRejected() {
            User sender = userWith(10L, 0L);
            Creator creator = creator();

            assertThatThrownBy(() -> donationService.donate(sender,
                    DonationTargetType.CREATOR, creator.getId(), CurrencyKind.STARS, 50L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("yetarli emas");

            assertThat(balanceOf(sender).getStarsBalance())
                    .as("Rad etilganda balans tegilmaydi")
                    .isEqualTo(10L);
        }

        @Test
        @DisplayName("⚠️ Manfiy miqdor balansni OSHIRMAYDI")
        void negativeAmountIsRejected() {
            User sender = userWith(100L, 0L);
            Creator creator = creator();

            // Tekshirilmasa, manfiy miqdor ayirish o'rniga qo'shardi —
            // ya'ni pul yaratish usuli bo'lardi.
            assertThatThrownBy(() -> donationService.donate(sender,
                    DonationTargetType.CREATOR, creator.getId(), CurrencyKind.STARS, -50L))
                    .isInstanceOf(BusinessException.class);

            assertThat(balanceOf(sender).getStarsBalance()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Nol miqdor ham rad etiladi")
        void zeroAmountIsRejected() {
            User sender = userWith(100L, 0L);
            Creator creator = creator();

            assertThatThrownBy(() -> donationService.donate(sender,
                    DonationTargetType.CREATOR, creator.getId(), CurrencyKind.STARS, 0L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Bloklangan foydalanuvchi donat yubora olmaydi")
        void blockedUserCannotDonate() {
            User sender = userWith(100L, 0L);
            userAdminService.setBlocked(null, sender.getId(), true, "sinov");
            Creator creator = creator();

            assertThatThrownBy(() -> donationService.donate(sender,
                    DonationTargetType.CREATOR, creator.getId(), CurrencyKind.STARS, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("bloklangan");
        }

        @Test
        @DisplayName("Anonim yubora olmaydi")
        void anonymousCannotDonate() {
            assertThatThrownBy(() -> donationService.donate(null,
                    DonationTargetType.CREATOR, 1L, CurrencyKind.STARS, 10L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // -------------------------------------------------------------- nishon

    @Nested
    @DisplayName("Nishon tekshiruvi")
    class Target {

        @Test
        @DisplayName("⚠️ Mavjud bo'lmagan ijodkorga donat rad etiladi")
        void unknownCreatorIsRejected() {
            User sender = userWith(100L, 0L);

            // Tekshirilmasa, balansdan yechilib hisobotda hech qayerda
            // ko'rinmasdi — pul yo'qolardi.
            assertThatThrownBy(() -> donationService.donate(sender,
                    DonationTargetType.CREATOR, 999_999L, CurrencyKind.STARS, 10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("topilmadi");

            assertThat(balanceOf(sender).getStarsBalance()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Nashr qilinmagan kontentga donat rad etiladi")
        void draftContentIsRejected() {
            User sender = userWith(100L, 0L);
            Content draft = content(PublicationStatus.DRAFT);

            // Foydalanuvchi uni ko'rmagan ham.
            assertThatThrownBy(() -> donationService.donate(sender,
                    DonationTargetType.CONTENT, draft.getId(), CurrencyKind.STARS, 10L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Nofaol ijodkorga donat rad etiladi")
        void inactiveCreatorIsRejected() {
            User sender = userWith(100L, 0L);
            Creator creator = creator();
            creator.setActive(false);
            creatorRepo.save(creator);

            assertThatThrownBy(() -> donationService.donate(sender,
                    DonationTargetType.CREATOR, creator.getId(), CurrencyKind.STARS, 10L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ------------------------------------------------------------- hisobot

    @Nested
    @DisplayName("Alohida hisoblash (ТЗ §39)")
    class PerTargetAccounting {

        @Test
        @DisplayName("Har bir ijodkor va kontent alohida hisoblanadi")
        void eachTargetIsCountedSeparately() {
            User sender = userWith(500L, 0L);
            Creator a = creator();
            Creator b = creator();
            Content film = publishedContent();

            donationService.donate(sender, DonationTargetType.CREATOR,
                    a.getId(), CurrencyKind.STARS, 100L);
            donationService.donate(sender, DonationTargetType.CREATOR,
                    b.getId(), CurrencyKind.STARS, 50L);
            donationService.donate(sender, DonationTargetType.CONTENT,
                    film.getId(), CurrencyKind.STARS, 30L);

            var report = monetizationService.donationReport(20, 30);

            assertThat(report.getTopCreators())
                    .filteredOn(r -> r.getTargetId().equals(a.getId()))
                    .first().extracting(r -> r.getTotal()).isEqualTo(100L);
            assertThat(report.getTopCreators())
                    .filteredOn(r -> r.getTargetId().equals(b.getId()))
                    .first().extracting(r -> r.getTotal()).isEqualTo(50L);
            assertThat(report.getTopContent())
                    .filteredOn(r -> r.getTargetId().equals(film.getId()))
                    .first().extracting(r -> r.getTotal()).isEqualTo(30L);
        }

        @Test
        @DisplayName("Yozuv O'ZGARMAS — tahrirlash endpointi yo'q")
        void transactionIsImmutable() {
            User sender = userWith(100L, 0L);
            Creator creator = creator();
            DonationTransaction d = donationService.donate(sender,
                    DonationTargetType.CREATOR, creator.getId(), CurrencyKind.STARS, 10L);

            // Moliyaviy tarix: DonationService da faqat `donate` bor,
            // `update` yoki `delete` YO'Q (§42).
            assertThat(donationRepo.findById(d.getId())).isPresent();
            assertThat(DonationService.class.getDeclaredMethods())
                    .extracting(java.lang.reflect.Method::getName)
                    .doesNotContain("update", "delete", "refund");
        }
    }

    // ------------------------------------------------------------ yordamchi

    private UserBalance balanceOf(User u) {
        return balanceRepo.findByUserId(u.getId()).orElseThrow();
    }

    private User userWith(long stars, long coins) {
        User u = appUser();
        balanceRepo.save(UserBalance.builder()
                .user(u)
                .starsBalance(stars)
                .coinBalance(coins)
                .moneyBalance(java.math.BigDecimal.ZERO)
                .build());
        return u;
    }

    private User appUser() {
        Role r = roleRepo.findByName(UserRoles.ROLE_USER);
        if (r == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            r = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9700000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Donator " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(r)));
        return userRepo.save(u);
    }

    private Creator creator() {
        int n = SEQ.incrementAndGet();
        Creator c = Creator.builder()
                .slug("ijodkor-" + n)
                .active(true)
                .featured(false)
                .sortOrder(0)
                .starsReceived(0L)
                .build();

        // ⚠️ Tarjimalar SHART. Faol ijodkor uch tilsiz saqlanmaydi
        // (`TranslationRules`), ya'ni tarjimasiz ijodkor ishlab
        // chiqarishda uchramaydi. Tarjimasiz fikstura hisobotdagi
        // nomni sinab bo'lmaydigan qilib qo'yardi.
        for (com.example.backend.Cms.Enums.Locale loc
                : com.example.backend.Cms.Enums.Locale.values()) {
            c.addTranslation(com.example.backend.Cms.Entity.CreatorTranslation.builder()
                    .locale(loc)
                    .displayName("Ijodkor " + loc + " " + n)
                    .build());
        }
        return creatorRepo.save(c);
    }

    private Content publishedContent() {
        return content(PublicationStatus.PUBLISHED);
    }

    private Content content(PublicationStatus status) {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(status);
        req.setVisibility(ContentVisibility.PUBLIC);
        req.setTranslations(Translations.all("Donat filmi " + SEQ.incrementAndGet()));
        return contentService.create(null, req);
    }

    // ---------------------------------------------- nishon nomi (ТЗ §42)

    @Nested
    @DisplayName("Donat kimga berilgani ko'rinadi")
    class TargetNames {

        /**
         * ⚠️ Ilgari panelda faqat «CREATOR #5» ko'rinardi.
         *
         * Asimmetriya buni e'tibordan chetda qolgan deb ko'rsatadi:
         * YUBORUVCHINING ismi qaytarilardi ({@code senderName}),
         * oluvchiniki esa yo'q. Admin donat kimga berilganini bila
         * olmasdi.
         */
        @Test
        @DisplayName("Hisobotda ijodkor ismi qaytadi")
        void reportCarriesCreatorName() {
            User sender = userWith(100L, 0L);
            Creator creator = creator();
            donationService.donate(sender, DonationTargetType.CREATOR,
                    creator.getId(), CurrencyKind.STARS, 10L);

            var report = monetizationService.donationReport(10, 30);

            assertThat(report.getTopCreators())
                    .as("nom bo'lmasa panel `#5` ko'rsatardi")
                    .isNotEmpty()
                    .allSatisfy(row -> assertThat(row.getTargetName()).isNotBlank());
        }

        @Test
        @DisplayName("Hisobotda kontent sarlavhasi qaytadi")
        void reportCarriesContentTitle() {
            User sender = userWith(0L, 100L);
            Content film = publishedContent();
            donationService.donate(sender, DonationTargetType.CONTENT,
                    film.getId(), CurrencyKind.UZCASTING_COIN, 10L);

            assertThat(monetizationService.donationReport(10, 30).getTopContent())
                    .isNotEmpty()
                    .allSatisfy(row -> assertThat(row.getTargetName()).isNotBlank());
        }

        /**
         * ⚠️ Nom topilmasa `null` qaytadi, to'qib chiqarilmaydi.
         *
         * O'chirilgan ijodkorga berilgan eski donat shu holatda
         * bo'ladi. Panel o'shanda `#5` ko'rsatadi — bu halol.
         */
        @Test
        @DisplayName("Topilmagan nishonda nom `null`")
        void unknownTargetHasNullName() {
            var names = targetNames.resolve(java.util.List.of(
                    new com.example.backend.Cms.Service.DonationTargetNames.Ref(
                            DonationTargetType.CREATOR, 999_999L)));

            assertThat(names).isEmpty();
        }

        /**
         * Nomlar TO'PLAM bo'lib yuklanadi.
         *
         * Bittalab yuklansa sahifadagi har bir qator uchun alohida
         * so'rov ketardi (§66).
         */
        @Test
        @DisplayName("Bir nechta nishon bitta chaqiruvda yechiladi")
        void namesResolveInBatch() {
            Creator a = creator();
            Creator b = creator();
            Content film = publishedContent();

            var names = targetNames.resolve(java.util.List.of(
                    new com.example.backend.Cms.Service.DonationTargetNames.Ref(
                            DonationTargetType.CREATOR, a.getId()),
                    new com.example.backend.Cms.Service.DonationTargetNames.Ref(
                            DonationTargetType.CREATOR, b.getId()),
                    new com.example.backend.Cms.Service.DonationTargetNames.Ref(
                            DonationTargetType.CONTENT, film.getId())));

            assertThat(names).hasSize(3);
        }
    }
}
