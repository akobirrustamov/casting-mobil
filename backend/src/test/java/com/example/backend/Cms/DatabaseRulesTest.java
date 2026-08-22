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
 * ТЗ §56 — baza qoidalari.
 *
 * <h2>Nima uchun migratsiyalar matn sifatida o'qiladi</h2>
 * Bu qoidalar SXEMANI emas, MIGRATSIYA TARIXINI himoya qiladi. Bazaga
 * qarab «hozir hammasi joyida» degan xulosa chiqarish mumkin, lekin
 * ma'lumot allaqachon yo'qolgan bo'lishi mumkin.
 *
 * Masalan `drop table` migratsiyasi ishlab chiqarishda bir marta
 * bajariladi va shundan keyin sxema yana «to'g'ri» ko'rinadi —
 * jadvaldagi ma'lumot esa qaytmaydi.
 */
class DatabaseRulesTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    private String allSql() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".sql")).sorted().toList()) {
                sb.append(Files.readString(f)).append('\n');
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------------- buzuvchi ish

    @Nested
    @DisplayName("Ma'lumot saqlanadi")
    class DataPreserved {

        /**
         * Ma'lumotni QAYTARIB BO'LMAYDIGAN tarzda yo'qotadigan buyruqlar.
         *
         * ⚠️ `drop index` bu ro'yxatda YO'Q: indeks ma'lumot emas, uni
         * bitta buyruq bilan qayta yaratish mumkin.
         */
        private static final List<String> DESTRUCTIVE = List.of(
                "drop table",
                "drop database",
                "drop schema",
                "truncate",
                "delete from",
                "drop column");

        @Test
        @DisplayName("⚠️ Migratsiyalarda ma'lumot o'chiruvchi buyruq YO'Q")
        void noDestructiveStatements() throws IOException {
            String sql = allSql().toLowerCase();
            List<String> found = DESTRUCTIVE.stream()
                    .filter(sql::contains)
                    .toList();

            assertThat(found)
                    .as("Buyurtmachi talabi: «Production database'ni drop "
                            + "qiladigan migration yozma. Existing data "
                            + "saqlansin.» Bunday migratsiya ishlab chiqarishda "
                            + "BIR MARTA bajariladi va ma'lumot qaytmaydi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Ijobiy nazorat: detektor ishlayapti")
        void detectorWorks() {
            assertThat("alter table x drop column y".contains("drop column")).isTrue();
        }
    }

    // ------------------------------------------------------- ortiqcha indeks

    @Nested
    @DisplayName("Sababsiz indeks yo'q")
    class NoRedundantIndexes {

        /** {@code create index NOM on JADVAL (ustunlar)}. */
        private Map<String, List<String>> indexes(String sql) {
            Map<String, List<String>> out = new LinkedHashMap<>();
            Matcher m = Pattern.compile(
                    "create (?:unique )?index (?:if not exists )?(\\w+) on (\\w+)\\s*\\(([^)]*)\\)",
                    Pattern.CASE_INSENSITIVE).matcher(sql);
            while (m.find()) {
                out.put(m.group(1).toLowerCase(),
                        List.of(m.group(2).toLowerCase(), m.group(3).toLowerCase()));
            }
            return out;
        }

