package com.example.backend;

import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Entity.CastingUser;
import com.example.backend.Entity.Message;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Repository.CastingUserRepo;
import com.example.backend.Repository.MessageRepo;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ESKI CASTING MODULI — XATTI-HARAKAT REGRESSIYASI.
 *
 * <h2>Nega bu {@code OldCastingFrozenTest} dan boshqa</h2>
 * U yo'llar va fayllar O'ZGARMAGANINI tekshiradi. Lekin yo'l joyida turib
 * ham ICHKI mantiq buzilishi mumkin: masalan kimdir {@code MessageRepo} ni
 * refaktor qilib, bot uchun xabar yozilmay qolsa — yo'l o'sha-o'sha, endpoint
 * 200 qaytaradi, ammo nomzod hech qachon javob olmaydi.
 *
 * Bu yerda AYNAN shu oqim ishlashini tekshiramiz.
 *
 * <h2>Casting oqimi</h2>
 * <ol>
 *   <li>Telegram bot foydalanuvchi anketasini <b>tokensiz</b> yuboradi;</li>
 *   <li>admin uni qabul qiladi yoki rad etadi;</li>
 *   <li>shu paytda bot uchun {@code message} jadvaliga xabar yoziladi;</li>
 *   <li>bot o'sha xabarni olib nomzodga yuboradi.</li>
 * </ol>
 *
 * Uchinchi qadam yo'qolsa, buzilish faqat foydalanuvchida ko'rinadi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
class ExistingCastingRegressionTest {

    private static final String TELEGRAM_ID = "770000001";

    @Autowired private MockMvc mockMvc;
    @Autowired private CastingUserRepo castingUserRepo;
    @Autowired private MessageRepo messageRepo;
    @Autowired private TestStaffFactory staff;

    private String token;

    @BeforeEach
    void adminToken() {
        // Eski endpoint ruxsat tekshirmaydi, faqat token talab qiladi.
        token = staff.tokenForRole("+998900000401", PlatformRole.ADMIN,
                EnumSet.noneOf(Permission.class));
    }

    /** Bot yuboradigan anketa — eng kam maydonlar bilan. */
    private String applicationJson(String telegramId, String name) {
        return """
                {
                  "telegramId": "%s",
                  "castingType": "model",
                  "gender": "female",
                  "name": "%s",
                  "region": "Toshkent",
                  "nationality": "uzbek",
                  "age": 24,
                  "height": 175,
                  "phone": "+998901234567",
                  "photos": []
                }
                """.formatted(telegramId, name);
    }

