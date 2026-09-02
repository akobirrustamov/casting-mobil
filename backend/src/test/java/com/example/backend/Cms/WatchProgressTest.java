package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.ContentType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Enums.StructureType;
import com.example.backend.Cms.Repository.WatchProgressRepo;
import com.example.backend.Cms.Service.ContentService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * «Ko'rishda davom eting».
 *
 * <h2>⚠️ Qaysi nosozlik tuzatilyapti</h2>
 * Odam qayerda to'xtagani HECH QAYERDA saqlanmasdi. Ikki soatlik
 * filmni 1:32:45 da to'xtatsa, ertasiga 0:00 dan boshlab qolgan
 * joyini O'ZI qidirishi kerak edi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WatchProgressTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final String BASE = "/api/v1/app/watch-progress";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private WatchProgressRepo progressRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ContentService contentService;
    @Autowired private com.example.backend.Cms.Repository.ContentRepo contentRepo;

    // ------------------------------------------------------------- yordamchi

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        u.setPhone("+99890" + (9700000 + SEQ.incrementAndGet()));
        u.setPassword(passwordEncoder.encode("Parol123!"));
        u.setName("Tomoshabin " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }

    /**
     * Nashr qilingan yaxlit kontent.
     *
     * ⚠️ HAQIQIY kontent kerak: lenta endi kartochka bilan qaytadi va
     * mavjud bo'lmagan kontentga ishora qiluvchi satrni CHIQARIB
     * tashlaydi. O'ylab topilgan `targetId` bilan lenta doim bo'sh
     * chiqardi va testlar hech narsani tekshirmasdi.
     */
    private Content publishedContent() {
        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setLanguage("uz");
        c.setTranslations(Map.of(
                Locale.UZ, TranslationDto.ofTitle("Film " + SEQ.incrementAndGet()),
                Locale.RU, TranslationDto.ofTitle("Фильм"),
                Locale.EN, TranslationDto.ofTitle("Movie")));
        return contentService.create(null, c);
    }

    /** {@code null} qiymatlarni ham yubora olishi uchun {@code HashMap}. */
    private String body(Integer position, Integer duration, String quality) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("position", position);
        map.put("duration", duration);
        map.put("quality", quality);
        return objectMapper.writeValueAsString(map);
    }

    private String save(User u, String type, long targetId,
                        Integer position, Integer duration, String quality) throws Exception {
        return mockMvc.perform(put(BASE + "/" + type + "/" + targetId)
                        .header("Authorization", token(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(position, duration, quality)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ------------------------------------------------------------- saqlash

    @Nested
    @DisplayName("Saqlash")
    class Saqlash {

        @Test
        @DisplayName("Pozitsiya saqlanadi va qaytariladi")
        void savesPosition() throws Exception {
            User u = user();
            save(u, "EPISODE", 42L, 5565, 7200, "720p");

            mockMvc.perform(get(BASE + "/EPISODE/42").header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.position").value(5565))
                    .andExpect(jsonPath("$.duration").value(7200))
                    .andExpect(jsonPath("$.quality").value("720p"))
                    .andExpect(jsonPath("$.completed").value(false));
        }

        /**
         * ⚠️ Klient bu maydonsiz ishlay olmaydi.
         *
         * U pozitsiyani ikki joyda saqlaydi — telefonda va serverda —
         * va ochilishda ikkalasi mos kelmasligi mumkin (oxirgi seans
         * internetsiz o'tgan bo'lsa). Vaqt belgisisiz «qaysi biri
         * yangiroq» degan savolga javob yo'q va klient taxmin
         * qilishga majbur bo'lardi.
         */
        @Test
        @DisplayName("Javobda updatedAt bo'ladi")
        void returnsUpdatedAt() throws Exception {
            User u = user();
            save(u, "EPISODE", 77L, 1000, 7200, "auto");

            mockMvc.perform(get(BASE + "/EPISODE/77").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV — progress har 15 soniyada keladi.
         *
         * Har so'rov yangi satr qoldirsa, ikki soatlik film bitta
         * ko'rish uchun ~480 satr yaratardi va «qayerda to'xtadi»
         * degan savolga javob yo'qolardi.
         */
        @Test
        @DisplayName("Takroriy saqlash BITTA satr qoldiradi")
        void upsertsSingleRow() throws Exception {
            User u = user();

            save(u, "EPISODE", 42L, 100, 7200, "auto");
            save(u, "EPISODE", 42L, 200, 7200, "auto");
            save(u, "EPISODE", 42L, 300, 7200, "auto");

            assertThat(progressRepo.findAll()).hasSize(1);
            assertThat(progressRepo.findAll().get(0).getPositionSeconds()).isEqualTo(300);
        }

        /**
         * ⚠️ ORQAGA surish ham saqlanadi.
         *
         * «Faqat oldinga» qoidasi qo'yilsa, orqaga qaytib ko'rish
         * yozilmasdi: odam 10 daqiqa orqaga qaytib ilovani yopsa,
         * qaytganda yana oldingi joyga tashlanardi.
         */
        @Test
        @DisplayName("Orqaga surilgan pozitsiya ham saqlanadi")
        void acceptsBackwardSeek() throws Exception {
            User u = user();

            save(u, "EPISODE", 42L, 5000, 7200, "auto");
            save(u, "EPISODE", 42L, 1200, 7200, "auto");

            mockMvc.perform(get(BASE + "/EPISODE/42").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.position").value(1200));
        }

        /**
         * ⚠️ SINGLE kontent ham saqlanishi kerak.
         *
         * Ko'rish ikki endpointdan boradi va filmlar aynan
         * {@code /watch/content/{id}} orqali ochiladi. Faqat
         * {@code episodeId} li jadval ularni butunlay tashlab
         * ketardi — ular esa eng uzunlari.
         */
        @Test
        @DisplayName("CONTENT turi ham saqlanadi va EPISODE dan ajratiladi")
        void separatesContentFromEpisode() throws Exception {
            User u = user();

            save(u, "EPISODE", 7L, 100, 3600, "auto");
            save(u, "CONTENT", 7L, 900, 3600, "auto");

            mockMvc.perform(get(BASE + "/EPISODE/7").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.position").value(100));
            mockMvc.perform(get(BASE + "/CONTENT/7").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.position").value(900));
        }

        /**
         * ⚠️ Pleyer davomiylikdan biroz KATTA qiymat berishi mumkin
         * (oxirgi segment to'liq emas). Kesilmasa foiz 100 dan oshib,
         * progress chizig'i tashqariga chiqib ketardi.
         */
        @Test
        @DisplayName("Davomiylikdan oshgan pozitsiya kesiladi")
        void clampsOverflow() throws Exception {
            User u = user();
            save(u, "EPISODE", 42L, 7250, 7200, "auto");

            mockMvc.perform(get(BASE + "/EPISODE/42").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.position").value(7200))
                    .andExpect(jsonPath("$.percent").value(100));
        }

        /**
         * ⚠️ Noma'lum sifat butun saqlashni YIQITMAYDI.
         *
         * Sifat — ikkinchi darajali ma'lumot. Uning ustidan xato
         * qaytarilsa, u bilan birga POZITSIYA ham yo'qolardi — bu
         * yerdagi asosiy qiymat esa aynan pozitsiya.
         */
        @Test
        @DisplayName("Noma'lum sifat null ga aylanadi, pozitsiya saqlanadi")
        void unknownQualityDoesNotBreakSave() throws Exception {
            User u = user();
            save(u, "EPISODE", 42L, 1234, 7200, "2160p");

            mockMvc.perform(get(BASE + "/EPISODE/42").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.position").value(1234))
                    .andExpect(jsonPath("$.quality").doesNotExist());
        }
    }

    // ------------------------------------------------------- davom ettirish

    @Nested
    @DisplayName("Davom eting ro'yxati")
    class Continue {

        /**
         * ⚠️ Tugatilgan film ro'yxatda QOLMASLIGI kerak.
         *
         * 100% kutilsa u abadiy osilib qolardi: odam titrlarni
         * oxirigacha ko'rmaydi va pleyer oxirgi soniyalarni ko'pincha
         * umuman bermaydi.
         */
        @Test
        @DisplayName("Oxirigacha ko'rilgani ro'yxatdan chiqadi")
        void completedIsExcluded() throws Exception {
            User u = user();
            Content yarim = publishedContent();
            Content tugagan = publishedContent();

            save(u, "CONTENT", yarim.getId(), 1000, 7200, "auto");    // yarmi ham emas
            save(u, "CONTENT", tugagan.getId(), 7000, 7200, "auto");  // 97% — tugatilgan

            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].progress.targetId").value(yarim.getId()));
        }

        /**
         * ⚠️ ENG MUHIM TEKSHIRUV LENTA UCHUN.
         *
         * Progress jadvalida faqat raqamlar bor. Kartochkasiz lenta
         * chizib bo'lmaydi: afishasi ham, sarlavhasi ham yo'q
         * to'rtburchaklar chiqardi. Klient ularni alohida so'rasa,
         * bosh sahifa ochilishida yigirmata qo'shimcha so'rov ketardi.
         */
        @Test
        @DisplayName("Har element KARTOCHKA bilan keladi")
        void includesContentCard() throws Exception {
            User u = user();
            Content film = publishedContent();
            save(u, "CONTENT", film.getId(), 1000, 7200, "auto");

            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.items[0].content.id").value(film.getId()))
                    .andExpect(jsonPath("$.items[0].content.title").exists())
                    .andExpect(jsonPath("$.items[0].progress.percent").value(14));
        }

        /**
         * ⚠️ Sarlavha SO'RALGAN tilda.
         *
         * Til uzatilmasa lenta o'zbekcha qolardi — bosh sahifadagi
         * qolgan qatorlar esa ruscha. Bitta ekranda ikki til.
         */
        @Test
        @DisplayName("Sarlavha so'ralgan tilda")
        void respectsLocale() throws Exception {
            User u = user();
            Content film = publishedContent();
            save(u, "CONTENT", film.getId(), 1000, 7200, "auto");

            mockMvc.perform(get(BASE + "/continue?locale=RU")
                            .header("Authorization", token(u)))
                    .andExpect(jsonPath("$.items[0].content.title").value("Фильм"));
        }

        /**
         * ⚠️ Nashrdan olingan kontent lentada QOLMASLIGI kerak.
         *
         * Odam ko'rgan film keyin yopilgan bo'lishi mumkin. Uni
         * qoldirish «bosdim — ochilmadi» degan holatga olib kelardi,
         * va sabab foydalanuvchiga umuman ko'rinmasdi.
         */
        @Test
        @DisplayName("Nashrdan olingan kontent lentadan chiqadi")
        void unpublishedIsExcluded() throws Exception {
            User u = user();
            Content film = publishedContent();
            save(u, "CONTENT", film.getId(), 1000, 7200, "auto");

            film.setStatus(PublicationStatus.DRAFT);
            contentRepo.save(film);

            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.items.length()").value(0));
        }

        /**
         * ⚠️ Osilib qolgan satr lentani YIQITMASLIGI kerak.
         *
         * {@code target_id} ga chet el kaliti ataylab qo'yilmagan — u
         * turga qarab ikki xil jadvalga ishora qiladi. Ya'ni maqsadi
         * o'chirilgan satr HAQIQATAN bo'lishi mumkin, va u butun
         * lentani 500 xatosiga olib kelmasligi kerak.
         */
        @Test
        @DisplayName("Mavjud bo'lmagan videoga ishora qiluvchi satr o'tkazib yuboriladi")
        void danglingRowIsSkipped() throws Exception {
            User u = user();
            Content film = publishedContent();

            save(u, "CONTENT", film.getId(), 1000, 7200, "auto");
            save(u, "CONTENT", 999_999L, 2000, 7200, "auto");

            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(u)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].progress.targetId").value(film.getId()));
        }

        /**
         * ⚠️ Yaxlit kontentda qism raqami BO'LMAYDI.
         *
         * «1-qism» deb yozish yolg'on bo'lardi: filmda qism degan
         * tushuncha yo'q.
         */
        @Test
        @DisplayName("Filmda qism raqami bo'lmaydi")
        void singleHasNoEpisodeNumber() throws Exception {
            User u = user();
            Content film = publishedContent();
            save(u, "CONTENT", film.getId(), 1000, 7200, "auto");

            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.items[0].episodeNumber").doesNotExist());
        }

        /**
         * ⚠️ Tasodifan ochilgan video ro'yxatga TUSHMAYDI.
         *
         * Odam videoni ochib darhol yopishi mumkin. Bunday yozuvlar
         * ro'yxatni to'ldirib, haqiqiy «davom eting» elementlarini
         * pastga surib yuborardi.
         */
        @Test
        @DisplayName("Boshidagi bir necha soniya ro'yxatga tushmaydi")
        void tooShortIsExcluded() throws Exception {
            User u = user();
            Content film = publishedContent();
            save(u, "CONTENT", film.getId(), 5, 7200, "auto");

            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.items.length()").value(0));
        }

        /**
         * ⚠️ Bir odamning ro'yxatida BOSHQA odamning videosi
         * ko'rinmasin. Bu shaxsiy ma'lumot: nima ko'rgani.
         */
        @Test
        @DisplayName("Ro'yxat faqat o'z egasiniki")
        void isPrivate() throws Exception {
            User a = user();
            User b = user();
            Content film = publishedContent();

            save(a, "CONTENT", film.getId(), 1000, 7200, "auto");

            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(b)))
                    .andExpect(jsonPath("$.items.length()").value(0));
        }

        @Test
        @DisplayName("Ro'yxatdan o'chirish")
        void forget() throws Exception {
            User u = user();
            Content film = publishedContent();
            save(u, "CONTENT", film.getId(), 1000, 7200, "auto");

            mockMvc.perform(delete(BASE + "/CONTENT/" + film.getId())
                            .header("Authorization", token(u)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.items.length()").value(0));
        }

        /**
         * ⚠️ Yo'q yozuvni o'chirish ham XATO EMAS: klient so'rovni
         * takrorlagan bo'lishi mumkin va natija bir xil.
         */
        @Test
        @DisplayName("Yo'q yozuvni o'chirish xato bermaydi")
        void forgetMissingIsFine() throws Exception {
            mockMvc.perform(delete(BASE + "/EPISODE/999").header("Authorization", token(user())))
                    .andExpect(status().isNoContent());
        }
    }

    // ------------------------------------------------------------- himoya

    @Nested
    @DisplayName("Himoya")
    class Himoya {

        /**
         * ⚠️ Nima ko'rgani — SHAXSIY ma'lumot. Tokensiz so'rov
         * o'tsa, uni saqlash ham, o'qish ham kimga tegishli ekani
         * noma'lum bo'lardi.
         */
        @Test
        @DisplayName("Tokensiz o'qib bo'lmaydi")
        void requiresToken() throws Exception {
            mockMvc.perform(get(BASE + "/continue"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("Tokensiz saqlab bo'lmaydi")
        void requiresTokenToSave() throws Exception {
            mockMvc.perform(put(BASE + "/EPISODE/42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(100, 7200, "auto")))
                    .andExpect(status().is4xxClientError());
        }
    }

    // -------------------------------------------------------- davomiyliksiz

    /**
     * ⚠️ Transkodlash tugamagan videoda davomiylik NOMA'LUM.
     *
     * Unda tugallanganlik hisoblab bo'lmaydi va uni «ko'rilgan» deb
     * belgilash video birinchi so'rovdayoq ro'yxatdan tushib
     * ketishiga olib kelardi.
     */
    @Nested
    @DisplayName("Davomiylik noma'lum")
    class NoDuration {

        @Test
        @DisplayName("Foiz null, tugallangan emas, pozitsiya ishlaydi")
        void handlesUnknownDuration() throws Exception {
            User u = user();
            Content film = publishedContent();
            save(u, "CONTENT", film.getId(), 4000, null, "auto");

            mockMvc.perform(get(BASE + "/CONTENT/" + film.getId())
                            .header("Authorization", token(u)))
                    .andExpect(jsonPath("$.position").value(4000))
                    .andExpect(jsonPath("$.percent").doesNotExist())
                    .andExpect(jsonPath("$.completed").value(false));

            // ⚠️ Davomiyligi noma'lum video lentada QOLADI: tugallangan
            // deb belgilash uchun asos yo'q, ya'ni odam uni ko'rishda
            // davom etayotgan bo'lishi mumkin.
            mockMvc.perform(get(BASE + "/continue").header("Authorization", token(u)))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].progress.percent").doesNotExist());
        }
    }
}
