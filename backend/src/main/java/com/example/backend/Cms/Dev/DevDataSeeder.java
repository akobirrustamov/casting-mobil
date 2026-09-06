package com.example.backend.Cms.Dev;

import com.example.backend.Cms.Entity.*;
import com.example.backend.Cms.Service.HomepageService;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.*;
import com.example.backend.Cms.Enums.CommentStatus;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Enums.AnalyticsEventType;
import com.example.backend.Cms.Enums.AdAudience;
import com.example.backend.Cms.Enums.LinkType;
import com.example.backend.Cms.Enums.InternalTargetType;
import com.example.backend.Cms.Service.AnalyticsService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Entity.UserPermission;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserPermissionRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Sms.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Dev/test uchun mock ma'lumot.
 *
 * <b>Faqat {@code app.dev.seed=true} bo'lganda ishlaydi</b> - bu xossa faqat
 * dev profilida bor, shuning uchun serverda tasodifan ham ishga tushmaydi.
 *
 * Idempotent: kontent allaqachon bo'lsa, hech narsa qilmaydi.
 *
 * Har bir matn UCH tilda yaratiladi (UZ/RU/EN) - ko'p tillilikni brauzerda
 * tekshirish uchun. Ba'zi kontentlarda rus tili uchun ALOHIDA afisha bor -
 * tilga bog'liq media mexanizmini sinash uchun.
 *
 * AutoRun'dan keyin ishlaydi (@Order), chunki rollar avval yaratilishi kerak.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dev.seed", havingValue = "true")
public class DevDataSeeder implements CommandLineRunner {

    /** Barcha dev hisoblari uchun bitta parol - eslab qolish oson bo'lsin. */
    public static final String DEV_PASSWORD = "12345678";

    /**
     * Qo'lda sinash uchun QO'SHIMCHA ilova foydalanuvchisi.
     *
     * Yuqoridagi beshta hisob stsenariy uchun (premium, muddati o'tgan,
     * xarid qilgan, bloklangan, bepul). Bu esa oltinchisi - egasi o'zi
     * kiradigan, o'z raqami bilan.
     *
     * <h2>Nega sozlamadan, kodda emas</h2>
     * Raqam ham, parol ham HAQIQIY. Kodga yozilsa jar ichiga tushib
     * serverga ketardi - lokal sozlama fayli esa yuklashdan oldin
     * tozalanadi. Bo'sh qoldirilsa hisob umuman yaratilmaydi.
     */
    @Value("${app.dev.test-user.phone:}")
    private String testUserPhone;

    @Value("${app.dev.test-user.password:}")
    private String testUserPassword;

    @Value("${app.dev.test-user.name:Sinov Foydalanuvchi}")
    private String testUserName;

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final UserPermissionRepo userPermissionRepo;
    private final PasswordEncoder passwordEncoder;

    private final CategoryRepo categoryRepo;
    private final GenreRepo genreRepo;
    private final CreatorRepo creatorRepo;
    private final ContentRepo contentRepo;
    private final SeasonRepo seasonRepo;
    private final EpisodeRepo episodeRepo;
    private final CommentRepo commentRepo;
    private final DonationRepo donationRepo;
    private final AnalyticsEventRepo analyticsEventRepo;
    private final AdvertisementRepo advertisementRepo;
    private final PremiereRepo premiereRepo;
    private final AnalyticsService analyticsService;
    private final DevMediaFactory media;
    private final UserAccountRepo userAccountRepo;
    private final UserBalanceRepo userBalanceRepo;
    private final SubscriptionRepo subscriptionRepo;
    private final PurchaseRepo purchaseRepo;
    private final TariffRepo tariffRepo;

    /**
     * ⚠️ Bosh sahifa bo'limlarini yaratish uchun.
     *
     * `HomeFeedService` bo'limlarni `homepage_section` jadvalidan o'qiydi,
     * jadval esa migratsiyada TO'LDIRILMAYDI — satrlar birinchi marta
     * admin paneli ochilganda paydo bo'ladi ({@code HomepageService.sections()}).
     *
     * Lokal stendda admin paneliga hech kim kirmaydi, natijada
     * {@code /api/v1/app/home} kontent bor bo'lsa ham BO'SH qaytardi va
     * ilova ishlamayotgandek ko'rinardi. Mantiq takrorlanmaydi — ayni
     * o'sha metod chaqiriladi.
     */
    private final HomepageService homepageService;

    @Override
    @Transactional
    public void run(String... args) {
        seedStaff();

        if (contentRepo.count() > 0) {
            log.info("DevDataSeeder: kontent allaqachon bor, o'tkazib yuborildi");
        } else {
            List<Category> categories = seedCategories();
            List<Genre> genres = seedGenres();
            List<Creator> creators = seedCreators();
            seedContent(categories, genres, creators);
            seedComments();
            seedDonations(creators);
            seedBanners();
            seedAnalytics();
        }

        // Kontent blokidan TASHQARIDA: bu foydalanuvchilar keyinroq qo'shilgan
        // va mavjud dev bazalarda ham paydo bo'lishi kerak. Metod idempotent.
        // Xarid qism narxiga bog'lanadi, shuning uchun kontentdan KEYIN.
        seedAppUsers();

        // ⚠️ Kontent blokidan TASHQARIDA va idempotent: metod faqat
        // YETISHMAYOTGAN bo'lim turlarini qo'shadi. Usiz bosh sahifa
        // bo'sh bo'lardi - kontent bazada bo'lsa ham.
        int sectionCount = homepageService.sections().size();

        log.info("DevDataSeeder: {} ta bosh sahifa bo'limi", sectionCount);
        log.info("DevDataSeeder: {} ilova foydalanuvchisi, {} obuna, {} xarid",
                userAccountRepo.count(), subscriptionRepo.count(), purchaseRepo.count());
        log.info("DevDataSeeder: {} kategoriya, {} janr, {} ijodkor, {} kontent, {} qism, "
                        + "{} izoh, {} donat, {} banner, {} analitika hodisasi",
                categoryRepo.count(), genreRepo.count(), creatorRepo.count(),
                contentRepo.count(), episodeRepo.count(),
                commentRepo.count(), donationRepo.count(),
                advertisementRepo.count(), analyticsEventRepo.count());

        // ⚠️ ENG OXIRIDA: jurnalning tepasiga chiqib ketmasin, ko'z
        // qidirmasin. Backend ko'tarilganda oxirgi ko'rinadigan narsa
        // aynan shu jadval bo'ladi.
        logAccounts();
    }

    // ------------------------------------------------------------------ staff

    /**
     * Bitta dev hisobi ta'rifi.
     *
     * ⚠️ Ekish ham, ishga tushishdagi ro'yxat ham AYNAN shu ro'yxatdan
     * o'qiladi. Ilgari ro'yxat faqat kodda edi va uni bilish uchun
     * manbani ochish kerak bo'lardi; alohida hujjat yozilsa esa u
     * kodpdan ajralib, vaqt o'tib yolg'on ma'lumot berardi.
     */
    private record DevAccount(String login, String name, UserRoles role,
                              Set<Permission> permissions, String note) {
    }

