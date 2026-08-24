package com.example.backend.Admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §51 — panel sahifalariga qo'yilgan talablar.
 *
 * <h2>Nima uchun bu test kerak</h2>
 * Bu talablarni bir marta bajarish oson. Qiyini — YANGI sahifa
 * qo'shilganda ularni yana bajarish. Shoshib yozilgan sahifada
 * odatda faqat «hammasi yaxshi» holati qilinadi: ma'lumot kelmasa
 * bo'sh ekran, xato bo'lsa esa umuman hech narsa.
 *
 * Xato foydalanuvchida ko'rinadi, dasturchida esa yo'q — chunki uning
 * mashinasida backend doim ishlaydi.
 */
class PanelUiRequirementsTest {

    private static final Path PAGES = Path.of("../frontend/src/adminpanel/pages");

    /**
     * Ma'lumot yuklaydigan sahifalar.
     *
     * Formalar ({@code *Form}, {@code *Editor}) va {@code LoginPage} bu
     * ro'yxatga kirmaydi: ular ro'yxat emas va o'z holatlarini ota
     * sahifadan oladi.
     */
    private static boolean isListPage(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith("Page.jsx")
                && !name.equals("LoginPage.jsx");
    }

    /** Buzuvchi amallar — tasdiqlashsiz chaqirilmasligi kerak. */
    private static final List<String> DESTRUCTIVE = List.of(
            "archiveContent", "deleteSeason", "deleteEpisode",
            "deleteAd", "deletePremiere", "deletePackage",
            "blockUser", "revokePremium", "revokeDevice",
            "deleteCreator", "deleteCategory", "deleteGenre",
            "deleteMedia");

    @Nested
    @DisplayName("Har bir ro'yxat sahifasida holatlar bor")
    class States {

        @Test
        @DisplayName("Yuklash, bo'sh va xato holati")
        void everyListPageHandlesAllStates() throws IOException {
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(PAGES)) {
                for (Path file : files.filter(PanelUiRequirementsTest::isListPage).toList()) {
                    String src = Files.readString(file);
                    String name = file.getFileName().toString();

                    if (!src.contains("LoadingState")) {
                        offenders.add(name + " → LoadingState");
                    }
                    if (!src.contains("ErrorState")) {
                        offenders.add(name + " → ErrorState");
                    }
                    // ⚠️ Bo'sh holat faqat RO'YXAT chizadigan sahifada
                    // talab qilinadi.
                    //
                    // Dashboard butunlay kartochkalardan iborat va u yerda
                    // «0» — HAQIQIY nol, bo'sh holat emas. Ro'yxatsiz
                    // sahifadan bo'sh holat talab qilish testni qo'pol
                    // qilardi: u haqiqiy muammoni emas, shaklni
                    // tekshirardi.
                    //
                    // §48 dagi jadvallar qo'shilganda bu qoida ularni
                    // avtomatik qamrab oladi.
                    boolean rendersList = src.contains(".map(");
                    if (rendersList && !src.contains("EmptyState")) {
                        offenders.add(name + " → EmptyState");
                    }
                }
            }

