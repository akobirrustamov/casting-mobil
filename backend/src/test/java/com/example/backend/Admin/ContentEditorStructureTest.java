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
 * ТЗ §53 — kontent muharriri.
 *
 * <h2>«Giant component» nima uchun muammo</h2>
 * ТЗ aniq yozgan: «Content form 100 ta inputli bitta giant component
 * bo'lmasin. Componentlarga ajrat.»
 *
 * Sabab shakl emas, oqibat: bitta faylda o'nlab maydon va yetti bo'lim
 * bo'lsa — bitta bo'limdagi holat o'zgarishi HAMMASINI qayta chizadi,
 * o'zgartirish kiritish uchun butun faylni o'qib chiqish kerak bo'ladi
 * va ikki kishi bir vaqtda ishlasa har safar konflikt chiqadi.
 */
class ContentEditorStructureTest {

    private static final Path PANEL = Path.of("../frontend/src/adminpanel");
    private static final Path EDITOR = PANEL.resolve("pages/ContentEditor.jsx");
    private static final Path TABS = PANEL.resolve("pages/editor");

    /**
     * Bitta fayl uchun oqilona chegara.
     *
     * ⚠️ Bu aniq raqam emas, TENDENSIYA nazorati: muharrir yana
     * o'sib ketmasin. Chegaraga yaqinlashsa — yangi bo'limni alohida
     * komponentga chiqarish kerak, chegarani oshirish emas.
     */
    private static final int MAX_LINES = 400;

    @Nested
    @DisplayName("Komponentlarga ajratilgan")
    class Split {

        @Test
        @DisplayName("⚠️ Muharrir bitta giant fayl EMAS")
        void editorIsNotOneGiantFile() throws IOException {
            long lines = Files.readAllLines(EDITOR).size();

            assertThat(lines)
                    .as("ТЗ §53: muharrir komponentlarga ajratilishi kerak. "
                            + "Chegaraga yetganda yangi bo'limni alohida faylga "
                            + "chiqaring — chegarani oshirmang.")
                    .isLessThanOrEqualTo(MAX_LINES);
        }

        @Test
        @DisplayName("Har bir bo'lim alohida faylda")
        void everyTabHasItsOwnFile() throws IOException {
            for (String tab : List.of("BasicTab", "TextTab", "MediaTab",
                    "CreditsTab", "AccessTab", "PublishTab")) {
                assertThat(Files.isRegularFile(TABS.resolve(tab + ".jsx")))
                        .as("Bo'lim komponenti yo'q: " + tab)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Ro'yxatlar BITTA joyda — nusxa yo'q")
        void constantsAreNotDuplicated() throws IOException {
            // Ilgari ular muharrirning o'zida edi. Bo'limlar ajratilganda
            // har biriga nusxa ko'chirilishi kerak bo'lardi — nusxa esa
            // vaqt o'tib chetga chiqadi: yangi kontent turi bittasida
            // paydo bo'lib, ikkinchisida yo'q bo'lardi.
            assertThat(Files.isRegularFile(TABS.resolve("constants.js"))).isTrue();

            List<String> offenders = new ArrayList<>();
            try (Stream<Path> files = Files.walk(TABS)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".jsx")).toList()) {
                    String src = Files.readString(file);
                    if (src.contains("const TYPES = [") || src.contains("const STATUSES = [")) {
                        offenders.add(file.getFileName().toString());
                    }
                }
            }
            assertThat(offenders).as("Ro'yxat nusxalangan").isEmpty();
        }
    }

    @Nested
    @DisplayName("ТЗ §53 talablari")
    class Requirements {

        private String allEditorSources() throws IOException {
            StringBuilder all = new StringBuilder(Files.readString(EDITOR));
            try (Stream<Path> files = Files.walk(TABS)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".jsx")).toList()) {
                    all.append(Files.readString(f));
                }
            }
            return all.toString();
        }

        @Test
        @DisplayName("Bo'limlar, saqlanmagan o'zgarish, oldindan ko'rish")
        void coreRequirementsArePresent() throws IOException {
            String src = allEditorSources();

            assertThat(src).as("bo'limlar").contains("TABS");
            assertThat(src).as("saqlanmagan o'zgarish").contains("dirty");
            assertThat(src).as("rasm oldindan ko'rish").contains("MediaField");
            assertThat(src).as("galereya va tartib").contains("GalleryField");
        }

        @Test
        @DisplayName("Kategoriya va ijodkor QIDIRUV bilan tanlanadi")
        void categoryAndCreatorAreSearchable() throws IOException {
            String basic = Files.readString(TABS.resolve("BasicTab.jsx"));
            String credits = Files.readString(TABS.resolve("CreditsTab.jsx"));

            // Ro'yxatlar cheklanmagan: oddiy `<select>` da kerakligini
            // topish uchun butun ro'yxatni aylantirish kerak bo'lardi.
            assertThat(basic).as("kategoriya qidiruvi").contains("SearchableSelect");
            assertThat(credits).as("ijodkor qidiruvi").contains("creatorQuery");
        }

        @Test
        @DisplayName("⚠️ Rejalashtirish uchun NASHR SANASI bor")
        void schedulingHasAPublicationDate() throws IOException {
            String publish = Files.readString(TABS.resolve("PublishTab.jsx"));
            String editor = Files.readString(EDITOR);

            // Ilgari panelda faqat premyera sanasi bor edi: admin
            // SCHEDULED holatini tanlay olardi, lekin kontent QACHON
            // chiqishini belgilay olmasdi. Maydon backend DTO'sida bor
            // edi — ya'ni rejalashtirish amalda ishlamasdi.
            assertThat(publish).contains("publicationDate");
            assertThat(editor)
                    .as("Sana serverga yuborilishi ham kerak")
                    .contains("publicationDate");
        }

        @Test
        @DisplayName("Janr KO'P TANLOVLI")
        void genresAreMultiSelect() throws IOException {
            String basic = Files.readString(TABS.resolve("BasicTab.jsx"));

            // Bitta kontentda bir nechta janr bo'ladi (Drama + Romantika).
            assertThat(basic).contains("genreIds");
            assertThat(basic).contains("uz-chip");
        }
    }
}
