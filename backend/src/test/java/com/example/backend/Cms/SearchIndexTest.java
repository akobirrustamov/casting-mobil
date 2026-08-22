package com.example.backend.Cms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §55 — qidiruv va indekslar.
 *
 * <h2>Nima uchun bu test kerak</h2>
 * V21 migratsiyasi PostgreSQL'da trigram indekslarini yaratadi va H2 da
 * o'tkazib yuboradi. Ya'ni ustun nomidagi XATO dev va testda umuman
 * sezilmaydi — u faqat ishlab chiqarishga deploy qilinganda, ilova
 * ishga tushayotganda chiqadi.
 *
 * O'shanda migratsiya yiqiladi va ilova umuman ko'tarilmaydi.
 *
 * Shuning uchun test migratsiya ro'yxatini HAQIQIY sxema bilan
 * solishtiradi.
 */
@SpringBootTest
@ActiveProfiles("test")
class SearchIndexTest {

    private static final Path MIGRATION = Path.of(
            "src/main/java/db/migration/V21__Search_indexes.java");
    private static final Path REPOS = Path.of(
            "src/main/java/com/example/backend");

    @Autowired private DataSource dataSource;

    /** V21 dagi {@code new String[]{"jadval", "ustun"}} juftliklari. */
    private List<String[]> declaredTargets() throws IOException {
        String src = Files.readString(MIGRATION);
        Pattern p = Pattern.compile("new String\\[]\\{\"(\\w+)\", \"(\\w+)\"}");
        Matcher m = p.matcher(src);
        List<String[]> out = new ArrayList<>();
        while (m.find()) {
            out.add(new String[]{m.group(1), m.group(2)});
        }
        return out;
    }

    private boolean columnExists(Connection c, String table, String column) throws Exception {
        try (ResultSet rs = c.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    @Nested
    @DisplayName("Migratsiya sxemaga mos")
    class SchemaMatches {

        @Test
        @DisplayName("⚠️ Har bir jadval va ustun HAQIQATDAN mavjud")
        void everyIndexedColumnExists() throws Exception {
            List<String[]> targets = declaredTargets();
            assertThat(targets)
                    .as("Migratsiyadan ustunlar o'qilmadi — naqsh buzilgan")
                    .isNotEmpty();

            List<String> missing = new ArrayList<>();
            try (Connection c = dataSource.getConnection()) {
                for (String[] t : targets) {
                    String table = t[0];
                    String column = t[1];
                    // ⚠️ Nom registri bazaga bog'liq: H2 odatda katta
                    // harfda saqlaydi, lekin bu loyihada
                    // `DATABASE_TO_LOWER=TRUE` — ya'ni kichik harfda.
                    // PostgreSQL ham kichik harfda saqlaydi.
                    //
                    // Ikkala variant ham sinaladi: test baza sozlamasiga
                    // bog'lanib qolmasin.
                    boolean found = columnExists(c, table, column)
                            || columnExists(c, table.toUpperCase(), column.toUpperCase());
                    if (!found) {
                        missing.add(table + "." + column);
                    }
                }
            }

            assertThat(missing)
                    .as("V21 mavjud bo'lmagan ustunga indeks qurmoqchi. "
                            + "H2 da migratsiya o'tkazib yuboriladi, ya'ni xato "
                            + "faqat ISHLAB CHIQARISHDA — ilova ishga tushayotganda "
                            + "chiqadi va u umuman ko'tarilmaydi.")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Qidiriladigan ustunlar qamrovi")
    class Coverage {

        /** Repozitoriylarda {@code like lower(concat(...))} qidiruvlari. */
        private Set<String> searchedFields() throws IOException {
            Set<String> fields = new LinkedHashSet<>();
            Pattern p = Pattern.compile("lower\\((\\w+)\\.(\\w+)\\)\\s*like");

            try (Stream<Path> files = Files.walk(REPOS)) {
                for (Path f : files.filter(x -> x.toString().endsWith("Repo.java")).toList()) {
                    Matcher m = p.matcher(Files.readString(f));
                    while (m.find()) {
                        fields.add(m.group(2));
                    }
                }
            }
            return fields;
        }

        @Test
        @DisplayName("⚠️ Har bir qidiriladigan maydon indekslangan")
        void everySearchedFieldIsIndexed() throws Exception {
            Set<String> searched = searchedFields();
            assertThat(searched)
                    .as("Qidiruv so'rovlari topilmadi — detektor noto'g'ri joyga qarayapti")
                    .isNotEmpty();

            // Migratsiyadagi ustunlar snake_case, JPQL dagi maydonlar camelCase.
            Set<String> indexed = new LinkedHashSet<>();
            for (String[] t : declaredTargets()) {
                indexed.add(toCamel(t[1]));
            }

            List<String> notIndexed = searched.stream()
                    .filter(f -> !indexed.contains(f))
                    .toList();

            assertThat(notIndexed)
                    .as("Qidiriladigan maydon uchun indeks yo'q. `like '%%q%%'` "
                            + "B-tree indeksini ISHLATA OLMAYDI — indekssiz baza "
                            + "har qidiruvda butun jadvalni skanerlaydi.")
                    .isEmpty();
        }

        private String toCamel(String snake) {
            StringBuilder sb = new StringBuilder();
            boolean up = false;
            for (char ch : snake.toCharArray()) {
                if (ch == '_') {
                    up = true;
                } else {
                    sb.append(up ? Character.toUpperCase(ch) : ch);
                    up = false;
                }
            }
            return sb.toString();
        }
    }

    @Nested
    @DisplayName("Portativlik")
    class Portability {

        @Test
        @DisplayName("H2 da migratsiya o'tkazib yuboriladi")
        void migrationSkipsOnH2() throws IOException {
            String src = Files.readString(MIGRATION);

            // `pg_trgm` — PostgreSQL kengaytmasi. H2 da u yo'q va oddiy
            // SQL migratsiya dev hamda testda yiqilardi.
            assertThat(src).contains("pg_trgm");
            assertThat(src)
                    .as("Baza turi tekshirilishi shart")
                    .contains("getDatabaseProductName");
        }

        @Test
        @DisplayName("Indeks `lower(...)` bo'yicha — so'rov bilan bir xil")
        void indexMatchesTheQueryExpression() throws IOException {
            String src = Files.readString(MIGRATION);

            // So'rov `lower(x) like lower(...)` qiladi. Indeks oddiy
            // ustun bo'yicha qurilsa, u ISHLATILMASDI — ifoda mos
            // kelmaydi.
            assertThat(src).contains("gin (lower(");
        }
    }
}
