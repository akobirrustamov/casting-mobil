package com.example.backend.Cms;

import com.example.backend.Admin.Dto.AdvertisementSaveRequest;
import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.EpisodeSaveRequest;
import com.example.backend.Admin.Dto.PremiereDto;
import com.example.backend.Admin.Dto.PremiereSaveRequest;
import com.example.backend.Cms.Entity.Advertisement;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.Premiere;
import com.example.backend.Cms.Entity.Purchase;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.AdAudience;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.PurchaseType;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.AdvertisementRepo;
import com.example.backend.Cms.Repository.CurrencyPackageRepo;
import com.example.backend.Cms.Repository.EpisodeRepo;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Repository.PremiereRepo;
import com.example.backend.Cms.Repository.PurchaseRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.EpisodeService;
import com.example.backend.Cms.Service.HomepageService;
import com.example.backend.Cms.Service.MonetizationService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Enums.UserRoles;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.support.Translations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §58 — soft delete.
 *
 * <h2>Nega bu muhim</h2>
 * Biznes yozuvining o'chishi — faqat bitta qator yo'qolishi emas. Unga
 * bog'langan <b>moliyaviy tarix</b> ma'nosini yo'qotadi: xarid yozuvida
 * «EPISODE #42 — 3 000 so'm» qoladi-yu, 42-qism yo'q. Foydalanuvchi pul
 * to'lagan, lekin na u ko'ra oladi, na qo'llab-quvvatlash nima sotilganini
 * aniqlay oladi. Yozuv butun ko'rinadi, aslida esa bo'sh — bu shunchaki
 * o'chirishdan ham yomonroq.
 *
 * <h2>Ikki xil strategiya</h2>
 * <ul>
 *   <li><b>Arxivlash</b> — reklama va premyera: yozuv qoladi, public
 *       feeddan chiqadi (ikkalasi ham {@code isLiveAt} da PUBLISHED
 *       talab qiladi), hisobotda nomi ko'rinaveradi.</li>
 *   <li><b>Taqiqlash</b> — qism va valyuta paketi: sotilgan bo'lsa
 *       umuman o'chirilmaydi, chunki ularda «arxiv» holati xaridorning
 *       huquqini anglatmaydi. Sotuvdan olish uchun boshqa yo'l bor.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SoftDeleteTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private HomepageService homepageService;
    @Autowired private MonetizationService monetizationService;
    @Autowired private EpisodeService episodeService;
    @Autowired private ContentService contentService;
    @Autowired private AdvertisementRepo advertisementRepo;
    @Autowired private PremiereRepo premiereRepo;
    @Autowired private CurrencyPackageRepo packageRepo;
    @Autowired private EpisodeRepo episodeRepo;
    @Autowired private PurchaseRepo purchaseRepo;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    // ------------------------------------------------------------- reklama

    @Nested
    @DisplayName("Reklama — arxivlanadi")
    class Advertisements {

        @Test
        @DisplayName("O'chirish yozuvni bazadan yo'qotmaydi")
        void deleteKeepsRow() {
            Advertisement ad = ad();
            homepageService.deleteAdvertisement(null, ad.getId());

            assertThat(advertisementRepo.findById(ad.getId()))
                    .as("reklama yozuvi bazada qolishi kerak")
                    .isPresent()
                    .get()
                    .extracting(Advertisement::getStatus)
                    .isEqualTo(PublicationStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Arxivlangani admin ro'yxatidan chiqadi")
        void archivedLeavesAdminList() {
            Advertisement ad = ad();
            assertThat(ids(homepageService.advertisements())).contains(ad.getId());

            homepageService.deleteAdvertisement(null, ad.getId());

            assertThat(ids(homepageService.advertisements()))
                    .as("o'chirilgan reklama panelda ko'rinmasligi kerak")
                    .doesNotContain(ad.getId());
        }

        @Test
        @DisplayName("Arxivlangani ommaviy feedga tushmaydi")
        void archivedIsNotLive() {
            Advertisement ad = ad();
            assertThat(ad.isLiveAt(LocalDateTime.now())).isTrue();

            homepageService.deleteAdvertisement(null, ad.getId());

            Advertisement after = advertisementRepo.findById(ad.getId()).orElseThrow();
            assertThat(after.isLiveAt(LocalDateTime.now()))
                    .as("arxivlangan reklama bosh sahifada ko'rsatilmaydi")
                    .isFalse();
        }
    }

    // ------------------------------------------------------------ premyera

    @Nested
    @DisplayName("Premyera — arxivlanadi")
    class Premieres {

        @Test
        @DisplayName("O'chirish yozuvni saqlab qoladi")
        void deleteKeepsRow() {
            Premiere p = premiere();
            homepageService.deletePremiere(null, p.getId());

            assertThat(premiereRepo.findById(p.getId()))
                    .isPresent()
                    .get()
                    .extracting(Premiere::getStatus)
                    .isEqualTo(PublicationStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Arxivlangani ro'yxatdan chiqadi")
        void archivedLeavesList() {
            Premiere p = premiere();
            homepageService.deletePremiere(null, p.getId());

            assertThat(homepageService.premieres().stream().map(Premiere::getId).toList())
                    .doesNotContain(p.getId());
        }
    }

    // ---------------------------------------------------------------- qism

    @Nested
    @DisplayName("Qism — sotilgani o'chirilmaydi")
    class Episodes {

        @Test
        @DisplayName("Xaridi bor qism o'chirilmaydi va joyida qoladi")
        void purchasedEpisodeSurvives() {
            Content c = content();
            Episode ep = episode(c);
            purchase(PurchaseType.EPISODE, ep.getId());

            assertThatThrownBy(() -> episodeService.deleteEpisode(null, c.getId(), ep.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("sotib olingan");

            assertThat(episodeRepo.findById(ep.getId()))
                    .as("taqiq ishlagach qism bazada turishi kerak")
                    .isPresent();
        }

        @Test
        @DisplayName("Xaridi yo'q qism odatdagidek o'chiriladi")
        void unpurchasedEpisodeIsDeletable() {
            Content c = content();
            Episode ep = episode(c);

            episodeService.deleteEpisode(null, c.getId(), ep.getId());

            assertThat(episodeRepo.findById(ep.getId()))
                    .as("hech kim sotib olmagan qismni o'chirish taqiqlanmaydi")
                    .isEmpty();
        }

        @Test
        @DisplayName("Qaytarilgan xarid ham qismni himoya qiladi")
        void refundedPurchaseStillProtects() {
            Content c = content();
            Episode ep = episode(c);
            Purchase p = purchase(PurchaseType.EPISODE, ep.getId());
            p.setRefundedAt(LocalDateTime.now());
            purchaseRepo.save(p);

            assertThatThrownBy(() -> episodeService.deleteEpisode(null, c.getId(), ep.getId()))
                    .as("qaytarilgan to'lov ham «nimaga qaytarildi» savoliga javob talab qiladi")
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Boshqa qismning xaridi to'sib qo'ymaydi")
        void otherEpisodePurchaseDoesNotBlock() {
            Content c = content();
            Episode paid = episode(c);
            Episode free = episode(c);
            purchase(PurchaseType.EPISODE, paid.getId());

            episodeService.deleteEpisode(null, c.getId(), free.getId());

            assertThat(episodeRepo.findById(free.getId())).isEmpty();
        }
    }

    // -------------------------------------------------------------- paket

    @Nested
    @DisplayName("Valyuta paketi — sotilgani o'chirilmaydi")
    class Packages {

        @Test
        @DisplayName("Xaridi bor paket o'chirilmaydi")
        void purchasedPackageSurvives() {
            CurrencyPackage pack = pack();
            purchase(PurchaseType.CURRENCY_PACKAGE, pack.getId());

            assertThatThrownBy(() -> monetizationService.deletePackage(null, pack.getId()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("sotib olingan");

            assertThat(packageRepo.findById(pack.getId())).isPresent();
        }

        @Test
        @DisplayName("Xaridi yo'q paket o'chiriladi")
        void unpurchasedPackageIsDeletable() {
            CurrencyPackage pack = pack();

            monetizationService.deletePackage(null, pack.getId());

            assertThat(packageRepo.findById(pack.getId())).isEmpty();
        }
    }

    // ------------------------------------------------------- manba qoidasi

    @Nested
    @DisplayName("Manba qoidasi")
    class SourceRule {

        /**
         * ТЗ §58 ro'yxatidagi turlar. Bularga {@code repo.delete(...)}
         * chaqirig'i yozilsa — test yiqiladi. Test kelajakdagi kodni
         * qo'riqlaydi: bugungi tuzatish ertaga qaytib kelmasin.
         */
        private static final List<String> PROTECTED = List.of(
                "contentrepo", "creatorrepo", "userrepo", "useraccountrepo",
                "staffprofilerepo", "advertisementrepo", "commentrepo",
                "tariffrepo", "premiererepo", "donationrepo", "purchaserepo",
                "subscriptionrepo");

        @Test
        @DisplayName("Himoyalangan turlarda hard delete chaqirig'i yo'q")
        void noHardDeleteOnProtectedTypes() throws IOException {
            Pattern call = Pattern.compile("(\\w+)Repo\\s*\\.\\s*delete(All)?\\s*\\(");
            List<String> violations = new ArrayList<>();

            for (Path f : sources()) {
                String src = Files.readString(f);
                Matcher m = call.matcher(src);
                while (m.find()) {
                    // Aniq nom bo'yicha: userPermissionRepo «user» bilan boshlansa
                    // ham foydalanuvchi emas, ruxsat qatori — u §58 ro'yxatida yo'q.
                    String repo = (m.group(1) + "Repo").toLowerCase();
                    if (PROTECTED.contains(repo)) {
                        violations.add(f.getFileName() + " → " + m.group());
                    }
                }
            }

            assertThat(violations)
                    .as("§58: bu turlar soft delete/arxiv orqali o'chirilishi kerak")
                    .isEmpty();
        }

        @Test
        @DisplayName("Qoida o'zini tekshira oladi")
        void ruleCanActuallyFail() throws IOException {
            // Yuqoridagi test hech qachon yiqilmasa, u qo'riqchi emas —
            // shunchaki bo'sh ro'yxat. Shuning uchun qidiruv namunasi
            // haqiqiy matnda ishlashini alohida tasdiqlaymiz.
            Pattern call = Pattern.compile("(\\w+)Repo\\s*\\.\\s*delete(All)?\\s*\\(");
            assertThat(call.matcher("        advertisementRepo.delete(ad);").find()).isTrue();
            assertThat(call.matcher("        contentRepo.deleteAll(list);").find()).isTrue();

            // Va manbalar haqiqatan o'qilayotganini — bo'sh papka emasligini.
            assertThat(sources()).hasSizeGreaterThan(50);
        }

        private List<Path> sources() throws IOException {
            Path root = Path.of("src/main/java/com/example/backend");
            try (Stream<Path> s = Files.walk(root)) {
                return s.filter(p -> p.toString().endsWith(".java"))
                        // Dev seeder test ma'lumotini tozalaydi — u prod kodi emas.
                        .filter(p -> !p.toString().contains("/Dev/"))
                        .toList();
            }
        }
    }

    // ----------------------------------------------------------- yordamchi

    private List<Long> ids(List<Advertisement> list) {
        return list.stream().map(Advertisement::getId).toList();
    }

    private Advertisement ad() {
        AdvertisementSaveRequest r = new AdvertisementSaveRequest();
        r.setName("Reklama " + SEQ.incrementAndGet());
        r.setImageMediaId(media().getId());
        r.setAudience(AdAudience.ADVERTISEMENT);
        r.setStatus(PublicationStatus.PUBLISHED);
        r.setTranslations(adTitles());
        return homepageService.saveAdvertisement(null, null, r);
    }

    private Map<Locale, com.example.backend.Admin.Dto.AdvertisementDto.AdTextDto> adTitles() {
        Map<Locale, com.example.backend.Admin.Dto.AdvertisementDto.AdTextDto> m = new LinkedHashMap<>();
        for (Locale l : Locale.values()) {
            m.put(l, com.example.backend.Admin.Dto.AdvertisementDto.AdTextDto.builder()
                    .title("Sarlavha " + l).build());
        }
        return m;
    }

    private Premiere premiere() {
        PremiereSaveRequest r = new PremiereSaveRequest();
        r.setName("Premyera " + SEQ.incrementAndGet());
        r.setStatus(PublicationStatus.PUBLISHED);
        Map<Locale, PremiereDto.PremiereTextDto> tr = new LinkedHashMap<>();
        for (Locale l : Locale.values()) {
            tr.put(l, PremiereDto.PremiereTextDto.builder().title("Tez kunda " + l).build());
        }
        r.setTranslations(tr);
        return homepageService.savePremiere(null, null, r);
    }

    private MediaAsset media() {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/soft-delete-" + SEQ.incrementAndGet())
                .originalFilename("banner.jpg")
                .type(MediaType.IMAGE)
                .mimeType("image/jpeg")
                .sizeBytes(100L)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Content content() {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.SERIES);
        req.setStructureType(StructureType.EPISODIC);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.DRAFT);
        req.setTranslations(Translations.all("Serial " + SEQ.incrementAndGet()));
        return contentService.create(null, req);
    }

    private Episode episode(Content c) {
        EpisodeSaveRequest e = new EpisodeSaveRequest();
        e.setEpisodeNumber(SEQ.incrementAndGet());
        e.setStatus(PublicationStatus.PUBLISHED);
        e.setTranslations(Translations.all("Qism"));
        return episodeService.saveEpisode(null, c.getId(), null, e);
    }

    private CurrencyPackage pack() {
        return packageRepo.save(CurrencyPackage.builder()
                .kind(CurrencyKind.STARS)
                .amount(100L)
                .price(new BigDecimal("10000.00"))
                .active(true)
                .sortOrder(SEQ.incrementAndGet())
                .build());
    }

    private User buyer() {
        Role r = roleRepo.findByName(UserRoles.ROLE_USER);
        if (r == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(Role::getId).max().orElse(0) + 1;
            r = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9400000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Xaridor " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(r)));
        return userRepo.save(u);
    }

    private Purchase purchase(PurchaseType type, Long targetId) {
        return purchaseRepo.save(Purchase.builder()
                .user(buyer())
                .type(type)
                .targetId(targetId)
                .amount(new BigDecimal("3000.00"))
                .currency("UZS")
                .createdAt(LocalDateTime.now())
                .build());
    }
}
