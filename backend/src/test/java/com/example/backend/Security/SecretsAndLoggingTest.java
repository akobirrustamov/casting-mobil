package com.example.backend.Security;

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
 * ТЗ §92 (sirlar), §93 (loglar), §94 (xato javoblari).
 *
 * <h2>Nega uchalasi bitta faylda</h2>
 * Uchalasi ham bitta savolga javob beradi: <b>server o'zi haqida
 * ortiqcha nima aytib qo'yadi?</b> Sir kodda qolsa — repozitoriyni
 * o'qigan biladi; parol logga tushsa — logni ko'rgan biladi;
 * stacktrace javobga tushsa — istalgan foydalanuvchi biladi.
 */
class SecretsAndLoggingTest {

    // -------------------------------------------------------- §92 sirlar

    @Nested
    @DisplayName("Sirlar repozitoriyda yo'q")
    class Secrets {

        /**
         * ⚠️ Faqat REPOZITORIYGA TUSHADIGAN fayllar tekshiriladi.
         *
         * ТЗ §92 «repositoryga hardcode qilma» deydi. Lokal
         * {@code application.properties} esa {@code .gitignore} da —
         * u dasturchining shaxsiy sozlamasi va u yerdagi parol hech
         * qachon repozitoriyga tushmaydi.
         *
         * Uni ham tekshirish noto'g'ri qamrov edi: bu test dasturchini
         * o'z mashinasida ishlaydigan sozlama yozishdan to'sardi.
         *
         * ⚠️ Agar kimdir faylni {@code .gitignore} dan chiqarsa,
         * tekshiruv YANA ishlaydi — quyidagi shart aynan shuni
         * ta'minlaydi.
         */
        @Test
        @DisplayName("Repozitoriyga tushadigan sozlamada qotirilgan sir yo'q")
        void productionConfigHasNoLiteralSecrets() throws IOException {
            Path local = Path.of("src/main/resources/application.properties");
            if (isGitIgnored("application.properties")) {
                // Fayl repozitoriyga tushmaydi - tekshirish o'rniga
                // namuna fayl tekshiriladi (pastdagi test).
                return;
            }
            String props = Files.readString(local);

            List<String> violations = new ArrayList<>();
            for (String line : props.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                String key = trimmed.substring(0, trimmed.indexOf('=')).toLowerCase();
                String value = trimmed.substring(trimmed.indexOf('=') + 1).trim();

                boolean sensitive = key.contains("password") || key.contains("secret")
                        || key.contains("api-key") || key.contains("token");
                // `${...}` — environmentdan; bo'sh qiymat ham xavfsiz.
                boolean fromEnv = value.startsWith("${") || value.isEmpty();
                // `*-disabled`, `*-ms` kabi sozlamalar sir emas.
                boolean flag = value.equals("true") || value.equals("false")
                        || value.matches("\\d+");

                if (sensitive && !fromEnv && !flag) {
                    violations.add(key);
                }
            }

            assertThat(violations)
                    .as("sir environment orqali berilsin - repozitoriyni "
                            + "o'qigan har kim uni ko'rmasin")
                    .isEmpty();
        }

        /**
         * ⚠️ Namuna fayl ESKIRIB QOLMASIN.
         *
         * Unda `APP_JWT_ACCESS_TOKEN_MS=6000000` (100 daqiqa) yozib
         * qo'yilgan edi — §61 da default 15 daqiqaga tushirilgandan
         * keyin ham. Shu bo'yicha sozlagan odam qisqa muddatli token
         * himoyasini bilmasdan bekor qilardi.
         */
        /** `.gitignore` da shu nomdagi qoida bormi. */
        private static boolean isGitIgnored(String fileName) throws IOException {
            Path ignore = Path.of("../.gitignore");
            if (!Files.exists(ignore)) {
                return false;
            }
            return Files.readAllLines(ignore).stream()
                    .map(String::trim)
                    .anyMatch(l -> !l.startsWith("#") && l.endsWith(fileName));
        }

