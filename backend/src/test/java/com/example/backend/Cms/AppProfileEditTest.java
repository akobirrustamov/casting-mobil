package com.example.backend.Cms;

import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Profilni tahrirlash — ism va til.
 *
 * <h2>⚠️ Qaysi bo'shliq yopilyapti</h2>
 * Ism faqat KIRISH oqimida yozilardi va keyin o'zgartirishning yo'li
 * yo'q edi. Til esa faqat telefonda saqlanardi:
 * {@code cms_user_account.language} hech qachon yangilanmasdi.
 *
 * Ikkinchisi ayniqsa jimgina yashiringan: push xabar aynan o'sha
 * maydondan til oladi, FCM esa hali ulanmagan — ya'ni xato faqat
 * bildirishnomalar yoqilgan kuni ko'rinardi va sababini topish
 * qiyin bo'lardi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppProfileEditTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String ME = "/api/v1/app/me";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private JwtService jwtService;

    @Nested
    @DisplayName("Ism")
    class Name {

        @Test
        @DisplayName("Yangi ism saqlanadi va javobda qaytadi")
        void nameIsSaved() throws Exception {
            User u = user("Eski Ism");

            mockMvc.perform(put(ME).header("Authorization", token(u))
                            .contentType("application/json")
                            .content("{\"name\":\"Yangi Ism\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Yangi Ism"));

            assertThat(userRepo.findById(u.getId()).orElseThrow().getName())
                    .isEqualTo("Yangi Ism");
        }

        /** Kirish oqimidagi bilan bir xil qoida — {@code PersonName}. */
        @Test
        @DisplayName("Ortiqcha bo'shliqlar tozalanadi")
        void nameIsTrimmed() throws Exception {
            User u = user("Eski");

            mockMvc.perform(put(ME).header("Authorization", token(u))
                            .contentType("application/json")
                            .content("{\"name\":\"  Ali   Valiyev  \"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Ali Valiyev"));
        }

        /**
         * ⚠️ Chegara kirish oqimi bilan BIR XIL bo'lishi shart. Aks holda
         * profilda qo'ygan ismi bilan keyingi kirishda «juda uzun» degan
         * xato chiqardi.
         */
        @Test
        @DisplayName("Juda qisqa ism rad etiladi")
        void tooShortRejected() throws Exception {
            mockMvc.perform(put(ME).header("Authorization", token(user("Ism")))
                            .contentType("application/json")
                            .content("{\"name\":\"A\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("NAME_INVALID"));
        }

        @Test
        @DisplayName("Juda uzun ism rad etiladi")
        void tooLongRejected() throws Exception {
            String long61 = "A".repeat(61);

            mockMvc.perform(put(ME).header("Authorization", token(user("Ism")))
                            .contentType("application/json")
                            .content("{\"name\":\"" + long61 + "\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("NAME_INVALID"));
        }
    }

    @Nested
    @DisplayName("Til")
    class Language {

        /**
         * ⚠️ Push xabar SHU maydondan til oladi. Ilova tilni faqat
         * telefonda saqlaganda ruscha so'zlashuvchi odam o'zbekcha
         * xabar olardi.
         */
        @Test
        @DisplayName("Til hisobga yoziladi")
        void languageIsSaved() throws Exception {
            User u = user("Ism");

            mockMvc.perform(put(ME).header("Authorization", token(u))
                            .contentType("application/json")
                            .content("{\"language\":\"RU\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.language").value("RU"));

            assertThat(accountRepo.findByUserId(u.getId()).orElseThrow().getLanguage())
                    .isEqualTo(Locale.RU);
        }

        /** Hisob yozuvi yo'q eski foydalanuvchida ham ishlashi kerak. */
        @Test
        @DisplayName("Hisob yozuvi bo'lmasa yaratiladi")
        void accountIsCreatedIfMissing() throws Exception {
            User u = user("Ism");
            assertThat(accountRepo.findByUserId(u.getId())).isEmpty();

            mockMvc.perform(put(ME).header("Authorization", token(u))
                            .contentType("application/json")
                            .content("{\"language\":\"EN\"}"))
                    .andExpect(status().isOk());

            assertThat(accountRepo.findByUserId(u.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("Qisman yangilash")
    class Partial {

        /**
         * ⚠️ Tilni almashtirish ismni qayta yozishni talab qilmasligi
         * kerak: ilova faqat o'zgarganini yuboradi.
         */
        @Test
        @DisplayName("Yuborilmagan maydon o'zgarmaydi")
        void missingFieldIsUntouched() throws Exception {
            User u = user("Asl Ism");
            accountRepo.save(UserAccount.builder()
                    .user(u).language(Locale.RU).createdAt(LocalDateTime.now()).build());

            mockMvc.perform(put(ME).header("Authorization", token(u))
                            .contentType("application/json")
                            .content("{\"name\":\"Boshqa Ism\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Boshqa Ism"))
                    // Til tegilmagan.
                    .andExpect(jsonPath("$.language").value("RU"));
        }

        @Test
        @DisplayName("Bo'sh tana hech narsani buzmaydi")
        void emptyBodyIsHarmless() throws Exception {
            User u = user("Asl Ism");

            mockMvc.perform(put(ME).header("Authorization", token(u))
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Asl Ism"));
        }

        @Test
        @DisplayName("Token yo'q bo'lsa 401")
        void anonymousRejected() throws Exception {
            mockMvc.perform(put(ME).contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ---------------------------------------------------------- yordamchi

    private User user(String name) {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99890" + (8700000 + n));
        u.setPassword("xesh-" + n);
        u.setName(name);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }
}