    private CastingUser apply(String telegramId, String name) throws Exception {
        mockMvc.perform(post("/api/v1/casting-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson(telegramId, name)))
                .andExpect(status().isCreated());

        return castingUserRepo.findAll().stream()
                .filter(u -> telegramId.equals(u.getTelegramId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Anketa saqlanmadi"));
    }

    // ------------------------------------------------------------ anketa

    @Nested
    @DisplayName("Anketa yuborish (bot oqimi)")
    class Application {

        @Test
        @DisplayName("Bot anketani TOKENSIZ yubora oladi")
        void botCanSubmitWithoutToken() throws Exception {
            CastingUser saved = apply(TELEGRAM_ID + "1", "Bot orqali anketa");

            // ⚠️ Bu endpoint ataylab ochiq: bot foydalanuvchisi tizimga
            // kirmaydi. Yopilsa butun anketa oqimi to'xtaydi.
            assertThat(saved.getName()).isEqualTo("Bot orqali anketa");
            assertThat(saved.getStatus()).isZero();
            assertThat(saved.getIsWebShow()).isFalse();
        }

        @Test
        @DisplayName("Nomzod o'z anketasini telegramId bo'yicha ko'radi")
        void applicantCanReadOwnApplication() throws Exception {
            String tg = TELEGRAM_ID + "2";
            apply(tg, "O'z anketasi");

            mockMvc.perform(get("/api/v1/casting-user/my/" + tg))
                    .andExpect(status().isOk());
        }
    }

    // ------------------------------------------------------- bot xabari

    @Nested
    @DisplayName("Qabul / rad etish — bot uchun xabar")
    class BotNotification {

        @Test
        @DisplayName("Qabul qilinganda ijobiy xabar yoziladi")
        void approvalWritesPositiveMessage() throws Exception {
            String tg = TELEGRAM_ID + "3";
            CastingUser user = apply(tg, "Qabul qilinadigan");

            mockMvc.perform(put("/api/v1/casting-user/status/"
                            + user.getId() + "/1/500000")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            Optional<Message> message = messageRepo.findByCastingUserId(user.getId());
            assertThat(message)
                    .as("Qabul qilingach bot uchun xabar yozilishi SHART - "
                            + "aks holda nomzod hech qachon javob olmaydi")
                    .isPresent();
            assertThat(message.get().getStatus()).isTrue();
            assertThat(message.get().getPrice()).isEqualTo("500000");
            assertThat(message.get().getName()).isEqualTo("Qabul qilinadigan");
            // Bot xabarni shu id bo'yicha yuboradi.
            assertThat(message.get().getTelegramId()).isNotNull();
            // Tur nomzodga tushunarli tilda tarjima qilinadi.
            assertThat(message.get().getCastingType()).isEqualTo("Model");
        }

        @Test
        @DisplayName("Rad etilganda salbiy xabar yoziladi")
        void rejectionWritesNegativeMessage() throws Exception {
            String tg = TELEGRAM_ID + "4";
            CastingUser user = apply(tg, "Rad etiladigan");

            mockMvc.perform(put("/api/v1/casting-user/status/"
                            + user.getId() + "/2/0")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            Optional<Message> message = messageRepo.findByCastingUserId(user.getId());
            assertThat(message).isPresent();
            assertThat(message.get().getStatus()).isFalse();
        }

        @Test
        @DisplayName("Qayta o'zgartirilsa xabar YANGILANADI, ikkinchisi yaratilmaydi")
        void statusChangeUpdatesExistingMessage() throws Exception {
            String tg = TELEGRAM_ID + "5";
            CastingUser user = apply(tg, "Ikki marta");

            mockMvc.perform(put("/api/v1/casting-user/status/"
                            + user.getId() + "/2/0")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
            mockMvc.perform(put("/api/v1/casting-user/status/"
                            + user.getId() + "/1/300000")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            long count = messageRepo.findAll().stream()
                    .filter(m -> m.getCastingUser() != null
                            && user.getId().equals(m.getCastingUser().getId()))
                    .count();

            // Ikkita xabar bo'lsa bot nomzodga qarama-qarshi ikki javob yuborardi.
            assertThat(count).isEqualTo(1);

            Optional<Message> message = messageRepo.findByCastingUserId(user.getId());
            assertThat(message).isPresent();
            assertThat(message.get().getStatus()).isTrue();
            assertThat(message.get().getPrice()).isEqualTo("300000");
        }

        @Test
        @DisplayName("Status o'zgarishi TOKENSIZ mumkin emas")
        void statusChangeRequiresToken() throws Exception {
            String tg = TELEGRAM_ID + "6";
            CastingUser user = apply(tg, "Himoyalangan");

            mockMvc.perform(put("/api/v1/casting-user/status/"
                            + user.getId() + "/1/100"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------- ochiq katalog

    @Nested
    @DisplayName("Ochiq katalog")
    class PublicCatalog {

        @Test
        @DisplayName("Saytga chiqarilgan anketalar tokensiz ko'rinadi")
        void webCatalogIsPublic() throws Exception {
            String tg = TELEGRAM_ID + "7";
            CastingUser user = apply(tg, "Saytda ko'rinadigan");
            user.setIsWebShow(Boolean.TRUE);
            user.setCreatedAt(LocalDateTime.now());
            castingUserRepo.save(user);

            mockMvc.perform(get("/api/v1/casting-user/web"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