        @Test
        @DisplayName("Namuna faylda haqiqiy sir yo'q va u eskirmagan")
        void exampleFileIsSafeAndCurrent() throws IOException {
            String example = Files.readString(
                    Path.of("src/main/resources/application.properties.example"));

            // Haqiqiy kalit 64 hex belgidan iborat bo'lardi.
            Matcher hex = Pattern.compile("=\\s*[0-9a-f]{32,}\\s*$", Pattern.MULTILINE)
                    .matcher(example);
            assertThat(hex.find())
                    .as("namunada haqiqiy kalitga o'xshash qiymat bo'lmasin")
                    .isFalse();

            assertThat(example)
                    .as("access token muddati kodagi default bilan mos bo'lsin")
                    .contains("APP_JWT_ACCESS_TOKEN_MS=900000");
        }
    }

    // --------------------------------------------------------- §93 loglar

    @Nested
    @DisplayName("Loglarda maxfiy qiymat yo'q")
    class Logging {

        /**
         * Ataylab ruxsat berilgan chaqiruvlar.
         *
         * Har biri uchun sabab yozilishi SHART — aks holda ro'yxat vaqt
         * o'tib «hammasi mumkin» ga aylanadi. Xuddi
         * {@code AdminEndpointGuardTest.INTENTIONALLY_OPEN} kabi.
         */
        private static final java.util.Map<String, String> ALLOWED =
                java.util.Map.of(
                        "BootstrapPasswordPolicy.rejectionReason",
                        "Parol funksiyaga KIRADI, lekin qaytgan qiymat faqat "
                                + "sabab TURI: «parol 8 belgidan qisqa». Parolning "
                                + "o'zi hech qachon matnga qo'shilmaydi.");

        @Test
        @DisplayName("Log chaqiruvlariga parol yoki token uzatilmaydi")
        void noSecretValuesInLogCalls() throws IOException {
            // Log chaqiruvining ARGUMENTLARI tekshiriladi, matni emas:
            // «Refresh token rad etildi» degan xabar xavfsiz, uzatilgan
            // `token` o'zgaruvchisi esa yo'q.
            Pattern logCall = Pattern.compile(
                    "log\\.(info|warn|error|debug|trace)\\(([^;]*)\\);", Pattern.DOTALL);
            Pattern secretArg = Pattern.compile(
                    "(?<![\"\\w])(password|rawPassword|token|refreshToken|accessToken|secret|apiKey)"
                            + "(?![\"\\w])");

            List<String> violations = new ArrayList<>();
            for (Path f : sources()) {
                Matcher m = logCall.matcher(Files.readString(f));
                while (m.find()) {
                    String args = m.group(2);
                    // Qo'shtirnoq ichidagi matnni chiqarib tashlaymiz.
                    String withoutText = args.replaceAll("\"[^\"]*\"", "");
                    boolean allowed = ALLOWED.keySet().stream()
                            .anyMatch(withoutText::contains);
                    if (!allowed && secretArg.matcher(withoutText).find()) {
                        violations.add(f.getFileName() + " → " + m.group().trim());
                    }
                }
            }

            assertThat(violations)
                    .as("log uzoq saqlanadi va uni ko'p odam o'qiydi")
                    .isEmpty();
        }

        @Test
        @DisplayName("Ishlab chiqarishda SQL loglanmaydi")
        void sqlIsNotLoggedInProduction() throws IOException {
            // SQL loglari parametrlarni ham chiqaradi - parol xeshi,
            // telefon raqami, to'lov summasi.
            //
            // Namuna doim tekshiriladi, lokal nusxa esa faqat mavjud
            // bo'lsa — sabab yuqoridagi izohda.
            for (Path p : List.of(
                    Path.of("src/main/resources/application.properties.example"),
                    Path.of("src/main/resources/application.properties"))) {
                if (Files.exists(p)) {
                    assertThat(Files.readString(p))
                            .as(p.getFileName() + " da show-sql")
                            .doesNotContain("spring.jpa.show-sql=true");
                }
            }
        }
    }

