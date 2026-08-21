package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Repository.SubscriptionRepo;
import com.example.backend.Cms.Service.AccessDecision;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Premium sovg'a qilish va tortib olish — entitlement bilan BIRGA.
 *
 * <h2>Nega birga</h2>
 * "Premium berildi" degan yozuv o'z-o'zicha hech narsani anglatmaydi.
 * Muhimi — foydalanuvchi shundan keyin pullik kontentni KO'RA OLADIMI va
 * tortib olingach KO'RA OLMAY QOLADIMI. Shuning uchun har bir amaldan keyin
 * {@link AccessService} javobi tekshiriladi.
 *
 * <h2>ТЗ §38</h2>
 * Sovg'a mavjud obuna USTIGA qo'shiladi, boshidan boshlanmaydi. Aks holda
 * pul to'lagan foydalanuvchi bir oylik sovg'adan keyin qolgan muddatini
 * yo'qotardi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PremiumLifecycleTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private UserAdminService userAdminService;
    @Autowired private AccessService accessService;
    @Autowired private ContentService contentService;
    @Autowired private EpisodeService episodeService;
    @Autowired private SubscriptionRepo subscriptionRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    private User newUser() {
        int n = SEQ.incrementAndGet();
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        return userRepo.save(User.builder()
                .phone(String.format("+99891%07d", 1000000 + n))
                .name("Premium sinovi " + n)
                .password(passwordEncoder.encode("12345678"))
                .roles(List.of(role))
                .build());
    }

    /** Faqat Premium ochadigan qism. */
    private Episode premiumEpisode() {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.SERIES);
        c.setStructureType(StructureType.EPISODIC);
        c.setAccessPolicy(AccessPolicy.PREMIUM_ONLY);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setTranslations(Translations.all("Premium serial " + SEQ.incrementAndGet()));
        Content content = contentService.create(null, c);

        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setEpisodeNumber(1);
        e.setStatus(PublicationStatus.PUBLISHED);
        e.setPrice(new BigDecimal("5000"));
        e.setTranslations(Translations.all("1-qism"));
        return episodeService.saveEpisode(null, content.getId(), null, e);
    }

    // ------------------------------------------------------------- sovg'a

    @Nested
    @DisplayName("Premium berish")
    class Grant {

        @Test
        @DisplayName("Berilgach foydalanuvchi pullik qismni ko'ra oladi")
        void grantOpensPremiumContent() {
            User user = newUser();
            Episode episode = premiumEpisode();

            AccessDecision before = accessService.canWatch(user, episode);
            assertThat(before.isAllowed()).isFalse();
            assertThat(before.getRequiredAction())
                    .isEqualTo(AccessDecision.RequiredAction.SUBSCRIBE);

            userAdminService.grantPremium(null, user.getId(), 1, null);

            AccessDecision after = accessService.canWatch(user, episode);
            assertThat(after.isAllowed()).isTrue();
            assertThat(after.getReason()).isEqualTo(AccessDecision.Reason.PREMIUM);
        }

        @Test
        @DisplayName("ТЗ §38: sovg'a mavjud muddat USTIGA qo'shiladi")
        void giftExtendsExistingSubscription() {
            User user = newUser();

            UserAccount first = userAdminService.grantPremium(null, user.getId(), 2, null);
            LocalDateTime afterFirst = first.getPremiumUntil();

            UserAccount second = userAdminService.grantPremium(null, user.getId(), 3, null);
            LocalDateTime afterSecond = second.getPremiumUntil();

            // 2 oy + 3 oy = 5 oy. Agar boshidan boshlansa, 3 oy bo'lib qolardi
            // va foydalanuvchi to'lagan 2 oyini yo'qotardi.
            assertThat(afterSecond).isEqualTo(afterFirst.plusMonths(3));
        }

        @Test
        @DisplayName("Sovg'a ADMIN_GIFT manbasi bilan yoziladi")
        void giftIsRecordedAsAdminGift() {
            User user = newUser();
            userAdminService.grantPremium(null, user.getId(), 1, null);

            List<Subscription> subs =
                    subscriptionRepo.findAllByUserIdOrderByEndAtDesc(user.getId());

            // Sovg'ani sotib olingan obunadan ajratib bo'lishi kerak -
            // aks holda daromad hisoboti sovg'alarni ham pul deb sanardi.
            assertThat(subs).isNotEmpty();
            assertThat(subs.get(0).getSource()).isEqualTo(SubscriptionSource.ADMIN_GIFT);
            assertThat(subs.get(0).getPaidAmount()).isNull();
        }

        @Test
        @DisplayName("Noto'g'ri muddat rad etiladi")
        void invalidDurationIsRejected() {
            User user = newUser();
            for (Integer months : new Integer[]{null, 0, -1}) {
                assertThatThrownBy(() ->
                        userAdminService.grantPremium(null, user.getId(), months, null))
                        .isInstanceOf(BusinessException.class);
            }
        }

        @Test
        @DisplayName("Mavjud bo'lmagan foydalanuvchiga berib bo'lmaydi")
        void unknownUserIsRejected() {
            assertThatThrownBy(() ->
                    userAdminService.grantPremium(null, UUID.randomUUID(), 1, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // --------------------------------------------------------- tortib olish

    @Nested
    @DisplayName("Premium tortib olish")
    class Revoke {

        @Test
        @DisplayName("Tortib olingach kontent DARHOL yopiladi")
        void revokeClosesContentImmediately() {
            User user = newUser();
            Episode episode = premiumEpisode();

            userAdminService.grantPremium(null, user.getId(), 6, null);
            assertThat(accessService.canWatch(user, episode).isAllowed()).isTrue();

            userAdminService.revokePremium(null, user.getId());

            AccessDecision after = accessService.canWatch(user, episode);
            assertThat(after.isAllowed()).isFalse();
            assertThat(after.getReason()).isEqualTo(AccessDecision.Reason.PAYMENT_REQUIRED);
        }

        @Test
        @DisplayName("Obuna yozuvi O'CHIRILMAYDI - bekor qilingan deb belgilanadi")
        void subscriptionIsMarkedNotDeleted() {
            User user = newUser();
            userAdminService.grantPremium(null, user.getId(), 3, null);
            userAdminService.revokePremium(null, user.getId());

            List<Subscription> subs =
                    subscriptionRepo.findAllByUserIdOrderByEndAtDesc(user.getId());

            // Tarix saqlanishi kerak: kim, qachon, nima uchun bergani va
            // tortib olgani keyinchalik tekshirilishi mumkin bo'lsin.
            assertThat(subs).isNotEmpty();
            assertThat(subs.get(0).getRevokedAt()).isNotNull();
        }

        @Test
        @DisplayName("Premiumsiz foydalanuvchidan tortib olish xato bermaydi")
        void revokingWithoutPremiumIsSafe() {
            User user = newUser();
            UserAccount account = userAdminService.revokePremium(null, user.getId());
            assertThat(account.getPremiumUntil()).isNull();
        }
    }
}