    /**
     * Har bir rol uchun bittadan (worker uchun ikkita) hisob.
     *
     * <h2>⚠️ Nega barcha beshta rol bor</h2>
     * Bittasi tushib qolsa, o'sha rol ostida panel qanday
     * ko'rinishini LOKALDA umuman sinab bo'lmasdi — nosozlik faqat
     * serverda, haqiqiy xodim shikoyat qilganda bilinardi.
     *
     * ⚠️ {@code ROLE_REKTOR}, {@code ROLE_STUDENT}, {@code ROLE_TEACHER},
     * {@code ROLE_DEKAN} ataylab YO'Q: ular eski universitet
     * modulidan qolgan va {@code RoleMapper} da
     * {@link com.example.backend.Enums.PlatformRole} ga umuman
     * bog'lanmagan, ya'ni bu mahsulotda hech qayerga kira olmaydi.
     * Ularga hisob ochish faqat ro'yxatni chalkashtirardi.
     */
    private static final List<DevAccount> STAFF = List.of(
            new DevAccount("+998901110001", "Hyper Admin",
                    UserRoles.ROLE_GIPERSUPERADMIN, null, "hamma narsa"),
            new DevAccount("+998901110002", "Super Admin",
                    UserRoles.ROLE_SUPERADMIN, null, "xodim boshqaruvi"),
            new DevAccount("+998901110003", "Admin Aliyev",
                    UserRoles.ROLE_ADMIN, null, "kundalik admin"),

            // To'liq huquqli worker - kontent bilan ishlaydi
            new DevAccount("+998901110004", "Worker Kamolov",
                    UserRoles.ROLE_WORKER, EnumSet.of(
                    Permission.CONTENT_VIEW, Permission.CONTENT_CREATE, Permission.CONTENT_EDIT,
                    Permission.CONTENT_PUBLISH, Permission.CATEGORY_VIEW, Permission.GENRE_VIEW,
                    Permission.CREATOR_VIEW, Permission.CREATOR_CREATE, Permission.CREATOR_EDIT,
                    Permission.MEDIA_VIEW, Permission.MEDIA_UPLOAD, Permission.COMMENT_VIEW,
                    Permission.COMMENT_MODERATE), "to'liq huquqli"),

            // Cheklangan worker - faqat ko'radi. Ruxsat farqini tekshirish uchun.
            new DevAccount("+998901110005", "Worker Nazarova",
                    UserRoles.ROLE_WORKER, EnumSet.of(
                    Permission.CONTENT_VIEW, Permission.CATEGORY_VIEW, Permission.CREATOR_VIEW,
                    Permission.MEDIA_VIEW), "faqat ko'radi"),

            // Oddiy foydalanuvchi - admin panelga KIRA OLMASLIGINI tekshirish uchun
            new DevAccount("+998901110009", "Oddiy Foydalanuvchi",
                    UserRoles.ROLE_USER, null, "panelga KIRA OLMAYDI"));

    private void seedStaff() {
        STAFF.forEach(a -> staff(a.login(), a.name(), a.role(), a.permissions()));
    }