            assertThat(offenders)
                    .as("ТЗ §51: har bir ro'yxat sahifasida yuklash, bo'sh va "
                            + "xato holati bo'lishi kerak. Ularsiz foydalanuvchi "
                            + "oq ekran ko'radi va nima bo'lganini bilmaydi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("⚠️ Xato holatida QAYTA URINISH tugmasi bor")
        void everyErrorStateOffersRetry() throws IOException {
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(PAGES)) {
                for (Path file : files.filter(PanelUiRequirementsTest::isListPage).toList()) {
                    String src = Files.readString(file);
                    if (src.contains("ErrorState") && !src.contains("onRetry")) {
                        offenders.add(file.getFileName().toString());
                    }
                }
            }

            assertThat(offenders)
                    .as("Xato ko'rsatiladi, lekin qayta urinish yo'q. Tarmoq "
                            + "uzilishi vaqtinchalik bo'ladi — foydalanuvchi "
                            + "butun sahifani yangilashga majbur bo'lmasin.")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Qidiruv va sahifalash")
    class SearchAndPaging {

        /**
         * Cheksiz o'sadigan ro'yxatlar.
         *
         * ⚠️ Bu ro'yxatga QO'SHILMAGANLAR ataylab tashqarida:
         * <ul>
         *   <li>{@code TariffsPage} — tariflar bir nechta (ТЗ §36 da
         *       to'rttasi). Sahifalash u yerda shovqin bo'lardi;</li>
         *   <li>{@code HomepagePage} — bo'limlar soni enum bilan
         *       CHEKLANGAN (13 ta), ular o'smaydi;</li>
         *   <li>{@code SettingsPage}, {@code ReportsPage},
         *       {@code DashboardPage} — ro'yxat emas.</li>
         * </ul>
         *
         * Qoidani hamma joyga yoyish testni qo'pol qilardi: u haqiqiy
         * muammoni emas, shaklni tekshirardi.
         */
        private static final List<String> UNBOUNDED_LISTS = List.of(
                "ContentPage.jsx",
                "CreatorsPage.jsx",
                "TaxonomyPage.jsx",
                "MediaPage.jsx",
                // ⚠️ Yo'l bilan: xodimlar sahifasi o'z papkasiga ko'chirilgan
                // (ro'yxat + forma + ruxsat tanlagichi bitta faylga
                // sig'masdi). Faqat nomni yozish `NoSuchFileException` berardi.
                "staff/StaffPage.jsx",
                "UsersPage.jsx",
                "CommentsPage.jsx",
                "NotificationsPage.jsx",
                "AuditPage.jsx",
                "DonationsPage.jsx");

        @Test
        @DisplayName("⚠️ Cheksiz o'sadigan ro'yxatda QIDIRUV bor")
        void unboundedListsHaveSearch() throws IOException {
            List<String> offenders = new ArrayList<>();

            for (String page : UNBOUNDED_LISTS) {
                String src = Files.readString(PAGES.resolve(page));
                // Donatlar tarixida qidiruv o'rniga hisobot filtri bor —
                // u alohida ko'rib chiqilgan.
                if (page.equals("DonationsPage.jsx")) {
                    continue;
                }
                if (!src.contains("SearchInput")) {
                    offenders.add(page);
                }
            }

            assertThat(offenders)
                    .as("Ro'yxat cheksiz o'sadi, lekin qidiruv yo'q. Admin "
                            + "kerakli yozuvni sahifalab qidirishga majbur "
                            + "bo'lardi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("⚠️ Cheksiz o'sadigan ro'yxatda SAHIFALASH bor")
        void unboundedListsHavePagination() throws IOException {
            List<String> offenders = new ArrayList<>();

            for (String page : UNBOUNDED_LISTS) {
                String src = Files.readString(PAGES.resolve(page));
                if (!src.contains("Pagination")) {
                    offenders.add(page);
                }
            }

            assertThat(offenders)
                    .as("Ro'yxat cheksiz o'sadi, lekin sahifalash yo'q. "
                            + "Butun jadval bitta so'rovda kelardi va "
                            + "platforma o'sgani sari sahifa sekinlashardi.")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Buzuvchi amallar tasdiqlanadi")
    class Confirmation {

        @Test
        @DisplayName("⚠️ Har bir buzuvchi amal tasdiqlashdan o'tadi")
        void destructiveActionsAreConfirmed() throws IOException {
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(PAGES)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".jsx")).toList()) {
                    List<String> lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        for (String action : DESTRUCTIVE) {
                            if (!lines.get(i).contains("adminApi." + action)) {
                                continue;
                            }
                            // Chaqiruvdan oldingi bir necha qatorda
                            // tasdiqlash bo'lishi kerak.
                            String window = String.join("\n",
                                    lines.subList(Math.max(0, i - 8), i + 1));
                            if (!window.contains("confirm")) {
                                offenders.add(file.getFileName() + ":" + (i + 1)
                                        + " → " + action);
                            }
                        }
                    }
                }
            }

            assertThat(offenders)
                    .as("ТЗ §51: «Delete actionlarga confirmation qo'y». "
                            + "Tasdiqlashsiz amal bitta tasodifiy bosishda "
                            + "bajariladi va uni QAYTARIB BO'LMAYDI.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Brauzer oynasi ishlatilmaydi")
        void noNativeBrowserDialogs() throws IOException {
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(PAGES)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".jsx")).toList()) {
                    String src = Files.readString(file);
                    // ⚠️ `window.confirm` va `window.prompt` TARJIMA
                    // QILINMAYDI: «OK / Cancel» brauzer tilida chiqadi.
                    // Ular panel dizayniga ham mos kelmaydi (§50).
                    if (src.contains("window.confirm(") || src.contains("window.prompt(")) {
                        offenders.add(file.getFileName().toString());
                    }
                }
            }

