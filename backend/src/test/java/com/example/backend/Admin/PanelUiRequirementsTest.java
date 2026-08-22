package com.example.backend.Admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
}