    /**
     * Hisob ochiladigan rollar — {@code DevAccountsTest} shu ro'yxatni
     * {@link com.example.backend.Enums.PlatformRole} bilan solishtiradi.
     *
     * ⚠️ Ochiq metod ATAYLAB: {@link #STAFF} yopiq qolsin, lekin
     * «har bir rolga hisob bor» qoidasi tekshiriladigan bo'lsin.
     * Aks holda yangi rol qo'shgan odam hisob ochishni unutardi va
     * buni faqat panelga kira olmay qolganda bilardi.
     */
    public static Set<UserRoles> seededRoles() {
        return STAFF.stream().map(DevAccount::role).collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Ishga tushishda hisoblarni konsolga chiqaradi.
     *
     * <h2>⚠️ Nega jurnalga, hujjatga emas</h2>
     * Hujjat o'qilmaydi va eskiradi. Bu ro'yxat esa har safar
     * backend ko'tarilganda ko'z oldida turadi va {@link #STAFF}
     * dan o'qilgani uchun HAR DOIM haqiqatga mos.
     *
     * ⚠️ Parolni jurnalga yozish odatda mumkin emas. Bu yerda mumkin:
     * butun sinf {@code app.dev.seed=true} ortida va bu xossa
     * serverda yoqilmaydi — {@code yuklash/application.properties}
     * da u umuman yo'q. Parol ham haqiqiy emas, dev uchun doimiy.
     */
    private void logAccounts() {
        // ⚠️ Kenglik HISOBLANADI, qo'lda sanalmaydi. Ilgari chegara
        // satrlari qo'lda yozilgandi va o'zbekcha matn bir belgiga
        // uzunroq bo'lishi bilanoq jadval qiyshayib ketdi.
        List<String> rows = new ArrayList<>();
        rows.add(String.format("  %-22s %-15s %s", "ROL", "LOGIN", "IZOH"));
        for (DevAccount a : STAFF) {
            rows.add(String.format("  %-22s %-15s %s",
                    a.role().name().replace("ROLE_", ""), a.login(), a.note()));
        }

        // ⚠️ Sozlamadagi hisob — paroli BOSHQA va u bu yerda
        // KO'RSATILMAYDI: u haqiqiy parol.
        boolean hasTestUser = testUserPhone != null && !testUserPhone.isBlank();
        if (hasTestUser) {
            rows.add(String.format("  %-22s %-15s %s", "USER (sozlamadan)",
                    OtpService.normalize(testUserPhone), "paroli sozlamada"));
        }

        String title = "  DEV HISOBLARI — parol: " + DEV_PASSWORD;
        int width = rows.stream().mapToInt(String::length).max().orElse(0);
        width = Math.max(width, title.length()) + 2;

        StringBuilder b = new StringBuilder("\n  ┌").append("─".repeat(width)).append("┐\n");
        b.append(pad(title, width)).append(pad("", width));
        for (int i = 0; i < rows.size(); i++) {
            // Sarlavha qatoridan keyin va sozlama hisobidan oldin ajratgich
            if (i == 1 || (hasTestUser && i == rows.size() - 1)) {
                b.append("  ├").append("─".repeat(width)).append("┤\n");
            }
            b.append(pad(rows.get(i), width));
        }
        b.append("  └").append("─".repeat(width)).append("┘");

        log.info(b.toString());
    }

    /** Qatorni ramka ichida kerakli kenglikkacha to'ldiradi. */
    private static String pad(String text, int width) {
        return "  │" + text + " ".repeat(Math.max(0, width - text.length())) + "│\n";
    }

    // ------------------------------------------------- ilova foydalanuvchilari

    /**
     * Turli entitlement holatidagi foydalanuvchilar.
     *
     * <h2>Nega kerak</h2>
     * {@code AccessService} to'rt xil manbadan ruxsat beradi: bepul, Premium,
     * qism xaridi, premyera xaridi. Bazada faqat xodimlar bo'lsa, bu yo'llarni
     * brauzerda umuman sinab ko'rib bo'lmaydi - hammasi bir xil "kiring"
     * javobini berardi.
     *
     * Parol xodimlarnikidek: {@code 12345678}.
     */
    private void seedAppUsers() {
        // 1. Faol Premium - hamma pullik kontentni ko'radi.
        User premium = appUser("+998901112001", "Premium Foydalanuvchi");
        grantPremium(premium, LocalDateTime.now().plusMonths(1));
        balance(premium, new BigDecimal("56000"), 456L, 56L);

        // 2. Muddati o'tgan Premium - endi ko'ra olmaydi.
        //    Aynan shu holat "obuna tugagach yopiladimi" degan savolni tekshiradi.
        User expired = appUser("+998901112002", "Muddati O'tgan");
        grantPremium(expired, LocalDateTime.now().minusDays(3));

        // 3. Bitta qismni sotib olgan - FAQAT o'sha qismni ko'radi.
        User buyer = appUser("+998901112003", "Qism Sotib Olgan");
        account(buyer);
        balance(buyer, new BigDecimal("12000"), 30L, 8L);
        episodeRepo.findAll().stream()
                .filter(e -> e.getPrice() != null)
                .findFirst()
                .ifPresent(e -> purchase(buyer, PurchaseType.EPISODE, e.getId(), e.getPrice()));

        // 4. Bloklangan - hech narsani ko'rmaydi, hatto sotib olgan bo'lsa ham.
        User blocked = appUser("+998901112004", "Bloklangan Foydalanuvchi");
        UserAccount blockedAccount = account(blocked);
        if (blockedAccount.getStatus() != UserStatus.BLOCKED) {
            blockedAccount.setStatus(UserStatus.BLOCKED);
            blockedAccount.setBlockedReason("Dev: bloklangan holatni tekshirish uchun");
            userAccountRepo.save(blockedAccount);
        }

        // 5. Oddiy - faqat bepul kontent. Reklama shunga ko'rsatiladi.
        account(appUser("+998901112005", "Bepul Foydalanuvchi"));

        // 6. Egasining o'z sinov hisobi - sozlamada raqam ko'rsatilgan bo'lsa.
        if (testUserPhone != null && !testUserPhone.isBlank()) {
            String password = testUserPassword == null || testUserPassword.isBlank()
                    ? DEV_PASSWORD
                    : testUserPassword;
            account(loginableUser(testUserPhone, testUserName, password));
        }
    }

    /**
     * Haqiqatan KIRA OLADIGAN ilova foydalanuvchisi.
     *
     * <h2>⚠️ Nega oddiy {@link #appUser} yetmaydi</h2>
     * Ikki jihatda undan farq qiladi, va ikkalasi ham kirishni butunlay
     * to'sadi - hech qanday xato ko'rsatmasdan, «raqam ro'yxatdan
     * o'tmagan» degan yolg'on javob bilan:
     *
     * <ol>
     *   <li><b>Telefon formati.</b> {@code AppAccountService.login()}
     *       raqamni {@link OtpService#normalize} orqali o'tkazadi, u esa
     *       {@code +} ni OLIB TASHLAYDI: {@code +998901112001} ->
     *       {@code 998901112001}. Bazada plyus bilan yotgan hisobni
     *       qidiruv topa olmaydi.</li>
     *   <li><b>{@code passwordSet} bayrog'i.</b> Haqiqiy ro'yxatdan
     *       o'tish uni {@code true} qiladi. {@code false} bo'lsa parol
     *       mos kelgan holatda kirish o'tadi va bayroq tuzatiladi -
     *       lekin parol xato kiritilsa, ilova «parol noto'g'ri» emas,
     *       «parol o'rnatilmagan» deydi va odamni ro'yxatdan o'tishga
     *       yuboradi.</li>
     * </ol>
     *
     * ⚠️ Yuqoridagi beshta stsenariy hisobi ({@code +99890111200x})
     * ATAYLAB tegilmadi: ular allaqachon shu bazada plyus bilan yotibdi
     * va formatni o'zgartirish ularni topilmas qilib, har ishga
     * tushirishda dublikat yaratardi. Ular token bilan sinaladi, kirish
     * orqali emas.
     */
    private User loginableUser(String rawPhone, String name, String password) {
        String phone = OtpService.normalize(rawPhone);
        return userRepo.findByPhone(phone).orElseGet(() -> userRepo.save(User.builder()
                .phone(phone)
                .name(name)
                .password(passwordEncoder.encode(password))
                .passwordSet(true)
                .roles(List.of(ensureRole(UserRoles.ROLE_USER)))
                .build()));
    }

    /**
     * Balans: pul, Stars va Coin.
     *
     * <h2>Nima uchun kerak</h2>
     * Ilgari sidda birorta ham {@code UserBalance} qatori yo'q edi, ya'ni
     * profildagi uchala son ham nol chiqardi. Nol o'zi HAQIQIY javob
     * (endpoint hisob yo'qligida nol qaytaradi), lekin bu holda profil
     * ekranini umuman sinab bo'lmasdi: to'ldirilgan va bo'sh holat bir xil
     * ko'rinardi.
     *
     * Raqamlar buyurtmachining «Screen 4» maketidan olingan.
     */
    private void balance(User user, BigDecimal money, long stars, long coins) {
        if (userBalanceRepo.findByUserId(user.getId()).isPresent()) {
            return;
        }
        userBalanceRepo.save(UserBalance.builder()
                .user(user)
                .moneyBalance(money)
                .starsBalance(stars)
                .coinBalance(coins)
                .build());
    }

    private User appUser(String phone, String name) {
        return appUser(phone, name, DEV_PASSWORD);
    }

    /**
     * ⚠️ Idempotent va parolni QAYTA YOZMAYDI: hisob bor bo'lsa shundayligicha
     * qaytariladi. Aks holda foydalanuvchi parolini o'zgartirsa, keyingi ishga
     * tushirishda eskisi tiklanardi.
     */
    private User appUser(String phone, String name, String password) {
        return userRepo.findByPhone(phone).orElseGet(() -> userRepo.save(User.builder()
                .phone(phone)
                .name(name)
                .password(passwordEncoder.encode(password))
                .roles(List.of(ensureRole(UserRoles.ROLE_USER)))
                .build()));
    }

    private UserAccount account(User user) {
        return userAccountRepo.findByUserId(user.getId())
                .orElseGet(() -> userAccountRepo.save(UserAccount.builder()
                        .user(user)
                        .status(UserStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    /**
     * Premium beradi: ham obuna yozuvi, ham hisobdagi sana.
     *
     * Ikkalasi ham kerak - {@code AccessService} tezlik uchun
     * {@code UserAccount.premiumUntil} ga qaraydi, obuna yozuvi esa tarix va
     * hisobot uchun.
     */
    private void grantPremium(User user, LocalDateTime until) {
        UserAccount acc = account(user);
        if (acc.getPremiumUntil() == null) {
            acc.setPremiumUntil(until);
            userAccountRepo.save(acc);
        }

        if (subscriptionRepo.count() > 0 && !subscriptionRepo.findAll().stream()
                .filter(x -> x.getUser() != null)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .toList().isEmpty()) {
            return;
        }

        Tariff tariff = tariffRepo.findAll().stream().findFirst().orElse(null);
        if (tariff == null) {
            // Tarif migratsiyada urug'lanadi; bo'lmasa obunani o'tkazib yuboramiz.
            log.warn("DevDataSeeder: tarif topilmadi, obuna yozuvi yaratilmadi");
            return;
        }
        subscriptionRepo.save(Subscription.builder()
                .user(user)
                .tariff(tariff)
                .startAt(until.minusMonths(tariff.getDurationMonths() == null
                        ? 1 : tariff.getDurationMonths()))
                .endAt(until)
                .paidAmount(tariff.getPrice())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void purchase(User user, PurchaseType type, Long targetId, BigDecimal amount) {
        boolean exists = !purchaseRepo
                .findAllByUserIdAndTypeAndTargetId(user.getId(), type, targetId).isEmpty();
        if (exists) {
            return;
        }
        purchaseRepo.save(Purchase.builder()
                .user(user)
                .type(type)
                .targetId(targetId)
                .amount(amount)
                .currency("UZS")
                // Dev muhitida haqiqiy to'lov yo'q - shuni yozib qo'yamiz.
                .paymentReference("DEV-SEED")
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Dev xodim hisobi — har safar QAYTA TIKLANADI.
     *
     * <h2>⚠️ Nega mavjud yozuv ham yangilanadi</h2>
     * Ilgari bu metod hisob bo'lsa darhol chiqib ketardi. Ya'ni bir
     * marta noto'g'ri holatga tushgan hisob SHU HOLDA QOLARDI:
     *
     * <ul>
     *   <li>parol qo'lda o'zgartirilgan — endi hech kim kira olmaydi;</li>
     *   <li>rol o'chirilgan yoki almashtirilgan — panel bo'limlari
     *       yo'qoladi;</li>
     *   <li>yozuv yarim yaratilgan — sababi bilinmaydi.</li>
     * </ul>
     *
     * Bularning hech biri xato bermasdi: kirish oynasi shunchaki
     * «telefon yoki parol xato» derdi va sabab bazada ekani
     * ko'rinmasdi. Bazani butunlay o'chirishdan boshqa yo'l qolmasdi.
     *
     * Endi har ishga tushishda parol, ism va rol ma'lum qiymatga
     * qaytariladi. Ya'ni {@link #DEV_PASSWORD} DOIM ishlaydi.
     *
     * ⚠️ Bu faqat dev uchun xavfsiz: butun sinf
     * {@code app.dev.seed=true} ortida turadi va bu xossa serverda
     * hech qachon yoqilmaydi.
     */
    private void staff(String phone, String name, UserRoles role, Set<Permission> permissions) {
        Role r = ensureRole(role);

        User draft = userRepo.findByPhone(phone).orElseGet(User::new);
        draft.setPhone(phone);
        draft.setName(name);
        draft.setPassword(passwordEncoder.encode(DEV_PASSWORD));

        // ⚠️ O'ZGARUVCHAN ro'yxat bo'lishi SHART.
        //
        // `List.of(...)` o'zgarmas. Yangi yozuv uchun bu muammo emas,
        // lekin MAVJUD yozuvni saqlashda Hibernate `merge` paytida eski
        // to'plamni `clear()` qilmoqchi bo'ladi va
        // `UnsupportedOperationException` bilan yiqiladi — butun ilova
        // ko'tarilmay qoladi.
        //
        // Ilgari bu ko'rinmasdi: metod faqat YANGI yozuv yaratardi.
        draft.setRoles(new ArrayList<>(List.of(r)));

        // ⚠️ Bayroq o'qilmasa ham to'g'ri qo'yiladi: yolg'on turgan
        // maydon keyinchalik uni ishlatmoqchi bo'lgan odamni
        // chalg'itadi.
        draft.setPasswordSet(true);

        final User user = userRepo.save(draft);

        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        if (!userPermissionRepo.findAllByUserId(user.getId()).isEmpty()) {
            return;
        }
        List<UserPermission> rows = permissions.stream()
                .map(p -> UserPermission.builder()
                        .userId(user.getId())
                        .permission(p)
                        .build())
                .toList();
        userPermissionRepo.saveAll(rows);
    }

    /** Role.id auto-generate emas - keyingi bo'sh id beriladi. */
    private Role ensureRole(UserRoles name) {
        Role existing = roleRepo.findByName(name);
        if (existing != null) {
            return existing;
        }
        int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
        return roleRepo.save(new Role(nextId, name));
    }

    // ------------------------------------------------------------- categories

    private List<Category> seedCategories() {
        String[][] data = {
                // slug,      UZ,           RU,             EN
                // ⚠️ Kategoriya KONTENT TURI EMAS (ТЗ §13).
                //
                // Ilgari bu ro'yxatda "podcast", "mini-series", "interview"
                // turgan edi — ular esa ContentType qiymatlari. Namuna
                // ma'lumot shu bilan noto'g'ri naqsh o'rgatardi: kimdir
                // demo'ga qarab kategoriyani tur bilan bir xil deb o'ylardi.
                //
                // Kategoriya — katalog BO'LIMI (kelib chiqishi, auditoriya,
                // mavzu). Tur — kontentning SHAKLI. Janr — uslubi.
                // Bitta kontent uchalasiga bir vaqtda ega:
                //   MINI_SERIES (tur) + Drama (kategoriya) + Romance (janr)
                {"drama", "Drama", "Драма", "Drama"},
                {"comedy", "Komediya", "Комедия", "Comedy"},
                {"uzbek", "O'zbek kinosi", "Узбекское кино", "Uzbek Cinema"},
                {"foreign", "Xorijiy", "Зарубежное", "Foreign"},
                {"kids", "Bolalar uchun", "Детям", "For Kids"},
                {"documentary", "Hujjatli", "Документальный", "Documentary"},
                {"romance", "Romantika", "Романтика", "Romance"},
        };
        List<Category> out = new ArrayList<>();
        int order = 0;
        for (String[] row : data) {
            Category c = Category.builder()
                    .slug(row[0])
                    .sortOrder(order++)
                    .active(true)
                    .icon(media.avatar(row[3], order))
                    .build();
            c.addTranslation(tr(CategoryTranslation.builder().locale(Locale.UZ).name(row[1]).build()));
            c.addTranslation(tr(CategoryTranslation.builder().locale(Locale.RU).name(row[2]).build()));
            c.addTranslation(tr(CategoryTranslation.builder().locale(Locale.EN).name(row[3]).build()));
            out.add(categoryRepo.save(c));
        }
        return out;
    }

    private CategoryTranslation tr(CategoryTranslation t) {
        return t;
    }

    // ----------------------------------------------------------------- genres

    private List<Genre> seedGenres() {
        String[][] data = {
                {"drama", "Drama", "Драма", "Drama"},
                {"comedy", "Komediya", "Комедия", "Comedy"},
                {"romance", "Romantika", "Романтика", "Romance"},
                {"action", "Jangari", "Боевик", "Action"},
                {"thriller", "Triller", "Триллер", "Thriller"},
                {"family", "Oilaviy", "Семейный", "Family"},
                {"crime", "Jinoyat", "Криминал", "Crime"},
                {"adventure", "Sarguzasht", "Приключения", "Adventure"},
        };
        List<Genre> out = new ArrayList<>();
        int order = 0;
        for (String[] row : data) {
            Genre g = Genre.builder().slug(row[0]).sortOrder(order++).active(true).build();
            g.addTranslation(GenreTranslation.builder().locale(Locale.UZ).name(row[1]).build());
            g.addTranslation(GenreTranslation.builder().locale(Locale.RU).name(row[2]).build());
            g.addTranslation(GenreTranslation.builder().locale(Locale.EN).name(row[3]).build());
            out.add(genreRepo.save(g));
        }
        return out;
    }

    // --------------------------------------------------------------- creators

    private List<Creator> seedCreators() {
        // slug, UZ ism, RU ism, EN ism, featured
        Object[][] data = {
                {"dilnoza-karimova", "Dilnoza Karimova", "Дилноза Каримова", "Dilnoza Karimova", true},
                {"sardor-rahimov", "Sardor Rahimov", "Сардор Рахимов", "Sardor Rahimov", true},
                {"malika-yusupova", "Malika Yusupova", "Малика Юсупова", "Malika Yusupova", true},
                {"jahongir-tursunov", "Jahongir Tursunov", "Жахонгир Турсунов", "Jahongir Tursunov", true},
                {"nilufar-ergasheva", "Nilufar Ergasheva", "Нилуфар Эргашева", "Nilufar Ergasheva", false},
                {"bekzod-olimov", "Bekzod Olimov", "Бекзод Олимов", "Bekzod Olimov", false},
                {"kamola-saidova", "Kamola Saidova", "Камола Саидова", "Kamola Saidova", false},
                {"otabek-nazarov", "Otabek Nazarov", "Отабек Назаров", "Otabek Nazarov", false},
        };
        List<Creator> out = new ArrayList<>();
        int i = 0;
        for (Object[] row : data) {
            String slug = (String) row[0];
            boolean featured = (Boolean) row[4];
            Creator c = Creator.builder()
                    .slug(slug)
                    .featured(featured)
                    .active(true)
                    .sortOrder(i)
                    .starsReceived(featured ? (long) (500 + i * 137) : (long) (i * 23))
                    .birthDate(LocalDate.of(1988 + (i % 12), 1 + (i % 12), 1 + (i % 27)))
                    .photo(media.avatar((String) row[3], i))
                    .cover(media.landscape((String) row[3] + " cover", i))
                    .build();
            c.addTranslation(CreatorTranslation.builder().locale(Locale.UZ)
                    .displayName((String) row[1])
                    .bio("O'zbekistonlik ijodkor. Bir nechta loyihada suratga tushgan.").build());
            c.addTranslation(CreatorTranslation.builder().locale(Locale.RU)
                    .displayName((String) row[2])
                    .bio("Узбекистанский артист. Снялся в нескольких проектах.").build());
            c.addTranslation(CreatorTranslation.builder().locale(Locale.EN)
                    .displayName((String) row[3])
                    .bio("Creator from Uzbekistan. Featured in several projects.").build());
            out.add(creatorRepo.save(c));
            i++;
        }
        return out;
    }

    // ---------------------------------------------------------------- content

    private void seedContent(List<Category> cats, List<Genre> genres, List<Creator> creators) {
        // Faslli serial - to'liq tuzilish sinovi
        Content series = content("qalbim-egasi", ContentType.SERIES, StructureType.SEASONAL,
                ContentOrientation.LANDSCAPE, PublicationStatus.PUBLISHED,
                AccessPolicy.PREMIUM_OR_PURCHASE, cats.get(0), genres.subList(0, 3), creators.subList(0, 4),
                "Qalbim egasi", "Хозяин моего сердца", "Owner of My Heart",
                new BigDecimal("15000"), true, true, 0);
        // Rus tili uchun ALOHIDA afisha - tilga bog'liq media sinovi
        addPoster(series, Locale.RU, media.landscape("Хозяин моего сердца", 11));

        Season s1 = season(series, 1, "Birinchi fasl", "Первый сезон", "Season One");
        Season s2 = season(series, 2, "Ikkinchi fasl", "Второй сезон", "Season Two");
        // 1-qism BEPUL - reklama uchun (kontent siyosatini bekor qiladi)
        episode(series, s1, 1, "Tanishuv", "Знакомство", "The Meeting", AccessPolicy.FREE, 2, 0);
        episode(series, s1, 2, "Sir", "Тайна", "The Secret", null, 1, 1);
        episode(series, s1, 3, "Qaror", "Решение", "The Decision", null, 1, 2);
        episode(series, s2, 1, "Yangi boshlanish", "Новое начало", "New Beginning", null, 1, 3);
        episode(series, s2, 2, "Yakun", "Финал", "The Finale", null, 1, 4);

        // Faslsiz mini-serial - seasonId = null bo'lishi sinovi
        Content mini = content("shahar-soyasida", ContentType.MINI_SERIES, StructureType.EPISODIC,
                ContentOrientation.LANDSCAPE, PublicationStatus.PUBLISHED,
                AccessPolicy.PREMIUM_OR_PURCHASE, cats.get(3), genres.subList(3, 5), creators.subList(1, 4),
                "Shahar soyasida", "В тени города", "In the Shadow of the City",
                new BigDecimal("15000"), true, false, 1);
        episode(mini, null, 1, "Birinchi qism", "Первая часть", "Part One", AccessPolicy.FREE, 1, 5);
        episode(mini, null, 2, "Ikkinchi qism", "Вторая часть", "Part Two", null, 1, 6);
        episode(mini, null, 3, "Uchinchi qism", "Третья часть", "Part Three", null, 1, 7);

        // Bitta qismlik film
        content("meni-kechir", ContentType.MOVIE, StructureType.SINGLE,
                ContentOrientation.LANDSCAPE, PublicationStatus.PUBLISHED,
                AccessPolicy.PREMIUM_ONLY, cats.get(0), genres.subList(0, 2), creators.subList(2, 5),
                "Meni kechir", "Прости меня", "Forgive Me", null, false, true, 2);

        // Bepul qisqa metraj
        content("orzular-ortida", ContentType.SHORT_FILM, StructureType.SINGLE,
                ContentOrientation.LANDSCAPE, PublicationStatus.PUBLISHED,
                AccessPolicy.FREE, cats.get(5), genres.subList(5, 7), creators.subList(0, 2),
                "Orzular ortida", "За мечтами", "Behind the Dreams", null, false, false, 3);

        // Podkast
        Content podcast = content("ochiq-suhbat", ContentType.PODCAST, StructureType.EPISODIC,
                ContentOrientation.LANDSCAPE, PublicationStatus.PUBLISHED,
                AccessPolicy.FREE, cats.get(2), List.of(), creators.subList(3, 5),
                "Ochiq suhbat", "Открытый разговор", "Open Talk", null, false, true, 4);
        episode(podcast, null, 1, "Kino sanoati haqida", "О киноиндустрии", "About the Film Industry",
                null, 1, 8);
        episode(podcast, null, 2, "Yosh ijodkorlar", "Молодые артисты", "Young Creators", null, 1, 9);

        // VERTIKAL kontent - Reels uslubi
        content("reels-hikoya", ContentType.CLIP, StructureType.SINGLE,
                ContentOrientation.VERTICAL, PublicationStatus.PUBLISHED,
                AccessPolicy.FREE, cats.get(1), genres.subList(1, 3), creators.subList(4, 6),
                "Qisqa hikoya", "Короткая история", "Short Story", null, false, true, 5);

        Content verticalSeries = content("reels-serial", ContentType.MINI_SERIES, StructureType.EPISODIC,
                ContentOrientation.VERTICAL, PublicationStatus.PUBLISHED,
                AccessPolicy.PREMIUM_OR_PURCHASE, cats.get(6), genres.subList(2, 4), creators.subList(2, 6),
                "Sevgi qissasi", "История любви", "A Love Story",
                new BigDecimal("15000"), true, true, 6);
        episode(verticalSeries, null, 1, "1-qism", "1 серия", "Episode 1", AccessPolicy.FREE, 1, 10);
        episode(verticalSeries, null, 2, "2-qism", "2 серия", "Episode 2", null, 1, 11);

        // Shou
        content("yulduzlar-kechasi", ContentType.SHOW, StructureType.SINGLE,
                ContentOrientation.LANDSCAPE, PublicationStatus.PUBLISHED,
                AccessPolicy.FREE, cats.get(1), List.of(), creators.subList(0, 3),
                "Yulduzlar kechasi", "Ночь звёзд", "Night of the Stars", null, true, false, 7);

        // Stream
        content("jonli-efir", ContentType.STREAM, StructureType.SINGLE,
                ContentOrientation.LANDSCAPE, PublicationStatus.PUBLISHED,
                AccessPolicy.FREE, cats.get(4), List.of(), creators.subList(1, 3),
                "Jonli efir", "Прямой эфир", "Live Stream", null, false, false, 8);

        // Turli statuslar - admin panelda filtrni sinash uchun
        content("yangi-loyiha", ContentType.MOVIE, StructureType.SINGLE,
                ContentOrientation.LANDSCAPE, PublicationStatus.DRAFT,
                AccessPolicy.PREMIUM_ONLY, cats.get(0), genres.subList(0, 1), creators.subList(0, 2),
                "Yangi loyiha", "Новый проект", "New Project", null, false, false, 9);

        content("tez-kunda", ContentType.SERIES, StructureType.SEASONAL,
                ContentOrientation.LANDSCAPE, PublicationStatus.SCHEDULED,
                AccessPolicy.PREMIUM_OR_PURCHASE, cats.get(6), genres.subList(2, 4), creators.subList(3, 6),
                "Tez kunda", "Скоро", "Coming Soon", new BigDecimal("15000"), true, false, 10);

        content("eski-arxiv", ContentType.MOVIE, StructureType.SINGLE,
                ContentOrientation.LANDSCAPE, PublicationStatus.ARCHIVED,
                AccessPolicy.FREE, cats.get(5), List.of(), creators.subList(6, 8),
                "Eski arxiv", "Старый архив", "Old Archive", null, false, false, 11);
    }

    // ---------------------------------------------------------------- izohlar

    /** Moderatsiya sahifasi bo'sh qolmasligi uchun. Ba'zilariga shikoyat qilingan. */
    private void seedComments() {
        List<Content> contents = contentRepo.findAll();
        List<User> users = userRepo.findAll().stream()
                .filter(u -> u.getPhone() != null && u.getPhone().endsWith("0009"))
                .toList();
        if (contents.isEmpty() || users.isEmpty()) {
            return;
        }

        Object[][] rows = {
                {"Juda zo'r serial, davomini kutyapman!", CommentStatus.VISIBLE, 0},
                {"Aktyorlar o'ynashi ajoyib", CommentStatus.VISIBLE, 0},
                {"Ovoz sifati yomon, tuzatinglar", CommentStatus.VISIBLE, 2},
                {"Bu yerda reklama joylashtiraman: t.me/spam", CommentStatus.HIDDEN, 7},
                {"Syujet oldingisidan kuchsizroq", CommentStatus.VISIBLE, 1},
                {"Nega yangi qism chiqmayapti?", CommentStatus.VISIBLE, 0},
                {"Haqoratli matn (moderator o'chirgan)", CommentStatus.DELETED, 11},
        };

        int i = 0;
        for (Object[] r : rows) {
            Content c = contents.get(i % contents.size());
            commentRepo.save(Comment.builder()
                    .author(users.get(0))
                    .content(c)
                    .text((String) r[0])
                    .status((CommentStatus) r[1])
                    .reportsCount((Integer) r[2])
                    .createdAt(LocalDateTime.now().minusHours(i * 7L + 1))
                    .build());
            i++;
        }
    }

    // ---------------------------------------------------- reklama / premyera

    /** Bosh sahifa bo'limlari bo'sh ko'rinmasin va analitika uchun nishon bo'lsin. */
    private void seedBanners() {
        if (advertisementRepo.count() > 0) {
            return;
        }

        Object[][] ads = {
                {"Yozgi chegirma", AdAudience.ADVERTISEMENT,
                 "Yozgi chegirma", "Летняя скидка", "Summer Sale", true},
                {"Premium taklifi", AdAudience.ADVERTISEMENT,
                 "Premium'ni sinab ko'ring", "Попробуйте Premium", "Try Premium", true},
                {"Texnik ishlar", AdAudience.ADMIN_ANNOUNCEMENT,
                 "Texnik ishlar 21-avgust", "Техработы 21 августа", "Maintenance on Aug 21", false},
        };

        int i = 0;
        for (Object[] row : ads) {
            Advertisement ad = Advertisement.builder()
                    .name((String) row[0])
                    .audience((AdAudience) row[1])
                    .status(PublicationStatus.PUBLISHED)
                    .buttonEnabled((Boolean) row[5])
                    .sortOrder(i)
                    .image(media.landscape((String) row[4], i + 20))
                    .link(InternalLink.builder()
                            .linkType((Boolean) row[5] ? LinkType.INTERNAL : LinkType.NONE)
                            .internalTargetType((Boolean) row[5] ? InternalTargetType.CONTENT : null)
                            .internalTargetId((Boolean) row[5] ? 1L : null)
                            .build())
                    .build();
            ad.addTranslation(AdvertisementTranslation.builder()
                    .locale(Locale.UZ).title((String) row[2]).buttonText("Batafsil").build());
            ad.addTranslation(AdvertisementTranslation.builder()
                    .locale(Locale.RU).title((String) row[3]).buttonText("Подробнее").build());
            ad.addTranslation(AdvertisementTranslation.builder()
                    .locale(Locale.EN).title((String) row[4]).buttonText("Learn more").build());
            advertisementRepo.save(ad);
            i++;
        }

        // Premyera kartochkalari — bosh sahifadagi «Yangi premyeralar» bo'limi uchun
        List<Content> featured = contentRepo.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getFeatured()))
                .limit(3).toList();
        int p = 0;
        for (Content c : featured) {
            String uzTitle = c.getTranslations().stream()
                    .filter(t -> t.getLocale() == Locale.UZ)
                    .map(ContentTranslation::getTitle).findFirst().orElse(c.getSlug());

            Premiere premiere = Premiere.builder()
                    .name(uzTitle + " premyerasi")
                    .content(c)
                    .status(PublicationStatus.PUBLISHED)
                    .buttonEnabled(true)
                    .sortOrder(p)
                    .image(media.landscape(uzTitle, p + 30))
                    .link(InternalLink.builder()
                            .linkType(LinkType.INTERNAL)
                            .internalTargetType(InternalTargetType.CONTENT)
                            .internalTargetId(c.getId())
                            .build())
                    .build();
            premiere.addTranslation(PremiereTranslation.builder()
                    .locale(Locale.UZ).title(uzTitle).subtitle("Tez kunda")
                    .buttonText("Treylerni ko'rish").build());
            premiere.addTranslation(PremiereTranslation.builder()
                    .locale(Locale.RU).title(uzTitle).subtitle("Скоро")
                    .buttonText("Смотреть трейлер").build());
            premiere.addTranslation(PremiereTranslation.builder()
                    .locale(Locale.EN).title(uzTitle).subtitle("Coming soon")
                    .buttonText("Watch trailer").build());
            premiereRepo.save(premiere);
            p++;
        }
    }

    // -------------------------------------------------------------- analitika

    /**
     * So'nggi 30 kun uchun hodisalar.
     *
     * Yaratilgach darhol agregatlanadi, shunda hisobot sahifasi bo'sh
     * ko'rinmaydi. Prod'da buni fon vazifasi qiladi.
     */
    private void seedAnalytics() {
        List<Content> contents = contentRepo.findAll();
        var ads = advertisementRepo.findAll();
        if (contents.isEmpty()) {
            return;
        }

        // Deterministik "tasodifiylik": har safar bir xil ma'lumot chiqsin
        long seed = 42;
        List<AnalyticsEvent> batch = new ArrayList<>();

        for (int dayBack = 29; dayBack >= 0; dayBack--) {
            LocalDate day = LocalDate.now().minusDays(dayBack);

            for (int ci = 0; ci < Math.min(6, contents.size()); ci++) {
                Content c = contents.get(ci);
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                int views = 5 + (int) Math.abs(seed % 40) - ci * 2;
                if (views < 1) {
                    views = 1;
                }
                int plays = Math.max(1, views / 2);
                int completes = Math.max(0, plays / 3);

                for (int i = 0; i < views; i++) {
                    batch.add(event(AnalyticsEventType.CONTENT_VIEW, c.getId(), day, "dev-" + (i % 12)));
                }
                for (int i = 0; i < plays; i++) {
                    batch.add(event(AnalyticsEventType.CONTENT_PLAY, c.getId(), day, "dev-" + (i % 12)));
                }
                for (int i = 0; i < completes; i++) {
                    batch.add(event(AnalyticsEventType.CONTENT_COMPLETE, c.getId(), day, "dev-" + (i % 12)));
                }
            }

            for (var ad : ads) {
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                int impressions = 20 + (int) Math.abs(seed % 60);
                int clicks = Math.max(0, impressions / 12);
                for (int i = 0; i < impressions; i++) {
                    batch.add(event(AnalyticsEventType.AD_IMPRESSION, ad.getId(), day, "dev-" + (i % 15)));
                }
                for (int i = 0; i < clicks; i++) {
                    batch.add(event(AnalyticsEventType.AD_CLICK, ad.getId(), day, "dev-" + (i % 15)));
                }
            }
        }

        analyticsEventRepo.saveAll(batch);
        // Darhol jamlanmaga aylantiramiz — hisobot sahifasi bo'sh bo'lmasin
        analyticsService.aggregate();
    }

    private AnalyticsEvent event(AnalyticsEventType type, Long targetId, LocalDate day, String deviceKey) {
        return AnalyticsEvent.builder()
                .type(type)
                .targetId(targetId)
                .eventDate(day)
                .deviceKey(deviceKey)
                .processed(false)
                .createdAt(day.atTime(12, 0))
                .build();
    }

    // ------------------------------------------------------------------ donat

    /** Donat hisoboti va reytingni ko'rish uchun. */
    private void seedDonations(List<Creator> creators) {
        List<Content> contents = contentRepo.findAll();
        List<User> users = userRepo.findAll().stream()
                .filter(u -> u.getPhone() != null && u.getPhone().endsWith("0009"))
                .toList();
        if (users.isEmpty() || creators.isEmpty()) {
            return;
        }
        User sender = users.get(0);

        for (int i = 0; i < creators.size(); i++) {
            Creator cr = creators.get(i);
            donationRepo.save(DonationTransaction.builder()
                    .sender(sender)
                    .targetType(DonationTargetType.CREATOR)
                    .targetId(cr.getId())
                    .kind(CurrencyKind.STARS)
                    .amount(50L * (creators.size() - i))
                    .createdAt(LocalDateTime.now().minusDays(i))
                    .build());
        }
        for (int i = 0; i < Math.min(4, contents.size()); i++) {
            donationRepo.save(DonationTransaction.builder()
                    .sender(sender)
                    .targetType(DonationTargetType.CONTENT)
                    .targetId(contents.get(i).getId())
                    .kind(CurrencyKind.UZCASTING_COIN)
                    .amount(100L * (4 - i))
                    .createdAt(LocalDateTime.now().minusDays(i))
                    .build());
        }
    }

    private Content content(String slug, ContentType type, StructureType structure,
                            ContentOrientation orientation, PublicationStatus status,
                            AccessPolicy access, Category category, List<Genre> genres,
                            List<Creator> creators, String uz, String ru, String en,
                            BigDecimal price, boolean featured, boolean popular, int variant) {

        Content c = Content.builder()
                .slug(slug)
                .contentType(type)
                .structureType(structure)
                .orientation(orientation)
                .status(status)
                .accessPolicy(access)
                .premierePrice(price)
                .category(category)
                .genres(new LinkedHashSet<>(genres))
                .ageRating(variant % 3 == 0 ? "16+" : "12+")
                .durationMinutes(structure == StructureType.SINGLE ? 75 + variant * 3 : null)
                .premiereDate(LocalDateTime.now().minusDays(30 - variant))
                .publicationDate(status == PublicationStatus.PUBLISHED
                        ? LocalDateTime.now().minusDays(25 - variant) : null)
                .featured(featured)
                .popular(popular)
                .viewCount((long) (1200 + variant * 733))
                .starsReceived((long) (variant * 89))
                .build();

        c.addTranslation(ContentTranslation.builder().locale(Locale.UZ).title(uz)
                .shortDescription(uz + " - qisqacha tavsif.")
                .description(uz + " haqida to'liq tavsif. Bu dev muhiti uchun namuna matn.").build());
        c.addTranslation(ContentTranslation.builder().locale(Locale.RU).title(ru)
                .shortDescription(ru + " - краткое описание.")
                .description("Полное описание «" + ru + "». Это тестовый текст для dev-среды.").build());
        c.addTranslation(ContentTranslation.builder().locale(Locale.EN).title(en)
                .shortDescription(en + " - short description.")
                .description("Full description of " + en + ". Sample text for the dev environment.").build());

        boolean vertical = orientation == ContentOrientation.VERTICAL;
        c.addMedia(ContentMedia.builder().role(MediaRole.POSTER).sortOrder(0)
                .media(vertical ? media.vertical(en, variant) : media.landscape(en, variant)).build());
        c.addMedia(ContentMedia.builder().role(MediaRole.COVER).sortOrder(0)
                .media(media.landscape(en + " cover", variant + 2)).build());
        for (int i = 0; i < 3; i++) {
            c.addMedia(ContentMedia.builder().role(MediaRole.GALLERY).sortOrder(i)
                    .media(media.landscape(en + " " + (i + 1), variant + i)).build());
        }

        // Treyler - har qanday tuzilmada bo'ladi (reklama roligi).
        c.addMedia(ContentMedia.builder().role(MediaRole.TRAILER).sortOrder(0)
                .media(media.video(en + "-trailer", 120)).build());

        // ASOSIY VIDEO faqat SINGLE uchun: ko'p qismli kontentda video
        // qismlarga biriktiriladi (EpisodeVideo), kontentga emas.
        //
        // Busiz dev bazasidagi filmlarni tomosha qilib bo'lmasdi - video
        // saqlanadigan joy yo'q edi.
        if (structure == StructureType.SINGLE) {
            // Ikkita segment: ТЗ §19 «ba'zi kinolar bir nechta video
            // segmentdan iborat bo'lishi mumkin».
            c.addMedia(ContentMedia.builder().role(MediaRole.VIDEO).sortOrder(0)
                    .media(media.video(en + "-part1", 2700)).build());
            c.addMedia(ContentMedia.builder().role(MediaRole.VIDEO).sortOrder(1)
                    .media(media.video(en + "-part2", 2700)).build());
        }

        int order = 0;
        for (Creator cr : creators) {
            CreatorProfession prof = order == 0 ? CreatorProfession.DIRECTOR
                    : (order % 2 == 0 ? CreatorProfession.ACTOR : CreatorProfession.ACTRESS);
            c.addCredit(ContentCredit.builder().creator(cr).profession(prof)
                    .characterName(prof == CreatorProfession.DIRECTOR ? null : "Qahramon " + order)
                    .sortOrder(order).build());
            order++;
        }

        return contentRepo.save(c);
    }

    private void addPoster(Content content, Locale locale, MediaAsset asset) {
        content.addMedia(ContentMedia.builder()
                .role(MediaRole.POSTER).locale(locale).sortOrder(0).media(asset).build());
        contentRepo.save(content);
    }

    private Season season(Content content, int number, String uz, String ru, String en) {
        Season s = Season.builder()
                .content(content)
                .seasonNumber(number)
                .sortOrder(number)
                .status(PublicationStatus.PUBLISHED)
                .premiereDate(LocalDateTime.now().minusDays(60L - number * 20L))
                .poster(media.landscape(en, number))
                .build();
        s.addTranslation(SeasonTranslation.builder().locale(Locale.UZ).title(uz).build());
        s.addTranslation(SeasonTranslation.builder().locale(Locale.RU).title(ru).build());
        s.addTranslation(SeasonTranslation.builder().locale(Locale.EN).title(en).build());
        return seasonRepo.save(s);
    }

    private void episode(Content content, Season season, int number,
                         String uz, String ru, String en,
                         AccessPolicy override, int videoParts, int variant) {
        Episode e = Episode.builder()
                .content(content)
                .season(season)
                .episodeNumber(number)
                .sortOrder(number)
                .status(PublicationStatus.PUBLISHED)
                .accessPolicyOverride(override)
                .price(override == AccessPolicy.FREE ? null : new BigDecimal("3000"))
                .durationSeconds(1500 + variant * 60)
                .premiereDate(LocalDateTime.now().minusDays(20L - number))
                .publicationDate(LocalDateTime.now().minusDays(18L - number))
                .viewCount((long) (300 + variant * 211))
                .thumbnail(media.landscape(en, variant))
                .build();

        e.addTranslation(EpisodeTranslation.builder().locale(Locale.UZ).title(uz)
                .shortDescription(uz + " - qisqacha.").build());
        e.addTranslation(EpisodeTranslation.builder().locale(Locale.RU).title(ru)
                .shortDescription(ru + " - кратко.").build());
        e.addTranslation(EpisodeTranslation.builder().locale(Locale.EN).title(en)
                .shortDescription(en + " - in brief.").build());

        // Bir qism bir nechta video segmentdan iborat bo'lishi mumkin
        for (int p = 1; p <= videoParts; p++) {
            e.addVideo(EpisodeVideo.builder()
                    .media(media.video(en + "-part" + p, 900))
                    .partNumber(p)
                    .sortOrder(p)
                    .build());
        }
        episodeRepo.save(e);
    }
}
