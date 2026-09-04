package com.example.backend.Admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §64 — API dizayni va versiyalash.
 *
 * <h2>Nega ТЗ misoliga o'tilmadi</h2>
 * ТЗ {@code /api/admin/v1/...} ko'rinishini misol qilib keltiradi,
 * loyihada esa {@code /api/v1/app/admin/...} ishlatiladi. Ikkalasi ham
 * versiyalangan — farq faqat versiya raqamining joyida.
 *
 * ТЗ ning o'zi «existing API convention bo'lsa uni to'satdan sindirma»
 * deydi. Qirqdan ortiq endpointni qayta nomlash ishlab turgan panelni
 * sindirardi va evaziga hech narsa bermasdi: versiya allaqachon bor.
 *
 * <h2>Bu test nimani ushlaydi</h2>
 * Yangi endpoint versiyasiz yoki boshqa prefiks bilan qo'shilishini.
 * Aralash konvensiya eng yomon variant: klient qaysi shaklni
 * kutishini bilmay qoladi va hujjat ham, kod ham ikkiga bo'linadi.
 */
class ApiConventionTest {

    private static final Pattern CLASS_MAPPING =
            Pattern.compile("@RequestMapping\\(\"([^\"]+)\"\\)");

    private static final Pattern METHOD_MAPPING =
            Pattern.compile("@(?:Get|Post|Put|Delete|Patch)Mapping\\(\\s*(?:value\\s*=\\s*)?\"(/api[^\"]*)\"");

    @Nested
    @DisplayName("Versiyalash")
    class Versioning {

        @Test
        @DisplayName("Har bir API yo'li versiyalangan")
        void everyPathIsVersioned() throws IOException {
            List<String> unversioned = paths().stream()
                    .filter(p -> !p.startsWith("/api/v1/"))
                    .toList();

            assertThat(unversioned)
                    .as("versiyasiz yo'l qo'shilsa, keyinchalik buzuvchi "
                            + "o'zgarish kiritishning yo'li qolmaydi")
                    .isEmpty();
        }

        @Test
        @DisplayName("Yangi modul `/api/v1/app/` ostida")
        void newModuleIsUnderApp() throws IOException {
            // Eski casting moduli ataylab tashqarida - u o'zgartirilmaydi (§75).
            Set<String> legacy = Set.of(
                    "/api/v1/auth", "/api/v1/security", "/api/v1/news",
                    "/api/v1/file", "/api/v1/casting-user", "/api/v1/admin");

            List<String> strays = new ArrayList<>();
            for (Path f : newModuleSources()) {
                for (String p : pathsIn(f)) {
                    boolean ok = p.startsWith("/api/v1/app/")
                            || legacy.stream().anyMatch(p::startsWith);
                    if (!ok) {
                        strays.add(f.getFileName() + " → " + p);
                    }
                }
            }

            assertThat(strays)
                    .as("yangi modul yo'llari `/api/v1/app/` bilan boshlansin")
                    .isEmpty();
        }

        @Test
        @DisplayName("Admin endpointlari `/api/v1/app/admin/` ostida")
        void adminPathsAreGrouped() throws IOException {
            // Bu guruh SecurityConfig va RateLimitFilter uchun ham muhim:
            // ular yo'l prefiksi bo'yicha qoida qo'llaydi.
            List<String> adminPaths = paths().stream()
                    .filter(p -> p.contains("/admin"))
                    .filter(p -> !p.startsWith("/api/v1/admin"))   // eski modul
                    .toList();

            assertThat(adminPaths).isNotEmpty();
            assertThat(adminPaths)
                    .allSatisfy(p -> assertThat(p).startsWith("/api/v1/app/admin"));
        }

