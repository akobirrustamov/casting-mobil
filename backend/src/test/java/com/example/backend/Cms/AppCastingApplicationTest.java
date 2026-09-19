package com.example.backend.Cms;

import com.example.backend.Entity.Attachment;
import com.example.backend.Entity.CastingUser;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.AttachmentRepo;
import com.example.backend.Repository.CastingUserRepo;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mobil ilovadan casting anketasi ({@code /api/v1/app/casting}).
 *
 * <h2>Nima qo'riqlanadi</h2>
 * <ul>
 *   <li>anketa eski {@code casting_user} jadvaliga, eski admin sayti kutgan
 *       boshlang'ich qiymatlar bilan tushadi — admin uni bot anketasi bilan
 *       bir ro'yxatda ko'radi;</li>
 *   <li>holat va narxni klient qo'ya olmaydi;</li>
 *   <li>bir vaqtda bitta kutilayotgan ariza;</li>
 *   <li>«mening arizalarim» faqat O'Z qatorlarini qaytaradi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppCastingApplicationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String APPLICATIONS = "/api/v1/app/casting/applications";
    private static final String MY = "/api/v1/app/casting/applications/my";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CastingUserRepo castingUserRepo;
    @Autowired private AttachmentRepo attachmentRepo;

    // ------------------------------------------------------------- yordamchi

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99891" + (7700000 + SEQ.incrementAndGet()));
        u.setPassword(passwordEncoder.encode("Parol123!"));
        u.setName("Nomzod " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }

    /** Yuklangan rasmning o'rnini bosadi — `/file/upload` yozadigan qator. */
    private UUID photo() {
        UUID id = UUID.randomUUID();
        attachmentRepo.save(new Attachment(id, "/casting", id + "_rasm.jpg", true));
        return id;
    }

    private Map<String, Object> validBody(List<UUID> photos) {
        Map<String, Object> b = new HashMap<>();
        b.put("castingType", "model");
        b.put("gender", "female");
        b.put("name", "Malika");
        b.put("region", "Toshkent");
        b.put("nationality", "o'zbek");
        b.put("birthday", "2000-05-17");
        b.put("height", 172);
        b.put("hairColor", "qora");
        b.put("eyeColor", "jigarrang");
        b.put("clothSize", "S");
        b.put("shoeSize", "37");
        b.put("bust", "86");
        b.put("waist", "62");
        b.put("son", "90");
        b.put("email", "malika@example.com");
        b.put("phone", "+998901112233");
        b.put("telegram", "@malika");
        b.put("instagram", "malika.uz");
        b.put("photos", photos);
        return b;
    }

    private ResultActions submit(User u, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(APPLICATIONS)
                .header("Authorization", token(u))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private List<String> errorFields(JsonNode error) {
        List<String> fields = new ArrayList<>();
        error.path("errors").forEach(e -> fields.add(e.get("field").asText()));
        return fields;
    }

    // --------------------------------------------------------------- yuborish

    @Nested
    @DisplayName("POST /applications")
    class Submit {

        @Test
        @DisplayName("Anketa eski jadvalga admin kutgan qiymatlar bilan tushadi")
        void storesIntoLegacyTableWithServerDefaults() throws Exception {
            User u = user();
            UUID p1 = photo();
            UUID p2 = photo();

            Map<String, Object> body = validBody(List.of(p1, p2));
            // Klient narx va holatni yuborib ko'radi — e'tiborga olinmasligi SHART.
            body.put("status", 1);
            body.put("price", 999.0);
            body.put("age", 99);
            body.put("telegramId", "123");

            JsonNode res = json(submit(u, body)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.price").doesNotExist())
                    .andExpect(jsonPath("$.paid").value(false))
                    .andExpect(jsonPath("$.isWebShow").value(false))
                    .andExpect(jsonPath("$.castingType").value("model"))
                    .andExpect(jsonPath("$.name").value("Malika")));

            assertThat(res.get("photos")).hasSize(2);
            assertThat(res.get("createdAt").isNull()).isFalse();

            CastingUser saved = castingUserRepo.findById(res.get("id").asInt()).orElseThrow();
            assertThat(saved.getAppUserId()).isEqualTo(u.getId());
            assertThat(saved.getTelegramId()).isNull();
            assertThat(saved.getStatus()).isZero();
            assertThat(saved.getPrice()).isNull();
            assertThat(saved.getIsWebShow()).isFalse();
            assertThat(saved.getFirstChan()).isZero();
            assertThat(saved.getSecondChan()).isZero();
            assertThat(saved.getBirthday()).isEqualTo(LocalDate.of(2000, 5, 17).atStartOfDay());
            assertThat(saved.getAge())
                    .isEqualTo(Period.between(LocalDate.of(2000, 5, 17), LocalDate.now()).getYears());
            assertThat(saved.getPhotos()).extracting(Attachment::getId)
                    .containsExactlyInAnyOrder(p1, p2);
        }

        @Test
        @DisplayName("Tug'ilgan kun sana-vaqt ko'rinishida ham qabul qilinadi")
        void birthdayAcceptsDateTime() throws Exception {
            User u = user();
            Map<String, Object> body = validBody(List.of(photo()));
            body.put("birthday", "1998-01-02T00:00:00.000Z");

            JsonNode res = json(submit(u, body).andExpect(status().isCreated()));

            CastingUser saved = castingUserRepo.findById(res.get("id").asInt()).orElseThrow();
            assertThat(saved.getBirthday()).isEqualTo(LocalDateTime.of(1998, 1, 2, 0, 0));
        }

        @Test
        @DisplayName("Tokensiz — 401")
        void requiresToken() throws Exception {
            mockMvc.perform(post(APPLICATIONS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validBody(List.of(photo())))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------ validatsiya

    @Nested
    @DisplayName("Validatsiya")
    class Validation {

        @Test
        @DisplayName("Bo'sh tana — har bir majburiy maydon nomi bilan")
        void emptyBodyNamesEveryRequiredField() throws Exception {
            User u = user();
            JsonNode error = json(submit(u, Map.of())
                    .andExpect(status().isUnprocessableEntity()));

            assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
            assertThat(errorFields(error)).contains(
                    "castingType", "gender", "name", "birthday", "phone", "photos");
            assertThat(castingUserRepo.findAllByAppUserIdOrderByCreatedAtDesc(u.getId())).isEmpty();
        }

        @Test
        @DisplayName("Noma'lum casting turi va jins rad etiladi")
        void unknownEnumsRejected() throws Exception {
            Map<String, Object> body = validBody(List.of(photo()));
            body.put("castingType", "singer");
            body.put("gender", "other");

            JsonNode error = json(submit(user(), body).andExpect(status().isUnprocessableEntity()));
            assertThat(errorFields(error)).contains("castingType", "gender");
        }

        @Test
        @DisplayName("Rasm: kamida 1, ko'pi bilan 10")
        void photoCountBounds() throws Exception {
            JsonNode none = json(submit(user(), validBody(List.of()))
                    .andExpect(status().isUnprocessableEntity()));
            assertThat(errorFields(none)).contains("photos");

            List<UUID> eleven = IntStream.range(0, 11).mapToObj(i -> photo()).toList();
            JsonNode tooMany = json(submit(user(), validBody(eleven))
                    .andExpect(status().isUnprocessableEntity()));
            assertThat(errorFields(tooMany)).contains("photos");
        }

        @Test
        @DisplayName("Mavjud bo'lmagan rasm — aniq xato, anketa saqlanmaydi")
        void unknownPhotoRejected() throws Exception {
            User u = user();
            submit(u, validBody(List.of(photo(), UUID.randomUUID())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("CASTING_PHOTO_NOT_FOUND"));

            assertThat(castingUserRepo.findAllByAppUserIdOrderByCreatedAtDesc(u.getId())).isEmpty();
        }

        @Test
        @DisplayName("Boshqa anketaga biriktirilgan rasm — 409, 500 emas")
        void photoAlreadyLinkedRejected() throws Exception {
            UUID shared = photo();
            submit(user(), validBody(List.of(shared))).andExpect(status().isCreated());

            submit(user(), validBody(List.of(shared)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CASTING_PHOTO_IN_USE"));
        }

        @Test
        @DisplayName("Noto'g'ri va kelajakdagi tug'ilgan sana rad etiladi")
        void badBirthdayRejected() throws Exception {
            Map<String, Object> body = validBody(List.of(photo()));
            body.put("birthday", "17.05.2000");
            JsonNode error = json(submit(user(), body).andExpect(status().isUnprocessableEntity()));
            assertThat(errorFields(error)).contains("birthday");

            Map<String, Object> future = validBody(List.of(photo()));
            future.put("birthday", LocalDate.now().plusDays(1).toString());
            submit(user(), future)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    // ------------------------------------------------------ kutilayotgan ariza

    @Nested
    @DisplayName("Bir vaqtda bitta kutilayotgan ariza")
    class Pending {

        @Test
        @DisplayName("Ikkinchi ariza 409, admin javob bergach — yana mumkin")
        void secondPendingApplicationConflicts() throws Exception {
            User u = user();
            JsonNode first = json(submit(u, validBody(List.of(photo())))
                    .andExpect(status().isCreated()));

            submit(u, validBody(List.of(photo())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CASTING_APPLICATION_PENDING"));

            // Admin rad etdi (eski sayt aynan shu ustunni yozadi).
            CastingUser c = castingUserRepo.findById(first.get("id").asInt()).orElseThrow();
            c.setStatus(2);
            castingUserRepo.save(c);

            submit(u, validBody(List.of(photo()))).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Boshqa odamning kutilayotgan arizasi to'sqinlik qilmaydi")
        void otherUsersPendingDoesNotBlock() throws Exception {
            submit(user(), validBody(List.of(photo()))).andExpect(status().isCreated());
            submit(user(), validBody(List.of(photo()))).andExpect(status().isCreated());
        }
    }

    // ------------------------------------------------------ mening arizalarim

    @Nested
    @DisplayName("GET /applications/my")
    class Mine {

        @Test
        @DisplayName("Faqat o'z arizalari, yangisi yuqorida, holat nomi bilan")
        void returnsOnlyOwnRowsNewestFirst() throws Exception {
            User me = user();
            User other = user();

            JsonNode older = json(submit(me, validBody(List.of(photo())))
                    .andExpect(status().isCreated()));
            // Admin qabul qildi, narx qo'ydi va to'langan deb belgiladi.
            CastingUser approved = castingUserRepo.findById(older.get("id").asInt()).orElseThrow();
            approved.setStatus(1);
            approved.setPrice(500000.0);
            approved.setSecondChan(1);
            approved.setIsWebShow(true);
            approved.setCreatedAt(LocalDateTime.now().minusDays(2));
            castingUserRepo.save(approved);

            JsonNode newer = json(submit(me, validBody(List.of(photo())))
                    .andExpect(status().isCreated()));

            submit(other, validBody(List.of(photo()))).andExpect(status().isCreated());

            // Bot anketasi — egasi yo'q, hech kimning ro'yxatiga tushmasligi kerak.
            CastingUser bot = new CastingUser();
            bot.setTelegramId("770099001");
            bot.setName("Bot anketasi");
            bot.setStatus(0);
            bot.setCreatedAt(LocalDateTime.now());
            castingUserRepo.save(bot);

            JsonNode list = json(mockMvc.perform(get(MY).header("Authorization", token(me)))
                    .andExpect(status().isOk()));

            assertThat(list).hasSize(2);
            assertThat(list.get(0).get("id").asInt()).isEqualTo(newer.get("id").asInt());
            assertThat(list.get(0).get("status").asText()).isEqualTo("PENDING");

            JsonNode second = list.get(1);
            assertThat(second.get("id").asInt()).isEqualTo(older.get("id").asInt());
            assertThat(second.get("status").asText()).isEqualTo("APPROVED");
            assertThat(second.get("price").asDouble()).isEqualTo(500000.0);
            assertThat(second.get("paid").asBoolean()).isTrue();
            assertThat(second.get("isWebShow").asBoolean()).isTrue();
            assertThat(second.get("photos")).hasSize(1);

            // Shaxsiy maydonlar ro'yxatda chiqmaydi.
            assertThat(second.has("phone")).isFalse();
            assertThat(second.has("email")).isFalse();
        }

        @Test
        @DisplayName("Rad etilgan ariza REJECTED")
        void rejectedStatusIsNamed() throws Exception {
            User u = user();
            JsonNode created = json(submit(u, validBody(List.of(photo())))
                    .andExpect(status().isCreated()));
            CastingUser c = castingUserRepo.findById(created.get("id").asInt()).orElseThrow();
            c.setStatus(2);
            castingUserRepo.save(c);

            mockMvc.perform(get(MY).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("REJECTED"));
        }

        @Test
        @DisplayName("Arizasi yo'q odam — bo'sh ro'yxat")
        void emptyForNewUser() throws Exception {
            mockMvc.perform(get(MY).header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Tokensiz — 401")
        void requiresToken() throws Exception {
            mockMvc.perform(get(MY)).andExpect(status().isUnauthorized());
        }
    }
}
