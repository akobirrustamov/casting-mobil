package com.example.backend.Admin;

import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ТЗ §52 — validatsiya va xato formati.
 *
 * <h2>Asosiy talab</h2>
 * «Faqat frontend validationga tayanma.» Ya'ni klient tekshiruvini
 * chetlab o'tish mumkin — to'g'ridan-to'g'ri so'rov yuborish, eski
 * klient, yoki shunchaki brauzer konsoli — va backend baribir rad
 * etishi kerak.
 *
 * <h2>Nima uchun `errors[]` muhim</h2>
 * Xato maydon YONIDA ko'rsatilishi kerak. Buning uchun backend qaysi
 * maydon noto'g'ri ekanini AYTISHI shart. Faqat umumiy xabar bo'lsa,
 * o'nlab maydonli formada foydalanuvchi ularni birma-bir sinab
 * ko'rishga majbur bo'lardi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
@Transactional
class ValidationFormatTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestStaffFactory staff;

    private String token;

    private String token() {
        if (token == null) {
            token = staff.tokenForRole("+998900007001", PlatformRole.ADMIN,
                    EnumSet.of(Permission.CATEGORY_CREATE, Permission.CATEGORY_VIEW));
        }
        return token;
    }

    private JsonNode postInvalid(String url, Object body) throws Exception {
        String response = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    // ---------------------------------------------------------------- format

    @Nested
    @DisplayName("Xato formati")
    class Format {

        @Test
        @DisplayName("ТЗ §52 dagi uchala maydon ham bor")
        void errorHasCodeMessageAndErrors() throws Exception {
            // Bo'sh tana: majburiy maydonlar yo'q.
            JsonNode error = postInvalid("/api/v1/app/admin/categories", Map.of());

            assertThat(error.has("code")).isTrue();
            assertThat(error.has("message")).isTrue();
            assertThat(error.has("errors")).isTrue();
            assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("⚠️ `errors[]` AYNAN qaysi maydon ekanini aytadi")
        void errorsNameTheField() throws Exception {
            JsonNode error = postInvalid("/api/v1/app/admin/categories", Map.of());

            assertThat(error.get("errors").isArray()).isTrue();
            assertThat(error.get("errors")).isNotEmpty();

            // Har bir element `{field, message}` — usiz frontend xatoni
            // maydon yoniga qo'ya olmasdi.
            JsonNode first = error.get("errors").get(0);
            assertThat(first.has("field")).isTrue();
            assertThat(first.has("message")).isTrue();
            assertThat(first.get("field").asText()).isNotBlank();
            assertThat(first.get("message").asText()).isNotBlank();
        }

        @Test
        @DisplayName("Xabar tushunarli — bo'sh yoki texnik emas")
        void messageIsHumanReadable() throws Exception {
            JsonNode error = postInvalid("/api/v1/app/admin/categories", Map.of());
            String message = error.get("errors").get(0).get("message").asText();

            // «must not be null» kabi standart texnik matn emas: DTO'larda
            // o'zbekcha xabarlar yozilgan.
            assertThat(message)
                    .isNotBlank()
                    .doesNotContain("must not be");
        }
    }

    // ------------------------------------------------- klientni chetlab o'tish

    @Nested
    @DisplayName("Klient tekshiruvini chetlab o'tib bo'lmaydi")
    class ServerSideEnforcement {

        @Test
        @DisplayName("⚠️ Backend o'zi ham tekshiradi")
        void backendValidatesIndependently() throws Exception {
            // Bu so'rov panelning o'zidan EMAS — to'g'ridan-to'g'ri
            // yuborilgan. Klient tekshiruvi bu yerda umuman ishlamaydi.
            mockMvc.perform(post("/api/v1/app/admin/categories")
                            .header("Authorization", "Bearer " + token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"translations\":{}}"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ---------------------------------------------------------------- panel

    @Nested
    @DisplayName("Panel maydon xatolarini ishlatadi")
    class PanelUsesFieldErrors {

        private static final Path PANEL = Path.of("../frontend/src/adminpanel");

        @Test
        @DisplayName("⚠️ Formalar `errors[]` ni O'QIYDI")
        void formsConsumeFieldErrors() throws IOException {
            // Backend maydon xatolarini qaytaradi, lekin ilgari ularni
            // FAQAT LoginPage ishlatardi. Qolgan formalar umumiy xabar
            // ko'rsatardi va foydalanuvchi qaysi maydonni tuzatishni
            // bilmasdi.
            for (String form : java.util.List.of(
                    "pages/TaxonomyForm.jsx",
                    "pages/CreatorForm.jsx",
                    "pages/ContentEditor.jsx",
                    "pages/LoginPage.jsx")) {

                String src = Files.readString(PANEL.resolve(form));
                assertThat(src)
                        .as(form + " backend maydon xatolarini ishlatmaydi")
                        .containsAnyOf("useFieldErrors", "err.errors");
            }
        }

        @Test
        @DisplayName("Xato uchun alohida uslub bor")
        void fieldErrorStyleExists() throws IOException {
            String css = Files.readString(PANEL.resolve("theme/panel.css"));

            assertThat(css)
                    .as("Maydon xatosi ko'zga tashlanishi kerak")
                    .contains(".uz-field-error");
        }
    }
}
