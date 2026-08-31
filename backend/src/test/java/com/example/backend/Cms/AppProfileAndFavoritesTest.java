package com.example.backend.Cms;

import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.FavoriteType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Repository.UserFavoriteRepo;
import com.example.backend.Cms.Service.FavoriteService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Foydalanuvchi profili va sevimlilar.
 *
 * <h2>⚠️ Qaysi ikki nosozlik tuzatilyapti</h2>
 * <ul>
 *   <li><b>Profil hech qachon yangilanmasdi.</b> U kirish javobidan
 *       olinib telefonda saqlanardi. Panelda ism o'zgarsa yoki
 *       Premium berilsa, ilova bilmasdi. Refresh token oqimi buni
 *       jiddiylashtirdi: sessiya endi kunlab yashaydi;</li>
 *   <li><b>Sevimlilar faqat telefonda edi.</b> Ilova qayta
 *       o'rnatilsa yo'qolardi, ikkinchi qurilmada bo'sh bo'lardi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppProfileAndFavoritesTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String ME = "/api/v1/app/me";
    private static final String FAVORITES = "/api/v1/app/favorites";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserFavoriteRepo favoriteRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------- yordamchi

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9500000 + SEQ.incrementAndGet()));
        u.setPassword(passwordEncoder.encode("Parol123!"));
        u.setName("Tomoshabin " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private List<Integer> idsOf(String json) throws Exception {
        return objectMapper.readValue(
                objectMapper.readTree(json).get("targetIds").toString(),
                objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, Integer.class));
    }

    private String addFavorites(User u, List<Long> ids) throws Exception {
        return mockMvc.perform(post(FAVORITES)
                        .header("Authorization", token(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("type", "CREATOR", "targetIds", ids))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ---------------------------------------------------------------- profil

    @Nested
    @DisplayName("/app/me")
    class Profile {

        @Test
        @DisplayName("Profil qaytariladi")
        void returnsProfile() throws Exception {
            User u = user();

            mockMvc.perform(get(ME).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(u.getId().toString()))
                    .andExpect(jsonPath("$.phone").value(u.getPhone()))
                    .andExpect(jsonPath("$.name").value(u.getName()));
        }

        /**
         * ⚠️ Shakl kirish javobidagi bilan bir xil bo'lishi kerak —
         * mobil ilovadagi `toRole()` ikkalasini ham o'qiydi.
         */
        @Test
        @DisplayName("Rollar `[{name}]` shaklida")
        void rolesKeepTheLoginShape() throws Exception {
            mockMvc.perform(get(ME).header("Authorization", token(user())))
                    .andExpect(jsonPath("$.roles[0].name").value("ROLE_USER"));
        }

        /**
         * ⚠️ ASOSIY SABAB: ilova Premium holatini boshqa yo'l bilan
         * bila olmaydi.
         */
        @Test
        @DisplayName("Faol Premium ko'rsatiladi")
        void activePremiumIsReported() throws Exception {
            User u = user();
            accountRepo.save(UserAccount.builder()
                    .user(u)
                    .status(UserStatus.ACTIVE)
                    .language(Locale.RU)
                    .premiumUntil(LocalDateTime.now().plusDays(30))
                    .createdAt(LocalDateTime.now())
                    .build());

            mockMvc.perform(get(ME).header("Authorization", token(u)))
                    .andExpect(jsonPath("$.premium.active").value(true))
                    .andExpect(jsonPath("$.premium.until").exists())
                    .andExpect(jsonPath("$.language").value("RU"));
        }

        /**
         * ⚠️ Muddati o'tganda sana SAQLANADI.
         *
         * Ilova «obunangiz tugadi» deb aniq ayta oladi — «obuna yo'q»
         * emas. Bu ikki boshqa xabar va ikki boshqa tugma.
         */
        @Test
        @DisplayName("Muddati o'tgan Premium: active=false, sana qoladi")
        void expiredPremiumKeepsTheDate() throws Exception {
            User u = user();
            accountRepo.save(UserAccount.builder()
                    .user(u)
                    .status(UserStatus.ACTIVE)
                    .language(Locale.UZ)
                    .premiumUntil(LocalDateTime.now().minusDays(1))
                    .createdAt(LocalDateTime.now())
                    .build());

            mockMvc.perform(get(ME).header("Authorization", token(u)))
                    .andExpect(jsonPath("$.premium.active").value(false))
                    .andExpect(jsonPath("$.premium.until").exists());
        }

        /**
         * ⚠️ Hisob yozuvi bo'lmasligi mumkin — eski foydalanuvchilar
         * `cms_user_account` paydo bo'lishidan oldin yaratilgan.
         * Bu xato emas, sukut qiymatlar beriladi.
         */
        @Test
        @DisplayName("Hisob yozuvisiz foydalanuvchi ham javob oladi")
        void worksWithoutAccountRow() throws Exception {
            mockMvc.perform(get(ME).header("Authorization", token(user())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.language").value("UZ"))
                    .andExpect(jsonPath("$.premium.active").value(false));
        }

        @Test
        @DisplayName("Bloklangan hisob sababi bilan qaytadi")
        void blockedAccountExplainsWhy() throws Exception {
            User u = user();
            accountRepo.save(UserAccount.builder()
                    .user(u)
                    .status(UserStatus.BLOCKED)
                    .blockedReason("Qoidabuzarlik")
                    .language(Locale.UZ)
                    .createdAt(LocalDateTime.now())
                    .build());

            mockMvc.perform(get(ME).header("Authorization", token(u)))
                    .andExpect(jsonPath("$.status").value("BLOCKED"))
                    .andExpect(jsonPath("$.blockedReason").value("Qoidabuzarlik"));
        }

        /**
         * ⚠️ Anonim uchun «profil» tushunchasi yo'q — bu 401, bo'sh
         * javob emas. Klient farqni bilishi kerak.
         */
        @Test
        @DisplayName("Tokensiz — 401")
        void anonymousIsRejected() throws Exception {
            mockMvc.perform(get(ME)).andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------ sevimlilar

    @Nested
    @DisplayName("Sevimlilar")
    class Favorites {

        @Test
        @DisplayName("Qo'shiladi va o'qiladi")
        void addAndList() throws Exception {
            User u = user();
            addFavorites(u, List.of(7L, 9L));

            mockMvc.perform(get(FAVORITES).header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("CREATOR"))
                    .andExpect(jsonPath("$.targetIds.length()").value(2));
        }

        /**
         * ⚠️ Har javob TO'LIQ ro'yxat.
         *
         * Klient bitta so'rov bilan holatini server bilan tenglaydi.
         * Faqat «ok» qaytarilsa, ikki tomon ajralib ketishi uchun
         * bitta yo'qolgan javob yetardi.
         */
        @Test
        @DisplayName("Qo'shish javobida to'liq ro'yxat keladi")
        void mutationReturnsWholeList() throws Exception {
            User u = user();
            assertThat(idsOf(addFavorites(u, List.of(1L)))).containsExactly(1);
            assertThat(idsOf(addFavorites(u, List.of(2L)))).containsExactlyInAnyOrder(1, 2);
        }

        @Test
        @DisplayName("O'chiriladi")
        void remove() throws Exception {
            User u = user();
            addFavorites(u, List.of(3L, 4L));

            String json = mockMvc.perform(delete(FAVORITES)
                            .header("Authorization", token(u))
                            .param("targetId", "3"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(idsOf(json)).containsExactly(4);
        }

        /**
         * ⚠️ Mobil tarmoq ishonchsiz: so'rov ketib, javob yo'qolishi
         * mumkin va klient uni takrorlaydi.
         *
         * Takror qo'shish xato bersa, «yurakcha» tugmasi tarmoq
         * sifati yomon joyda xato ko'rsatardi — foydalanuvchi uchun
         * esa hech narsa buzilmagan.
         */
        @Test
        @DisplayName("Takroriy qo'shish xato BERMAYDI va nusxa yasamaydi")
        void addIsIdempotent() throws Exception {
            User u = user();
            addFavorites(u, List.of(5L));

            assertThat(idsOf(addFavorites(u, List.of(5L)))).containsExactly(5);
        }

        @Test
        @DisplayName("Mavjud bo'lmaganini o'chirish xato bermaydi")
        void removeIsIdempotent() throws Exception {
            mockMvc.perform(delete(FAVORITES)
                            .header("Authorization", token(user()))
                            .param("targetId", "999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.targetIds").isEmpty());
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV — kirishdan keyingi birlashtirish.
         *
         * Odam tizimga kirmasdan yurakcha bosadi, ro'yxat telefonda
         * yig'iladi. Kirgach klient uni yuboradi.
         *
         * Almashtirilsa boshqa qurilmada belgilangan sevimlilar
         * JIMGINA yo'qolardi.
         */
        @Test
        @DisplayName("Kirishdan keyin ro'yxatlar BIRLASHADI, almashmaydi")
        void loginMergesInsteadOfReplacing() throws Exception {
            User u = user();
            // Boshqa qurilmada belgilangan.
            addFavorites(u, List.of(10L, 11L));

            // Telefonda anonim yig'ilgan — bittasi ustma-ust tushadi.
            String json = addFavorites(u, List.of(11L, 12L));

            assertThat(idsOf(json)).containsExactlyInAnyOrder(10, 11, 12);
        }

        /**
         * ⚠️ Yangisi yuqorida: odam oxirgi qo'shganini qidiradi.
         */
        @Test
        @DisplayName("Yangi qo'shilgani ro'yxat boshida")
        void newestComesFirst() throws Exception {
            User u = user();
            addFavorites(u, List.of(20L));
            String json = addFavorites(u, List.of(21L));

            assertThat(idsOf(json).get(0)).isEqualTo(21);
        }

        /**
         * ⚠️ Takror qo'shish tartibni ARALASHTIRMASLIGI kerak.
         *
         * Mavjud yozuv qayta yozilsa `created_at` yangilanib, ro'yxat
         * sababsiz qayta tartiblanardi — foydalanuvchi uchun bu
         * «ro'yxatim o'zgarib ketdi» bo'lib ko'rinardi.
         */
        @Test
        @DisplayName("Takror qo'shish tartibni buzmaydi")
        void reAddKeepsOrder() throws Exception {
            User u = user();
            addFavorites(u, List.of(30L));
            addFavorites(u, List.of(31L));

            assertThat(idsOf(addFavorites(u, List.of(30L))).get(0))
                    .as("eski element yangisidan yuqoriga chiqib ketdi")
                    .isEqualTo(31);
        }

        /**
         * ⚠️ Bir odamning ro'yxati boshqasiga ko'rinmasligi kerak.
         */
        @Test
        @DisplayName("Ro'yxat FAQAT o'z egasiga ko'rinadi")
        void listIsPrivate() throws Exception {
            User owner = user();
            addFavorites(owner, List.of(40L, 41L));

            mockMvc.perform(get(FAVORITES).header("Authorization", token(user())))
                    .andExpect(jsonPath("$.targetIds").isEmpty());
        }

        /**
         * ⚠️ Boshqa odamning yozuvini o'chirib bo'lmasligi kerak.
         */
        @Test
        @DisplayName("Boshqaning sevimlisi o'chirilmaydi")
        void cannotDeleteSomeoneElses() throws Exception {
            User owner = user();
            addFavorites(owner, List.of(50L));

            mockMvc.perform(delete(FAVORITES)
                            .header("Authorization", token(user()))
                            .param("targetId", "50"))
                    .andExpect(status().isOk());

            mockMvc.perform(get(FAVORITES).header("Authorization", token(owner)))
                    .andExpect(jsonPath("$.targetIds.length()").value(1));
        }

        @Test
        @DisplayName("Tokensiz — 401")
        void anonymousIsRejected() throws Exception {
            mockMvc.perform(get(FAVORITES)).andExpect(status().isUnauthorized());
        }

        /**
         * ⚠️ Turlar ALOHIDA ro'yxat.
         *
         * Aks holda saqlangan film ijodkorlar ro'yxatida paydo
         * bo'lardi — id'lar boshqa jadvallardan va ular kesishishi
         * mumkin.
         */
        @Test
        @DisplayName("CREATOR va CONTENT aralashmaydi")
        void typesAreSeparate() throws Exception {
            User u = user();
            addFavorites(u, List.of(60L));

            mockMvc.perform(post(FAVORITES)
                            .header("Authorization", token(u))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("type", "CONTENT", "targetIds", List.of(60L)))))
                    .andExpect(jsonPath("$.targetIds.length()").value(1));

            mockMvc.perform(get(FAVORITES)
                            .header("Authorization", token(u))
                            .param("type", "CREATOR"))
                    .andExpect(jsonPath("$.targetIds.length()").value(1));

            assertThat(favoriteRepo.count()).isEqualTo(2);
        }

        /**
         * ⚠️ Chegara suiiste'molni to'xtatadi.
         *
         * `targetId` mavjudligi tekshirilmaydi (eski modul bilan
         * bog'lanmaslik uchun), ya'ni chegarasiz klient jadvalni
         * to'ldirib tashlashi mumkin edi.
         */
        @Test
        @DisplayName("Ro'yxat chegarasi bor")
        void listSizeIsCapped() throws Exception {
            User u = user();
            List<Long> tooMany = IntStream.rangeClosed(1, FavoriteService.MAX_BATCH + 1)
                    .mapToObj(Long::valueOf).toList();

            mockMvc.perform(post(FAVORITES)
                            .header("Authorization", token(u))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("type", "CREATOR", "targetIds", tooMany))))
                    .andExpect(status().is4xxClientError());
        }

        /**
         * Bo'sh ro'yxat — `null` emas. Klientda `null.length`
         * xatosi chiqmasligi uchun.
         */
        @Test
        @DisplayName("Bo'sh ro'yxat BO'SH MASSIV bo'lib keladi")
        void emptyIsAnArrayNotNull() throws Exception {
            mockMvc.perform(get(FAVORITES).header("Authorization", token(user())))
                    .andExpect(jsonPath("$.targetIds").isArray())
                    .andExpect(jsonPath("$.targetIds").isEmpty());
        }
    }
}
