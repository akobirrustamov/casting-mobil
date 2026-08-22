package com.example.backend.Admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §50 — brend va dizayn tokenlari.
 *
 * <h2>Nima uchun bu testni yozdim</h2>
 * Buyurtmachi aniq ogohlantirgan: «Exact color aniqlanmagan bo'lsa
 * kodning hamma joyiga hex rang tarqatib yuborma.»
 *
 * Bu talabni bir marta bajarish oson — hozir palitra bitta faylda.
 * Lekin uni SAQLAB QOLISH qiyin: har bir yangi sahifada shoshib
 * {@code color: '#fff'} yozib qo'yish tabiiy. Kod ishlaydi, ko'zga
 * ham chiroyli ko'rinadi — va gammani almashtirish kerak bo'lganda
 * o'sha tarqoq qiymatlar qolib ketadi.
 *
 * O'shanda muammo ko'rinadi, lekin sabab emas: panel yarmi yangi rangda,
 * yarmi eskisida bo'ladi va har birini qo'lda qidirishga to'g'ri keladi.
 */
class DesignTokensTest {

    private static final Path PANEL = Path.of("../frontend/src/adminpanel");
    private static final Path THEME = PANEL.resolve("theme/panel.css");

    /** {@code #fff}, {@code #1E3163}, {@code #ffffffcc} — barchasi. */
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{3,8}\\b");

    /** {@code rgb(...)} va {@code rgba(...)} ham xuddi shunday tarqoq rang. */
    private static final Pattern RGB = Pattern.compile("\\brgba?\\s*\\(");

    @Nested
    @DisplayName("Ranglar bir joyda")
    class SinglePlace {

        @Test
        @DisplayName("⚠️ Komponentlarda hex rang YO'Q")
        void noHexColoursInComponents() throws IOException {
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(PANEL)) {
                for (Path file : files.filter(DesignTokensTest::isScript).toList()) {
                    String source = Files.readString(file);
                    Matcher m = HEX.matcher(source);
                    while (m.find()) {
                        offenders.add(PANEL.relativize(file) + " → " + m.group());
                    }
                }
            }

            assertThat(offenders)
                    .as("Komponentga hex rang yozilgan. ТЗ §50: ranglar faqat "
                            + "`theme/panel.css` da. Aks holda gammani "
                            + "almashtirganda panel yarmi yangi rangda, yarmi "
                            + "eskisida qoladi va har birini qo'lda qidirishga "
                            + "to'g'ri keladi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Komponentlarda rgb()/rgba() ham yo'q")
        void noRgbColoursInComponents() throws IOException {
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(PANEL)) {
                for (Path file : files.filter(DesignTokensTest::isScript).toList()) {
                    if (RGB.matcher(Files.readString(file)).find()) {
                        offenders.add(PANEL.relativize(file).toString());
                    }
                }
            }

            assertThat(offenders).isEmpty();
        }

        @Test
        @DisplayName("Ijobiy nazorat: detektor haqiqatan hex topadi")
        void detectorFindsHex() {
            // Yuqoridagi testlar BO'SH ro'yxat kutadi. Naqsh buzilsa yoki
            // yo'l noto'g'ri bo'lsa ular ham bo'sh qaytaradi va abadiy
            // yashil turadi — hech narsani tekshirmasdan.
            assertThat(HEX.matcher("color: '#fff'").find()).isTrue();
            assertThat(HEX.matcher("background: #1E3163;").find()).isTrue();
            assertThat(HEX.matcher("var(--brand-primary)").find()).isFalse();
        }
    }

    @Nested
    @DisplayName("ТЗ §50 token shartnomasi")
    class TokenContract {

        @Test
        @DisplayName("To'qqizta token ham e'lon qilingan")
        void allNineTokensAreDeclared() throws IOException {
            String css = Files.readString(THEME);

            List<String> required = List.of(
                    "--brand-primary",
                    "--brand-secondary",
                    "--background",
                    "--surface",
                    "--text",
                    "--muted",
                    "--danger",
                    "--warning",
                    "--success");

            List<String> missing = required.stream()
                    .filter(token -> !css.contains(token + ":"))
                    .toList();

            assertThat(missing)
                    .as("ТЗ §50 da so'ralgan token e'lon qilinmagan")
                    .isEmpty();
        }

        @Test
        @DisplayName("⚠️ Holat ranglari brenddan MUSTAQIL")
        void statusColoursAreIndependentOfBrand() throws IOException {
            String css = Files.readString(THEME);

            // Xato qizil, ogohlantirish sariq, muvaffaqiyat yashil bo'lib
            // qolishi kerak. Brend rangiga moslashtirilsa, «xato» va
            // «asosiy tugma» bir xil ko'rinardi va ogohlantirish o'z
            // kuchini yo'qotardi.
            for (String status : List.of("--danger", "--warning", "--success")) {
                String line = lineOf(css, status);
                assertThat(line)
                        .as(status + " brend tokeniga bog'lanmasligi kerak")
                        .doesNotContain("--brand-primary")
                        .doesNotContain("--brand-secondary");
            }
        }

        @Test
        @DisplayName("Eski nomlar yangi tokenlardan OLINADI — manba bitta")
        void legacyNamesDeriveFromTokens() throws IOException {
            String css = Files.readString(THEME);

            // `--p-*` nomlari panel CSS'ida keng ishlatiladi. Ularni bir
            // yo'la almashtirish katta va xavfli tahrir bo'lardi, shuning
            // uchun ular yangi tokenlardan olinadi: nomi ikkita, manba bitta.
            assertThat(lineOf(css, "--p-primary")).contains("var(--brand-primary)");
            assertThat(lineOf(css, "--p-bg")).contains("var(--background)");
            assertThat(lineOf(css, "--p-text")).contains("var(--text)");
        }

        @Test
        @DisplayName("Gammani almashtirish uchun bitta fayl yetarli")
        void wholePaletteLivesInOneFile() throws IOException {
            // Barcha hex qiymatlar SHU faylda. Boshqa CSS fayllarda
            // rang bo'lmasligi kerak.
            List<String> offenders = new ArrayList<>();
            try (Stream<Path> files = Files.walk(PANEL)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".css")).toList()) {
                    if (file.equals(THEME)) {
                        continue;
                    }
                    if (HEX.matcher(Files.readString(file)).find()) {
                        offenders.add(PANEL.relativize(file).toString());
                    }
                }
            }
            assertThat(offenders)
                    .as("Rang boshqa CSS faylga tarqalgan")
                    .isEmpty();
        }
    }

    /** Faqat komponent fayllari — tarjima fayli emas. */
    private static boolean isScript(Path file) {
        String name = file.toString();
        return (name.endsWith(".jsx") || name.endsWith(".js"))
                && !name.endsWith("i18n.js");
    }

    private static String lineOf(String css, String token) {
        for (String line : css.split("\n")) {
            if (line.trim().startsWith(token + ":")) {
                return line;
            }
        }
        return "";
    }
}