        @Test
        @DisplayName("Qoida haqiqatan yiqila oladi")
        void ruleCanFail() throws IOException {
            assertThat(METHOD_MAPPING.matcher("@GetMapping(\"/api/v2/thing\")").find()).isTrue();
            assertThat(CLASS_MAPPING.matcher("@RequestMapping(\"/api/no-version\")").find()).isTrue();
            assertThat(paths()).hasSizeGreaterThan(10);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private Set<String> paths() throws IOException {
        Set<String> all = new LinkedHashSet<>();
        for (Path f : sources()) {
            all.addAll(pathsIn(f));
        }
        return all;
    }

    private Set<String> pathsIn(Path f) throws IOException {
        Set<String> found = new LinkedHashSet<>();
        String src = Files.readString(f);

        Matcher c = CLASS_MAPPING.matcher(src);
        while (c.find()) {
            found.add(c.group(1));
        }
        Matcher m = METHOD_MAPPING.matcher(src);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    private List<Path> sources() throws IOException {
        try (Stream<Path> s = Files.walk(Path.of("src/main/java/com/example/backend"))) {
            return s.filter(p -> p.toString().endsWith("Controller.java")).toList();
        }
    }

    /** Yangi modul: `Admin/` va `Cms/` paketlaridagi controllerlar. */
    private List<Path> newModuleSources() throws IOException {
        return sources().stream()
                .filter(p -> p.toString().contains("/Admin/") || p.toString().contains("/Cms/"))
                .toList();
    }

    // ---------------------------------------------------- DTO qoidasi (§65)

    @Nested
    @DisplayName("DTO qoidasi")
    class DtoRule {

        /** Yangi modul entity'lari — bular javobda chiqmasligi kerak. */
        private static final Set<String> ENTITIES = Set.of(
                "Content", "Episode", "Season", "Creator", "Category", "Genre",
                "MediaAsset", "Advertisement", "Premiere", "Tariff", "Comment",
                "Notification", "Donation", "Purchase", "Subscription",
                "UserBalance", "UserAccount", "CurrencyPackage", "AuditLog",
                "RefreshToken", "ContentCredit", "ContentMedia");

        @Test
        @DisplayName("Controller entity qaytarmaydi")
        void controllersReturnDtos() throws IOException {
            Pattern ret = Pattern.compile(
                    "ResponseEntity<\\s*(?:List<)?\\s*(\\w+)");
            List<String> violations = new ArrayList<>();

            for (Path f : newModuleSources()) {
                Matcher m = ret.matcher(Files.readString(f));
                while (m.find()) {
                    if (ENTITIES.contains(m.group(1))) {
                        violations.add(f.getFileName() + " → " + m.group(1));
                    }
                }
            }

            assertThat(violations)
                    .as("entity javobda chiqsa, lazy proxy va ichki maydonlar "
                            + "ham birga ketadi - klient kutmagan ma'lumot")
                    .isEmpty();
        }

        /**
         * DTO ichida entity turi bo'lmasin.
         *
         * ⚠️ Aylanma havola aynan shu yerdan boshlanadi: `Content` ichida
         * `credits`, `ContentCredit` ichida `content` — Jackson bu
         * halqani cheksiz aylantiradi va so'rov `StackOverflowError`
         * bilan tugaydi. Bundan tashqari lazy proxy'ga tegish
         * tranzaksiyadan tashqarida xato beradi.
         */
        @Test
        @DisplayName("DTO maydonlari entity emas")
        void dtoFieldsAreNotEntities() throws IOException {
            Pattern field = Pattern.compile(
                    "private\\s+(?:List<|Set<|Map<[^,]+,\\s*)?(\\w+)[>\\s]");
            List<String> violations = new ArrayList<>();

            try (Stream<Path> s = Files.walk(Path.of(
                    "src/main/java/com/example/backend/Admin/Dto"))) {
                for (Path f : s.filter(p -> p.toString().endsWith(".java")).toList()) {
                    Matcher m = field.matcher(Files.readString(f));
                    while (m.find()) {
                        if (ENTITIES.contains(m.group(1))) {
                            violations.add(f.getFileName() + " → " + m.group(1));
                        }
                    }
                }
            }

            assertThat(violations)
                    .as("DTO faqat oddiy turlarni va boshqa DTO'larni saqlasin")
                    .isEmpty();
        }

        @Test
        @DisplayName("Qoida haqiqatan yiqila oladi")
        void ruleCanFail() {
            Pattern ret = Pattern.compile("ResponseEntity<\\s*(?:List<)?\\s*(\\w+)");
            Matcher m = ret.matcher("public ResponseEntity<Content> get() {");
            assertThat(m.find()).isTrue();
            assertThat(ENTITIES.contains(m.group(1))).isTrue();

            Pattern field = Pattern.compile(
                    "private\\s+(?:List<|Set<|Map<[^,]+,\\s*)?(\\w+)[>\\s]");
            Matcher f = field.matcher("    private List<ContentCredit> credits;");
            assertThat(f.find()).isTrue();
            assertThat(ENTITIES.contains(f.group(1))).isTrue();
        }
    }

    // ----------------------------------------------------- sahifalash (§95)

    @Nested
    @DisplayName("Sahifalash")
    class Pagination {

        /**
         * Sahifalanmasligi ATAYLAB qabul qilingan endpointlar.
         *
         * ТЗ §95 «users, content, comments, creators, ads, audit logs va
         * transactions uchun pagination majburiy» deydi. Ulardan
         * oltitasi sahifalangan. Reklama esa istisno va sabab quyida —
         * har bir istisno uchun sabab yozilishi SHART, aks holda ro'yxat
         * vaqt o'tib «hammasi mumkin» ga aylanadi.
         */
        private static final Map<String, String> UNPAGINATED = Map.ofEntries(
                Map.entry("advertisements",
                        "Karusel TARTIBLANADIGAN ro'yxat (§81.4). Sahifalansa "
                                + "2-sahifadagi bannerni 1-sahifaga ko'chirib "
                                + "bo'lmasdi. O'chirilgani arxivlanadi va ro'yxatdan "
                                + "chiqadi (§58), ya'ni ro'yxat cheksiz o'smaydi."),
                Map.entry("premieres", "Reklama bilan bir xil: tartiblanadi va arxivlanadi."),
                Map.entry("sections", "Bosh sahifa sozlamasi - bir necha bo'lim, tartiblanadi."),
                Map.entry("reorderSections", "Tartiblash amalining natijasi."),
                Map.entry("sectionItems", "Bitta bo'lim ichidagi qo'lda tanlangan kontent."),
                Map.entry("replaceSectionItems", "Tartiblash amalining natijasi."),
                Map.entry("homepageCreators", "Tanlangan ijodkorlar - chegarasi so'rovda."),
                Map.entry("tariffs", "Sozlama ro'yxati: bir necha tarif."),
                Map.entry("list",
                        "Promokodlar ro'yxati (`PromocodeAdminController`). Aksiya "
                                + "kodlari — o'nlab, yuzlab emas: har biri qo'lda "
                                + "yaratiladi va admin ularni bir ko'zda ko'rishi "
                                + "kerak. O'chirish yo'q, lekin muddati o'tganlari "
                                + "holat bilan belgilanadi va ro'yxat cheksiz "
                                + "o'smaydi."),
                Map.entry("redemptions",
                        "BITTA promokodni kimlar ishlatgani. Soni kodning "
                                + "`maxRedemptions` chegarasi bilan cheklangan — "
                                + "cheksiz kodlar kam va ular uchun sahifalash "
                                + "keyinroq qo'shiladi."),
                Map.entry("mine",
                        "Foydalanuvchining O'ZI ishlatgan promokodlari. Bitta "
                                + "odam uchun bu bir nechta qator: bitta kodni ikki "
                                + "marta ishlatib bo'lmaydi (`uk_promocode_user`)."),
                Map.entry("packages", "Sozlama ro'yxati: bir necha valyuta paketi."),
                Map.entry("seasons", "BITTA kontent fasllari - o'sha kontent hajmi bilan chegaralangan."),
                Map.entry("episodes", "BITTA kontent qismlari - o'sha kontent hajmi bilan chegaralangan."),
                Map.entry("usage", "Bitta fayl qayerda ishlatilgani (§26)."),
                Map.entry("settings",
                        "Platforma sozlamalari - kalitlar to'plami kodda "
                                + "belgilangan (`SettingKeys`), foydalanuvchi "
                                + "yangisini qo'sha olmaydi."),
                Map.entry("devices",
                        "BITTA foydalanuvchi qurilmalari. Soni "
                                + "`account.device.limit` sozlamasi bilan "
                                + "chegaralangan (ТЗ bo'yicha 2 ta)."),
                Map.entry("categories",
                        "Ilovaning KATALOG MENYUSI: faol kategoriyalar, admin "
                                + "bergan tartibda. Klient bosh sahifani chizish "
                                + "uchun BARCHA qatorni bilishi kerak - menyuni "
                                + "sahifalash uni yarim chizilgan holda "
                                + "qoldirardi. Kartochkalarning o'zi bu yerda "
                                + "yo'q: har bir kategoriya alohida, sahifalab "
                                + "so'raladi (`/catalog/categories/{id}`)."));

        /**
         * ⚠️ Bu test asosan KELAJAK uchun.
         *
         * Bugungi ro'yxatlar chegaralangan, lekin kimdir
         * {@code List<ContentListDto> allContent()} qo'shsa, u dastlab
         * ishlaydi va faqat ma'lumot o'sgach yiqiladi — o'shanda ham
         * sekinlik sifatida, xato sifatida emas.
         */
        @Test
        @DisplayName("Sahifalanmagan ro'yxat sababsiz qo'shilmaydi")
        void everyUnpaginatedListIsJustified() throws IOException {
            Pattern listReturn = Pattern.compile(
                    "ResponseEntity<List<[^>]+>>\\s+(\\w+)\\s*\\(");

            List<String> undocumented = new ArrayList<>();
            for (Path f : sources()) {
                Matcher m = listReturn.matcher(Files.readString(f));
                while (m.find()) {
                    String method = m.group(1);
                    if (!UNPAGINATED.containsKey(method)) {
                        undocumented.add(f.getFileName() + "#" + method);
                    }
                }
            }

            assertThat(undocumented)
                    .as("yangi ro'yxat endpointi sahifalansin yoki sababi "
                            + "UNPAGINATED ro'yxatiga yozilsin")
                    .isEmpty();
        }

        @Test
        @DisplayName("ТЗ sanagan ro'yxatlar sahifalangan")
        void listedResourcesArePaginated() throws IOException {
            // Bular cheksiz o'sadi: foydalanuvchi, kontent, izoh,
            // ijodkor, audit va tranzaksiya.
            String all = allSources();
            for (String method : List.of("users", "contents", "comments",
                    "creators", "list", "donationTransactions")) {
                // Har biri PageResponse qaytarishi kerak.
                assertThat(all)
                        .as(method + " uchun sahifalash")
                        .contains("PageResponse");
            }

            // Aniqroq: audit va foydalanuvchi endpointlari.
            assertThat(Files.readString(Path.of(
                    "src/main/java/com/example/backend/Admin/Controller/AuditLogController.java")))
                    .contains("PageResponse<AuditLogDto>");
            assertThat(Files.readString(Path.of(
                    "src/main/java/com/example/backend/Admin/Controller/UserAdminController.java")))
                    .contains("PageResponse");
        }

        @Test
        @DisplayName("Har bir sahifalangan endpointda hajm chegarasi bor")
        void pageSizeIsCapped() throws IOException {
            // ⚠️ `?size=100000` bilan butun jadvalni tortib olish mumkin
            // bo'lmasin - bu §95 ning asosiy maqsadi.
            //
            // Har bir FAYL alohida tekshiriladi: umumiy `contains`
            // boshqa fayldagi chegara tufayli o'tib ketardi va
            // mutatsiya buni ko'rsatdi.
            // O'ZGARUVCHI hajm bilan sahifa so'ralgan joylar qidiriladi.
            // `PageRequest.of(0, 5)` kabi qotirilgan chegara xavfsiz -
            // uni foydalanuvchi boshqara olmaydi.
            Pattern variableSize = Pattern.compile(
                    "PageRequest\\.of\\([^,]+,\\s*([a-zA-Z_]\\w*)\\s*\\)");

            List<String> uncapped = new ArrayList<>();
            for (Path f : sources()) {
                String src = Files.readString(f);
                if (!variableSize.matcher(src).find()) {
                    continue;
                }
                // Chegara ifodasi har xil yozilishi mumkin
                // (`Math.min(Math.max(1, size), 200)` yoki
                // `Math.min(Math.max(limit, 1), 50)`), shuning uchun
                // aniq matn emas, `Math.min` borligi tekshiriladi.
                if (!src.contains("Math.min(")) {
                    uncapped.add(f.getFileName().toString());
                }
            }

            assertThat(uncapped)
                    .as("sahifa hajmiga yuqori chegara qo'yilsin")
                    .isEmpty();
        }

        @Test
        @DisplayName("Qoida haqiqatan yiqila oladi")
        void ruleCanFail() {
            Pattern listReturn = Pattern.compile(
                    "ResponseEntity<List<[^>]+>>\\s+(\\w+)\\s*\\(");
            Matcher m = listReturn.matcher(
                    "public ResponseEntity<List<ContentListDto>> allContent() {");
            assertThat(m.find()).isTrue();
            assertThat(UNPAGINATED).doesNotContainKey(m.group(1));
        }

        private String allSources() throws IOException {
            StringBuilder sb = new StringBuilder();
            for (Path f : sources()) {
                sb.append(Files.readString(f)).append('\n');
            }
            return sb.toString();
        }
    }
}