            assertThat(offenders)
                    .as("Brauzer oynasi o'rniga ConfirmDialog ishlatilsin")
                    .isEmpty();
        }

        @Test
        @DisplayName("Ijobiy nazorat: detektor haqiqatan qidiryapti")
        void detectorActuallyChecks() throws IOException {
            // Yuqoridagi testlar BO'SH ro'yxat kutadi. Yo'l noto'g'ri
            // bo'lsa ular ham bo'sh qaytaradi va abadiy yashil turadi.
            try (Stream<Path> files = Files.walk(PAGES)) {
                assertThat(files.filter(PanelUiRequirementsTest::isListPage).count())
                        .as("Sahifalar topilmadi — detektor noto'g'ri joyga qarayapti")
                        .isGreaterThan(5);
            }
        }
    }

    // ------------------------------------------------------- modal fokusi

    @Nested
    @DisplayName("Modal fokusi (ТЗ §97)")
    class ModalFocus {

        /**
         * ⚠️ Bu ilgari faqat IZOHDA yozilgan edi: «fokus ichida qoladi
         * (§97)». Kodda esa hech qanday fokus boshqaruvi yo'q edi.
         *
         * Natijada klaviatura bilan ishlaydigan foydalanuvchi modal
         * ochilganda orqadagi sahifada qolib ketardi va Tab bosib
         * ko'rinmaydigan tugmalar bo'ylab yurardi — modal ochiq
         * turgani holda.
         */
        @Test
        @DisplayName("Modal fokusni ichkariga oladi va tuzoqda ushlaydi")
        void modalTrapsFocus() throws java.io.IOException {
            String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "../frontend/src/adminpanel/components/Modal.jsx"));

            assertThat(src)
                    .as("ochilganda fokus ichkariga ko'chirilsin")
                    .contains(".focus()");
            assertThat(src)
                    .as("Tab bosilganda oxirgidan birinchisiga qaytsin")
                    .contains("e.key !== 'Tab'");
            assertThat(src)
                    .as("yopilgach fokus ochgan tugmaga qaytsin")
                    .contains("returnFocusRef");
            assertThat(src)
                    .as("dialog elementiga ref kerak")
                    .contains("ref={dialogRef}");
        }

        @Test
        @DisplayName("Ijobiy nazorat: detektor haqiqatan matnni o'qiydi")
        void detectorReadsTheFile() throws java.io.IOException {
            String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "../frontend/src/adminpanel/components/Modal.jsx"));
            assertThat(src).hasSizeGreaterThan(500);
            assertThat(src).contains("aria-modal");
        }
    }

    // --------------------------------------------- filtr va sahifa bog'liqligi

    @Nested
    @DisplayName("Filtr o'zgarganda sahifa boshiga qaytadi (ТЗ §72)")
    class FilterResetsPage {

        /**
         * ⚠️ Nega bu muhim.
         *
         * Foydalanuvchi 3-sahifada turib filtr qo'ysa va sahifa raqami
         * qolib ketsa, natija BO'SH chiqadi — filtrlangan ro'yxatda 3-
         * sahifa yo'q. Ekranda «hech narsa topilmadi» ko'rinadi, aslida
         * esa ma'lumot bor. Hech qanday xato chiqmaydi.
         *
         * Bu qoida hozir 11 ta sahifada qo'lda takrorlangan. Ularni
         * qayta yozish o'rniga (§90 — ishlayotgan kodni sababsiz
         * o'zgartirma) qoidaning o'zi test bilan qo'riqlanadi.
         */
        @Test
        @DisplayName("Har bir filtr sahifani nolga qaytaradi")
        void everyFilterResetsPage() throws java.io.IOException {
            List<String> violations = new ArrayList<>();

            for (java.nio.file.Path f : panelPages()) {
                String src = java.nio.file.Files.readString(f);
                if (!src.contains("setPage")) {
                    continue;
                }

                Matcher deps = Pattern
                        .compile("useApi\\(\\s*\\(\\)\\s*=>.*?\\[([^\\]]*)\\]\\s*\\)",
                                Pattern.DOTALL)
                        .matcher(src);
                if (!deps.find()) {
                    continue;
                }

                // ⚠️ Faqat FILTR O'ZGARISHI qatorlari hisobga olinadi.
                //
                // «Hammasini tozalash» tugmasi ham `setPage(0)` ni o'z
                // ichiga oladi, lekin u boshqa narsa: u qiymatlarni
                // bo'shatadi (`setQ('')`). Uni ham hisobga olsak,
                // bitta shunday tugma butun sahifani «qamralgan» qilib
                // ko'rsatardi — mutatsiya buni ko'rsatdi.
                //
                // Farq argumentda: filtr HAQIQIY QIYMAT uzatadi
                // (`setter(value)`, `setStatus(e.target.value)`),
                // tozalash esa bo'sh literal (`setQ('')`).
                Pattern passesValue = Pattern.compile(
                        "set(?!Page)\\w*\\(\\s*[^'\"\\s)]");
                String resetLines = src.lines()
                        .filter(l -> l.contains("setPage(0)"))
                        .filter(l -> passesValue.matcher(l).find())
                        .collect(java.util.stream.Collectors.joining(" "));

                for (String dep : deps.group(1).split(",")) {
                    String name = dep.trim();
                    if (name.isEmpty() || name.equals("page") || name.equals("size")) {
                        continue;
                    }
                    // Prop bo'lgan bog'liqlikning setteri yo'q — u
                    // marshrutdan keladi va `key` qoidasi bilan
                    // qamraladi (quyidagi test).
                    // ⚠️ Aniq `const [` bo'lishi shart: `[kind,` matni
                    // useApi bog'liqliklar massivida ham uchraydi va
                    // tekshiruv o'zini aldab qo'yardi.
                    if (!src.contains("const [" + name + ",")) {
                        continue;
                    }

                    String setter = "set" + Character.toUpperCase(name.charAt(0))
                            + name.substring(1);
                    // `setter` — umumiy yordamchi (onFilter(setX)) ishlatilgan holat.
                    boolean covered = resetLines.contains(setter)
                            || resetLines.contains("setter");
                    if (!covered) {
                        violations.add(f.getFileName() + " → " + name);
                    }
                }
            }

            assertThat(violations)
                    .as("filtr o'zgarganda sahifa boshiga qaytmasa, foydalanuvchi "
                            + "bo'sh ro'yxat ko'radi va ma'lumot yo'q deb o'ylaydi")
                    .isEmpty();
        }

        /**
         * ⚠️ Bitta komponent ikki xil marshrutda ishlatilsa, React uni
         * qayta ishlatadi va HOLATNI SAQLAB QOLADI.
         *
         * `TaxonomyPage` da aynan shu bo'lgan: kategoriyani tahrirlash
         * oynasi ochiq turganda «Janrlar» ga o'tilsa, oyna yopilmasdi va
         * saqlash kategoriya ma'lumotini o'sha raqamli JANR ustiga
         * yozardi — jimgina, boshqa turdagi yozuvni buzib.
         */
        @Test
        @DisplayName("Bir komponent ikki marshrutda ishlatilsa `key` bo'ladi")
        void sharedComponentRoutesAreKeyed() throws java.io.IOException {
            String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "../frontend/src/adminpanel/PanelApp.jsx"));

            Matcher m = Pattern.compile("<(\\w+)\\s+(?:key=\"[^\"]*\"\\s+)?kind=")
                    .matcher(src);
            List<String> unkeyed = new ArrayList<>();
            while (m.find()) {
                String tag = m.group(0);
                if (!tag.contains("key=")) {
                    unkeyed.add(m.group(1));
                }
            }

            assertThat(unkeyed)
                    .as("`kind` bilan ajratilgan komponentga `key` qo'yilsin - "
                            + "aks holda marshrut almashganda eski holat qoladi")
                    .isEmpty();
        }

        @Test
        @DisplayName("Detektor haqiqatan sahifalarni o'qiydi")
        void detectorReadsPages() throws java.io.IOException {
            assertThat(panelPages()).hasSizeGreaterThan(10);
        }

        private List<java.nio.file.Path> panelPages() throws java.io.IOException {
            // ⚠️ `list` emas, `walk`: sahifalar ichki papkalarga bo'lingan
            // (`staff/`, `homepage/`, `reports/`). `list` bilan detektor
            // ularni umuman ko'rmasdi va tekshiruv jimgina torayardi —
            // ya'ni yangi sahifada qoida buzilsa test buni sezmasdi.
            try (java.util.stream.Stream<java.nio.file.Path> s =
                         java.nio.file.Files.walk(java.nio.file.Path.of(
                                 "../frontend/src/adminpanel/pages"))) {
                return s.filter(p -> p.toString().endsWith(".jsx")).toList();
            }
        }
    }
}
