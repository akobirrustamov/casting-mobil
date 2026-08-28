package com.example.backend;

import org.junit.jupiter.api.DisplayName;
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
 * Sozlama fayllaridagi {@code ${...}} havolalari.
 *
 * <h2>⚠️ Nima uchun bu test yozildi</h2>
 * {@code application.properties} da shunday qator turgan edi:
 *
 * <pre>
 *   spring.datasource.password=${akow8434}
 * </pre>
 *
 * Bu parol EMAS — {@code akow8434} nomli xususiyatga havola. Bunday
 * xususiyat yo'q, ya'ni prod profilida ilova
 * {@code Could not resolve placeholder 'akow8434'} bilan
 * KO'TARILMASDI.
 *
 * Nosozlik lokalda BILINMASDI: {@code dev} profili o'z
 * {@code datasource} ini beradi va bu qatorga umuman yetib bormaydi.
 * U faqat serverda, birinchi ishga tushirishda chiqardi — ya'ni eng
 * yomon paytda.
 *
 * <h2>Qoida</h2>
 * Har bir havola {@code ${NOM:zaxira}} shaklida bo'lishi kerak.
 * Zaxirasiz {@code ${NOM}} faqat environment o'zgaruvchisi ALBATTA
 * beriladigan joyda ma'noli, lekin unda ham xato qilish oson.
 */
class ConfigurationPlaceholderTest {

    private static final Path RESOURCES = Path.of("src/main/resources");

    /** {@code ${...}} ichidagi hamma narsa. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)}");

    /**
     * Bu havolalar zaxirasiz ham TO'G'RI.
     *
     * ⚠️ Ular Spring yoki tizim tomonidan HAR DOIM beriladi, ya'ni
     * hech qachon hal qilinmay qolmaydi.
     */
    private static final List<String> ALWAYS_AVAILABLE = List.of(
            "java.io.tmpdir",
            "user.home",
            "user.dir",
            "PID",
            "spring.application.name");

