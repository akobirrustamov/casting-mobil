package com.example.backend.Cms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §57 — ID strategiyasi.
 *
 * <h2>Qoida</h2>
 * «Existing project qaysi ID strategiyasidan foydalansa, uni imkon qadar
 * davom ettir. Mavjud entity `Long` ishlatsa hamma narsani bir kunda
 * UUIDga rewrite qilma. Consistency saqla.»
 *
 * <h2>Loyihadagi holat</h2>
 * <table>
 *   <tr><td>Eski casting moduli</td><td>aralash: {@code UUID} ({@code User}),
 *       {@code Integer} ({@code CastingUser}, {@code News}),
 *       {@code int} ({@code Role})</td></tr>
 *   <tr><td>Yangi CMS moduli</td><td>{@code Long} + {@code IDENTITY} —
 *       istisnosiz</td></tr>
 * </table>
 *
 * Eski qism O'ZGARTIRILMAYDI: uning ID turini almashtirish barcha FK
 * ustunlarini qayta yozishni talab qilardi va mavjud ma'lumot bilan
 * bog'liq xavf foydadan katta.
 *
 * <h2>Nima uchun bu test</h2>
 * Izchillikni saqlash — bir marta emas, DOIM qilinadigan ish. Yangi
 * entity qo'shgan dasturchi «UUID zamonaviyroq» deb o'ylab, aralashmani
 * yana chuqurlashtirishi mumkin. Kod ishlaydi, testlar yashil — va
 * loyihada uchinchi ID turi paydo bo'ladi.
 */
class IdStrategyTest {

    private static final Path CMS = Path.of(
            "src/main/java/com/example/backend/Cms/Entity");
    private static final Path LEGACY = Path.of(
            "src/main/java/com/example/backend/Entity");

    /**
     * ⚠️ Naqsh TO'LIQ nomlarni ham tanishi kerak.
     *
     * Birinchi variantda u faqat `private Long id` ni topardi va
     * `private java.util.UUID id` e'tiborsiz qolardi — ya'ni to'liq nom
     * yozgan dasturchi tekshiruvdan bemalol o'tib ketardi.
     *
     * Buni mutatsiya ko'rsatdi: UUID ga o'tkazsam test o'tib ketdi.
     */
    private static final Pattern ID_FIELD = Pattern.compile(
            "@Id\\s*(?:@GeneratedValue\\([^)]*\\)\\s*)?(?:@Column\\([^)]*\\)\\s*)?"
                    + "private\\s+([\\w.]+)\\s+(\\w+)\\s*;");