    // ---------------------------------------------------- §94 xato javobi

    @Nested
    @DisplayName("Xato javoblari ichki tuzilishni ochmaydi")
    class Errors {

        @Test
        @DisplayName("Stacktrace klientga yuborilmaydi")
        void stacktraceIsNeverSent() throws IOException {
            // ⚠️ Majburiy fayl — NAMUNA, lokal nusxa emas.
            //
            // `application.properties` `.gitignore` da: toza klonda u
            // umuman yo'q va test `NoSuchFileException` bilan
            // yiqilardi. Yangi muhit esa aynan namunadan quriladi,
            // ya'ni qoida o'sha yerda turishi kerak. Lokal nusxa
            // mavjud bo'lsa — u ham tekshiriladi.
            for (Path p : configsToCheck()) {
                String props = Files.readString(p);
                assertThat(props).as(p.getFileName() + " da stacktrace")
                        .contains("server.error.include-stacktrace=never");
                assertThat(props).as(p.getFileName() + " da exception")
                        .contains("server.error.include-exception=false");
            }
        }

        /** Namuna doim, lokal nusxa esa faqat mavjud bo'lsa. */
        private List<Path> configsToCheck() {
            List<Path> checked = new java.util.ArrayList<>();
            checked.add(Path.of("src/main/resources/application.properties.example"));
            Path local = Path.of("src/main/resources/application.properties");
            if (Files.exists(local)) {
                checked.add(local);
            }
            return checked;
        }

        @Test
        @DisplayName("Kutilmagan xatoda umumiy xabar qaytadi")
        void unexpectedErrorsAreGeneric() throws IOException {
            String src = Files.readString(Path.of(
                    "src/main/java/com/example/backend/exceptions/GlobalExceptionHandler.java"));

            Matcher m = Pattern.compile(
                    "@ExceptionHandler\\(Exception\\.class\\)(.*?)\\n    \\}", Pattern.DOTALL)
                    .matcher(src);
            assertThat(m.find()).as("umumiy ushlagich bo'lsin").isTrue();

            assertThat(m.group(1))
                    .as("kutilmagan xatoda `ex.getMessage()` qaytarilmasin - "
                            + "u ichki tafsilotni ochishi mumkin")
                    .doesNotContain("ex.getMessage()");
        }

        @Test
        @DisplayName("ТЗ sanagan xato kodlari mavjud")
        void listedErrorCodesExist() throws IOException {
            String all = allSources();
            for (String code : List.of("ACCESS_DENIED", "VALIDATION_ERROR",
                    "DUPLICATE_PHONE", "NOT_FOUND")) {
                assertThat(all).as(code).contains(code);
            }
        }

        @Test
        @DisplayName("Qoidalar haqiqatan yiqila oladi")
        void rulesCanFail() throws IOException {
            Pattern secretArg = Pattern.compile(
                    "(?<![\"\\w])(password|token)(?![\"\\w])");
            assertThat(secretArg.matcher("log.info(\"x {}\", token);"
                    .replaceAll("\"[^\"]*\"", "")).find()).isTrue();
            // Xabar matnidagi so'z ushlanmasin.
            assertThat(secretArg.matcher("log.info(\"token rad etildi\");"
                    .replaceAll("\"[^\"]*\"", "")).find()).isFalse();

            assertThat(sources()).hasSizeGreaterThan(50);
        }
    }

    // ------------------------------------------------------------ yordamchi

    private static List<Path> sources() throws IOException {
        try (Stream<Path> s = Files.walk(Path.of("src/main/java/com/example/backend"))) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    private static String allSources() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Path f : sources()) {
            sb.append(Files.readString(f)).append('\n');
        }
        return sb.toString();
    }
}