    private List<Path> propertyFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(RESOURCES)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".properties"))
                    .toList();
        }
    }

    @Test
    @DisplayName("Har bir ${...} havolasida ZAXIRA qiymat bor")
    void everyPlaceholderHasADefault() throws IOException {
        List<String> broken = new ArrayList<>();

        for (Path file : propertyFiles()) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                Matcher matcher = PLACEHOLDER.matcher(line);
                while (matcher.find()) {
                    String reference = matcher.group(1);

                    // `${NOM:zaxira}` — to'g'ri shakl.
                    if (reference.contains(":")) {
                        continue;
                    }
                    if (ALWAYS_AVAILABLE.contains(reference)) {
                        continue;
                    }

                    broken.add(String.format("%s:%d → ${%s}",
                            file.getFileName(), i + 1, reference));
                }
            }
        }

        assertThat(broken)
                .as("Zaxirasiz havola topildi. Prod profilida ilova KO'TARILMAYDI. "
                        + "To'g'ri shakl: ${NOM:zaxira}")
                .isEmpty();
    }

    /**
     * ⚠️ Test profili ISHLAB CHIQARISH bilan mos bo'lishi kerak.
     *
     * <h2>Nima uchun</h2>
     * Test profili prod'dan farq qilsa, testlar ishlaydigan tizimni
     * emas, BOSHQA narsani sinaydi. Ular yashil bo'ladi, prod esa
     * yiqiladi — yoki teskarisi.
     *
     * Aynan shunday bo'ldi: {@code application-test.properties} gitignore
     * tufayli yo'qolib, qayta yozilgan va unda
     * {@code spring.jpa.open-in-view=false} paydo bo'lgan. Prod'da esa
     * u {@code true} va eski casting moduli unga TAYANADI —
     * {@code CastingUser.photos} lazy yuklanadi.
     *
     * Natijada 4 ta test yiqildi, ishlab chiqarishda esa hech narsa
     * o'zgarmagan edi.
     *
     * <h2>Nima solishtiriladi</h2>
     * Hamma narsa emas — faqat XATTI-HARAKATGA ta'sir qiladigan
     * sozlamalar. Baza manzili, dialekt va Flyway ataylab farq qiladi:
     * testlar H2 da ishlaydi.
     */
    @Test
    @DisplayName("Test profili prod bilan MOS — xatti-harakatga ta'sir qiluvchi sozlamalar")
    void testProfileMatchesProduction() throws IOException {
        Path main = RESOURCES.resolve("application.properties");
        Path test = Path.of("src/test/resources/application-test.properties");
        if (!Files.isRegularFile(main) || !Files.isRegularFile(test)) {
            return;
        }

        // ⚠️ Ro'yxat ATAYLAB qisqa va qo'lda yozilgan. Hamma sozlamani
        // solishtirish testni shovqinli qilardi va uni o'chirib
        // qo'yishga olib borardi.
        List<String> mustMatch = List.of(
                "spring.jpa.open-in-view",
                "spring.jpa.hibernate.ddl-auto");

        List<String> mismatched = new ArrayList<>();
        for (String key : mustMatch) {
            String mainValue = valueOf(main, key);
            String testValue = valueOf(test, key);

            if (mainValue == null || testValue == null) {
                continue;
            }
            if (!mainValue.equals(testValue)) {
                mismatched.add(String.format("%s: prod=%s, test=%s",
                        key, mainValue, testValue));
            }
        }

        assertThat(mismatched)
                .as("Test profili prod'dan farq qiladi — testlar boshqa tizimni sinaydi")
                .isEmpty();
    }

    /** Fayldagi kalitning qiymati. Izohlar e'tiborga olinmaydi. */
    private String valueOf(Path file, String key) throws IOException {
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || !trimmed.startsWith(key + "=")) {
                continue;
            }
            return trimmed.substring(key.length() + 1).trim();
        }
        return null;
    }

    /**
     * ⚠️ Namuna fayli haqiqiy maxfiy qiymat SAQLAMASLIGI kerak.
     *
     * U repozitoriyga tushadi va undagi har qanday parol yoki kalit
     * ochiq matn bo'lib qoladi.
     */
    @Test
    @DisplayName("Namuna faylida haqiqiy parol yoki kalit YO'Q")
    void exampleFileHasNoRealSecrets() throws IOException {
        Path example = RESOURCES.resolve("application.properties.example");
        if (!Files.isRegularFile(example)) {
            return;
        }

        List<String> suspicious = new ArrayList<>();
        List<String> lines = Files.readAllLines(example);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("#") || !line.contains("=")) {
                continue;
            }

            String key = line.substring(0, line.indexOf('=')).toLowerCase();
            String value = line.substring(line.indexOf('=') + 1).trim();

            boolean secretKey = key.contains("password")
                    || key.contains("secret")
                    || key.contains("access-key");

            // Xavfsiz shakllar:
            //   (bo'sh)                     — qiymat berilmagan
            //   ${NOM:...}                  — environment havolasi
            //   <matn>                      — HUJJAT o'rinbosari
            //
            // ⚠️ Uchinchisi ham kerak: namuna faylida
            // `<openssl rand -hex 32>` kabi ko'rsatmalar bor va ular
            // maxfiy qiymat EMAS. Ularsiz test soxta signal berardi va
            // soxta signal beradigan testni odamlar e'tiborsiz
            // qoldirishni o'rganadi.
            boolean safe = value.isEmpty()
                    || value.startsWith("${")
                    || (value.startsWith("<") && value.endsWith(">"));

            if (secretKey && !safe) {
                suspicious.add(String.format("%d-qator: %s", i + 1, key));
            }
        }

        assertThat(suspicious)
                .as("Namuna faylida ochiq maxfiy qiymat — u repozitoriyga tushadi")
                .isEmpty();
    }
}