    /** {@code entity -> id turi}. */
    private Map<String, String> idTypes(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                Matcher m = ID_FIELD.matcher(Files.readString(f));
                if (m.find()) {
                    String name = f.getFileName().toString().replace(".java", "");
                    String type = m.group(1);
                    // `java.util.UUID` -> `UUID`: taqqoslash oddiy nom bo'yicha.
                    type = type.substring(type.lastIndexOf('.') + 1);
                    out.put(name, type);
                }
            }
        }
        return out;
    }

    @Nested
    @DisplayName("Yangi modul izchil")
    class NewModule {

        /**
         * Tabiiy kalitli istisnolar.
         *
         * Bularda ID — sun'iy raqam emas, MA'NOLI qiymat:
         * <ul>
         *   <li>{@code PlatformSetting} — kalitning o'zi
         *       ({@code pricing.episode.default});</li>
         *   <li>{@code UploadSession} — yuklash tokeni.</li>
         * </ul>
         * Ularga qo'shimcha raqamli ID berish faqat ortiqcha ustun
         * qo'shardi va qidiruvni bir bosqich uzaytirardi.
         */
        private static final Map<String, String> NATURAL_KEYS = Map.of(
                "PlatformSetting", "String",
                "UploadSession", "String");

        @Test
        @DisplayName("⚠️ Barcha CMS entity'lari `Long` ishlatadi")
        void everyCmsEntityUsesLong() throws IOException {
            List<String> offenders = new ArrayList<>();

            idTypes(CMS).forEach((entity, type) -> {
                String expected = NATURAL_KEYS.getOrDefault(entity, "Long");
                if (!type.equals(expected)) {
                    offenders.add(entity + " -> " + type + " (kutilgan: " + expected + ")");
                }
            });

            assertThat(offenders)
                    .as("ТЗ §57: «Consistency saqla». Yangi modulda ID turi "
                            + "`Long` — yangi entity boshqa turdan foydalansa, "
                            + "loyihada uchinchi ID turi paydo bo'ladi va har "
                            + "bir bog'lanishda tur o'girish kerak bo'ladi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Ijobiy nazorat: entity'lar haqiqatan o'qildi")
        void entitiesWereActuallyRead() throws IOException {
            // Yuqoridagi test BO'SH ro'yxat kutadi. Naqsh buzilsa yoki
            // yo'l noto'g'ri bo'lsa u ham bo'sh qaytaradi va abadiy
            // yashil turadi — hech narsani tekshirmasdan.
            Map<String, String> types = idTypes(CMS);

            assertThat(types).hasSizeGreaterThan(30);
            assertThat(types).containsEntry("Content", "Long");
        }
    }

    @Nested
    @DisplayName("Eski modul tegilmagan")
    class LegacyUntouched {

        /**
         * Eski casting modulining ID turlari — MUZLATILGAN.
         *
         * ⚠️ Bularni o'zgartirish barcha FK ustunlarini qayta yozishni
         * talab qilardi. Buyurtmachi talabi (§4): eski casting moduli
         * regressiyaga uchramasin.
         */
        private static final Map<String, String> FROZEN = Map.of(
                "User", "UUID",
                "CastingUser", "Integer",
                "News", "Integer",
                "Message", "Integer",
                "Role", "int",
                "Attachment", "UUID");

        @Test
        @DisplayName("⚠️ Eski entity'larning ID turi o'zgarmagan")
        void legacyIdTypesAreUnchanged() throws IOException {
            Map<String, String> actual = idTypes(LEGACY);
            List<String> changed = new ArrayList<>();

            FROZEN.forEach((entity, type) -> {
                String found = actual.get(entity);
                if (found != null && !found.equals(type)) {
                    changed.add(entity + ": " + type + " -> " + found);
                }
            });

            assertThat(changed)
                    .as("ТЗ §57: «hamma narsani bir kunda UUIDga rewrite "
                            + "qilma». ID turini o'zgartirish barcha FK "
                            + "ustunlarini qayta yozishni talab qiladi va "
                            + "mavjud ma'lumot bilan bog'liq xavf foydadan "
                            + "katta.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Yangi modulda eski ID turlari ISHLATILMAYDI")
        void newModuleDoesNotAdoptLegacyTypes() throws IOException {
            List<String> offenders = idTypes(CMS).entrySet().stream()
                    .filter(e -> e.getValue().equals("Integer") || e.getValue().equals("int"))
                    .map(e -> e.getKey() + " -> " + e.getValue())
                    .toList();

            // Eski modulda `Integer` bor, lekin u MERO — yangi kodga
            // ko'chirilmaydi. `Integer` 2 milliardda tugaydi va
            // analitika hodisalari bu chegaraga yetishi mumkin.
            assertThat(offenders).isEmpty();
        }
    }

    @Nested
    @DisplayName("Chegara aniq belgilangan")
    class Boundary {

        @Test
        @DisplayName("Foydalanuvchi ID si hamma joyda `UUID`")
        void userIdIsAlwaysUuid() throws IOException {
            Path src = Path.of("src/main/java/com/example/backend");
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(src)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String text = Files.readString(f);
                    // `Long userId` — chegarada tur o'girilib ketgan holat.
                    if (text.contains("Long userId") || text.contains("Long actorId")
                            || text.contains("Long senderId")) {
                        offenders.add(f.getFileName().toString());
                    }
                }
            }

            assertThat(offenders)
                    .as("Foydalanuvchi ID si `UUID`. `Long` sifatida saqlansa "
                            + "chegarada tur o'girish kerak bo'lardi va u "
                            + "ertami-kechmi xato beradi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("⚠️ Kasting ID si chegarada ANIQ o'giriladi")
        void castingIdConversionIsExplicit() throws IOException {
            String validator = Files.readString(Path.of(
                    "src/main/java/com/example/backend/Cms/Service/InternalLinkValidator.java"));

            // Havola nishoni `Long`, eski `CastingUser` esa `Integer`.
            // O'girish YASHIRIN bo'lmasligi kerak: `Long` `Integer` ga
            // sig'masligi mumkin va bu jimgina noto'g'ri natija berardi.
            assertThat(validator)
                    .as("Tur o'girish aniq va chegara tekshiruvi bilan bo'lsin")
                    .contains("id <= Integer.MAX_VALUE")
                    .contains("id.intValue()");
        }
    }

    // ------------------------------------------------- dublikat yo'qligi

    @Nested
    @DisplayName("Dublikat model yo'q (ТЗ §89)")
    class NoDuplicates {

        /**
         * ⚠️ Ikkita entity bitta jadvalga bog'lansa, ular bir-birining
         * yozuvini bilmasdan yozadi: birida qo'yilgan cheklov ikkinchisi
         * orqali chetlab o'tiladi va kesh ham ikkiga bo'linadi.
         *
         * Bu odatda «mavjudini kengaytirish qiyin edi, yangisini
         * yozdim» degan qarordan kelib chiqadi.
         */
        @Test
        @DisplayName("Bitta jadvalga ikkita entity bog'lanmagan")
        void noTwoEntitiesShareATable() throws java.io.IOException {
            java.util.Map<String, java.util.List<String>> byTable = new java.util.HashMap<>();

            for (java.nio.file.Path f : entitySources()) {
                String src = java.nio.file.Files.readString(f);
                Matcher m = Pattern.compile("@Table\\(\\s*name\\s*=\\s*\"([a-z_]+)\"")
                        .matcher(src);
                if (m.find()) {
                    byTable.computeIfAbsent(m.group(1), k -> new java.util.ArrayList<>())
                            .add(f.getFileName().toString());
                }
            }

            java.util.List<String> shared = byTable.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .map(e -> e.getKey() + " → " + e.getValue())
                    .toList();

            assertThat(shared).as("har bir jadval bitta entity'ga tegishli bo'lsin")
                    .isEmpty();
        }

        /**
         * ТЗ §89 sanagan tushunchalar. Har biri uchun BITTA entity
         * bo'lishi kerak.
         *
         * ⚠️ {@code Movie} va {@code Series} ataylab yo'q: ular
         * {@code Content} ning turi ({@code ContentType}). Ularni
         * alohida entity qilish aynan §89 ogohlantirgan dublikat
         * bo'lardi — bir xil maydonlar ikki joyda yashardi.
         */
        @Test
        @DisplayName("Asosiy tushunchalar takrorlanmagan")
        void coreConceptsAreNotDuplicated() throws java.io.IOException {
            java.util.List<String> names = entitySources().stream()
                    .map(f -> f.getFileName().toString().replace(".java", ""))
                    .toList();

            for (String concept : java.util.List.of(
                    "User", "Role", "Content", "Subscription", "Notification")) {
                assertThat(names.stream().filter(n -> n.equals(concept)).count())
                        .as(concept + " uchun aniq bitta entity bo'lsin")
                        .isEqualTo(1);
            }

            assertThat(names)
                    .as("Movie va Series - Content turi, alohida entity emas")
                    .doesNotContain("Movie", "Series");
        }

        @Test
        @DisplayName("Detektor haqiqatan entity'larni topadi")
        void detectorFindsEntities() throws java.io.IOException {
            assertThat(entitySources()).hasSizeGreaterThan(40);
        }

        private java.util.List<java.nio.file.Path> entitySources() throws java.io.IOException {
            try (java.util.stream.Stream<java.nio.file.Path> s =
                         java.nio.file.Files.walk(
                                 java.nio.file.Path.of("src/main/java/com/example/backend"))) {
                java.util.List<java.nio.file.Path> all = s
                        .filter(p -> p.toString().endsWith(".java")).toList();
                java.util.List<java.nio.file.Path> out = new java.util.ArrayList<>();
                for (java.nio.file.Path p : all) {
                    if (java.nio.file.Files.readString(p).contains("\n@Entity")) {
                        out.add(p);
                    }
                }
                return out;
            }
        }
    }
}