        @Test
        @DisplayName("⚠️ Kompozit kalit qamragan ustunga alohida indeks yo'q")
        void noIndexDuplicatingACompositeKeyPrefix() throws IOException {
            String sql = allSql();

            // Kompozit birlamchi kalitlar: `primary key (a, b)`.
            Map<String, String> pkLead = new LinkedHashMap<>();
            Matcher tables = Pattern.compile(
                    "create table (\\w+)\\s*\\(([^;]*)\\);",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(sql);
            while (tables.find()) {
                Matcher pk = Pattern.compile("primary key \\(([^)]*)\\)",
                        Pattern.CASE_INSENSITIVE).matcher(tables.group(2));
                if (pk.find() && pk.group(1).contains(",")) {
                    pkLead.put(tables.group(1).toLowerCase(),
                            pk.group(1).split(",")[0].trim().toLowerCase());
                }
            }

            List<String> redundant = new ArrayList<>();
            indexes(sql).forEach((name, spec) -> {
                String table = spec.get(0);
                String first = spec.get(1).split(",")[0].trim();
                if (first.equals(pkLead.get(table))) {
                    redundant.add(name + " (" + table + "." + first + ")");
                }
            });

            assertThat(redundant)
                    .as("Kompozit birlamchi kalit o'zining BIRINCHI ustuni "
                            + "bo'yicha so'rovlarga allaqachon xizmat qiladi "
                            + "(prefiks qoidasi). Qo'shimcha indeks faqat yozuv "
                            + "tezligini sekinlashtiradi — ТЗ §56: «Sababsiz "
                            + "har bir fieldga index qo'yma».")
                    .isEmpty();
        }

        @Test
        @DisplayName("Indeks nomlari takrorlanmaydi")
        void indexNamesAreUnique() throws IOException {
            String sql = allSql();
            List<String> names = new ArrayList<>();
            Matcher m = Pattern.compile(
                    "create (?:unique )?index (?:if not exists )?(\\w+) on",
                    Pattern.CASE_INSENSITIVE).matcher(sql);
            while (m.find()) {
                names.add(m.group(1).toLowerCase());
            }

            assertThat(names)
                    .as("Bir xil nomli indeks migratsiyani yiqitardi")
                    .doesNotHaveDuplicates();
        }
    }

    // ----------------------------------------------------- talab qilinganlar

    @Nested
    @DisplayName("ТЗ §56 ro'yxatidagi ustunlar indekslangan")
    class RequiredIndexes {

        @Test
        @DisplayName("Sakkizala naqsh ham qamralgan")
        void everyListedPatternIsCovered() throws IOException {
            String sql = allSql().toLowerCase();

            Map<String, String> required = new LinkedHashMap<>();
            // slug · phone · email — `unique` cheklovi B-tree indeks yaratadi
            required.put("slug", "slug varchar(128) not null unique");
            required.put("phone", "phone varchar(255) unique");
            required.put("email", "email varchar(255) unique");
            // qolganlari aniq indekslar
            required.put("status", "idx_content_status");
            required.put("category", "idx_content_category");
            required.put("publication date", "idx_content_publication");
            required.put("premiere date", "idx_content_premiere");
            required.put("role", "idx_users_roles_role");
            required.put("content relations", "idx_credit_content");

            List<String> missing = required.entrySet().stream()
                    .filter(e -> !sql.contains(e.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();

            assertThat(missing)
                    .as("ТЗ §56 da sanalgan naqsh uchun indeks yo'q")
                    .isEmpty();
        }

        @Test
        @DisplayName("⚠️ `users_roles` indekslangan — eng issiq yo'l")
        void userRolesIsIndexed() throws IOException {
            String sql = allSql().toLowerCase();

            // `User.roles` EAGER: Spring Security uni HAR BIR
            // autentifikatsiyalangan so'rovda yuklaydi. Indekssiz bu
            // jadvalni har safar to'liq skanerlash degani.
            assertThat(sql).contains("idx_users_roles_user");
        }

        @Test
        @DisplayName("⚠️ Teskari tartibli tarjima cheklovlari qoplangan")
        void reversedTranslationConstraintsHaveAnIndex() throws IOException {
            String sql = allSql().toLowerCase();

            // Beshta jadvalda `unique (locale, parent_id)` — tartib
            // teskari. Unikallik to'g'ri ishlaydi, lekin
            // `where parent_id = ?` indeksdan foydalana olmaydi:
            // birinchi ustun `locale` va unda atigi uchta qiymat bor.
            for (String idx : List.of(
                    "idx_homepage_tr_section",
                    "idx_notification_tr_parent",
                    "idx_premiere_tr_parent",
                    "idx_season_tr_parent",
                    "idx_tariff_tr_parent")) {
                assertThat(sql).as(idx + " yo'q").contains(idx);
            }
        }
    }
}
