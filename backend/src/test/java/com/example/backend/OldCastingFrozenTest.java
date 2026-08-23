package com.example.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ESKI CASTING TIZIMI MUZLATILGAN.
 *
 * <h2>Nima uchun</h2>
 * Bu endpointlardan HOZIR ishlab turgan mijozlar foydalanadi:
 * Telegram bot (anketa yuboradi va rasm yuklaydi), eski admin sayti
 * ({@code src/admin/}) va mobil ilova ({@code mobile/}). Ular alohida
 * joylashtirilgan va bir vaqtda yangilanmaydi.
 *
 * Yo'lni o'zgartirish yoki controllerni ko'chirish — ularning hammasini
 * BIR ZUMDA sindiradi va buni faqat foydalanuvchi sezadi.
 *
 * <h2>Yangi kod qayerda</h2>
 * Yangi platforma ATAYLAB alohida makonda:
 *   backend  → {@code /api/v1/app/**}
 *   frontend → {@code /app/**}
 *
 * Ya'ni yangi yo'l qo'shish uchun bu yerga tegish KERAK EMAS. Agar shu test
 * yiqilsa — demak eski mijozlarni sindiradigan o'zgarish qilingan.
 *
 * <h2>Bu ro'yxatni qanday o'zgartirish mumkin</h2>
 * Faqat ataylab va mijozlar bilan kelishib. Ro'yxatdan yo'l olib tashlash =
 * "bu endpoint endi kerak emas" degan qaror, uni kod ko'rigida ko'rish kerak.
 */
class OldCastingFrozenTest {

    private static final Path CONTROLLERS =
            Paths.get("src/main/java/com/example/backend/Controller");

    /**
     * Eski controller → uning bazaviy yo'li.
     *
     * ⚠️ {@code AnalyticsIngestController} bu yerda YO'Q: u yangi kod bo'lib,
     * yangi makonga ko'chirilgan.
     */
    private static final Map<String, String> FROZEN_BASE_PATHS = new LinkedHashMap<>();

    static {
        FROZEN_BASE_PATHS.put("AdminController.java", "/api/v1/admin");
        FROZEN_BASE_PATHS.put("AttachmentController.java", "/api/v1/file");
        FROZEN_BASE_PATHS.put("AuthController.java", "/api/v1/auth");
        FROZEN_BASE_PATHS.put("CastingUserController.java", "/api/v1/casting-user");
        FROZEN_BASE_PATHS.put("NewsController.java", "/api/v1/news");
        FROZEN_BASE_PATHS.put("SecurityController.java", "/api/v1/security");
    }

    /**
     * To'liq endpoint ro'yxati.
     *
     * Bazaviy yo'l o'zgarmay, ichidagi bitta metod yo'li o'zgarsa ham mijoz
     * sinadi — shuning uchun har biri alohida qotirilgan.
     */
    private static final List<String> FROZEN_ENDPOINTS = List.of(
            "GET /api/v1/admin/statistic",

            "POST /api/v1/file/upload",
            "GET /api/v1/file/getFile/{id}",
            "PUT /api/v1/file/{attachmentId}",

            "POST /api/v1/auth/login",
            "POST /api/v1/auth/google",
            "POST /api/v1/auth/refresh",
            "GET /api/v1/auth/decode",
            "PUT /api/v1/auth/password/{adminId}",

            "GET /api/v1/casting-user",
            "GET /api/v1/casting-user/web",
            "POST /api/v1/casting-user",
            "PUT /api/v1/casting-user/web-show/{userId}",
            "GET /api/v1/casting-user/payed/{castingUserId}",
            "PUT /api/v1/casting-user/status/{castingUserId}/{status}/{price}",
            "PUT /api/v1/casting-user/price/{castingUserId}/{price}",
            "DELETE /api/v1/casting-user/{castingUserId}",
            "GET /api/v1/casting-user/my/{telegramId}",
            "GET /api/v1/casting-user/appeal/{appealId}",

            "GET /api/v1/news",
            "POST /api/v1/news",
            "PUT /api/v1/news/{id}",
            "DELETE /api/v1/news/{id}",
            "GET /api/v1/news/{id}",

            "GET /api/v1/security");

    /**
     * Eski casting ma'lumot qatlami: entity → jadval nomi.
     *
     * ⚠️ Bu yerda {@code AuditLog} va {@code UserPermission} YO'Q: ular
     * eski paketda tursa-da, RBAC uchun keyin qo'shilgan yangi kod.
     */
    private static final Map<String, String> FROZEN_ENTITIES = new LinkedHashMap<>();

    static {
        FROZEN_ENTITIES.put("Attachment.java", "attachment");
        FROZEN_ENTITIES.put("CastingUser.java", "casting_user");
        // Nomzodga Telegram bot yuboradigan javob. Bu MESSENGER emas -
        // «qabul qilindingiz / rad etildi» xabari.
        FROZEN_ENTITIES.put("Message.java", "message");
        FROZEN_ENTITIES.put("News.java", "news");
        FROZEN_ENTITIES.put("User.java", "users");
    }

    /** Eski repozitoriylar. Yangi RBAC repozitoriylari bu ro'yxatda yo'q. */
    private static final List<String> FROZEN_REPOS = List.of(
            "AttachmentRepo.java", "CastingUserRepo.java", "MessageRepo.java",
            "NewsRepo.java", "RoleRepo.java", "UserRepo.java");

    private static final Path ENTITIES =
            Paths.get("src/main/java/com/example/backend/Entity");

    private static final Path REPOS =
            Paths.get("src/main/java/com/example/backend/Repository");

    private static final Pattern TABLE =
            Pattern.compile("@Table\\(\\s*name\\s*=\\s*\"([^\"]+)\"");

    private static final Pattern BASE =
            Pattern.compile("@RequestMapping\\(\"([^\"]+)\"\\)");

    /**
     * Yo'l qiymatini oladi.
     *
     * {@code @PostMapping(value = "/login", consumes = "application/json")}
     * shaklini ham qamrashi kerak — shuning uchun yopuvchi qavs talab
     * qilinmaydi. {@code consumes} birinchi argument bo'lsa, qiymat
     * olinmaydi: tirnoq bevosita {@code (} yoki {@code value =} dan keyin
     * kelishi shart.
     */
    private static final Pattern MAPPING =
            Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\")?");

    @Test
    @DisplayName("Eski casting endpointlari o'zgarmagan")
    void oldEndpointsAreUnchanged() throws IOException {
        assertThat(Files.isDirectory(CONTROLLERS))
                .as("Eski controller papkasi ko'chirilgan yoki o'chirilgan: %s",
                        CONTROLLERS.toAbsolutePath())
                .isTrue();

        List<String> actual = new ArrayList<>();

        for (Map.Entry<String, String> entry : FROZEN_BASE_PATHS.entrySet()) {
            Path file = CONTROLLERS.resolve(entry.getKey());

            assertThat(Files.isRegularFile(file))
                    .as("Eski controller yo'qolgan yoki nomi o'zgargan: %s. "
                            + "Undan Telegram bot va eski sayt foydalanadi.", entry.getKey())
                    .isTrue();

            String source = Files.readString(file);

            Matcher base = BASE.matcher(source);
            assertThat(base.find())
                    .as("%s da @RequestMapping yo'q", entry.getKey())
                    .isTrue();
            assertThat(base.group(1))
                    .as("%s ning bazaviy yo'li o'zgargan. Bu barcha eski "
                            + "mijozlarni bir zumda sindiradi.", entry.getKey())
                    .isEqualTo(entry.getValue());

            Matcher m = MAPPING.matcher(source);
            while (m.find()) {
                String suffix = m.group(2) == null ? "" : m.group(2);
                actual.add(m.group(1).toUpperCase() + " " + entry.getValue() + suffix);
            }
        }

        assertThat(actual)
                .as("Eski endpointlar ro'yxati o'zgargan. Yangi yo'l kerak bo'lsa "
                        + "u /api/v1/app/** makoniga qo'shilsin — bu yerga tegilmasin.")
                .containsExactlyInAnyOrderElementsOf(FROZEN_ENDPOINTS);
    }

    @Test
    @DisplayName("Eski controller papkasiga yangi kod qo'shilmagan")
    void noNewCodeLandsInTheOldPackage() throws IOException {
        List<String> unexpected;
        try (var files = Files.list(CONTROLLERS)) {
            unexpected = files
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(".java"))
                    .filter(name -> !FROZEN_BASE_PATHS.containsKey(name))
                    .sorted()
                    .toList();
        }

        assertThat(unexpected)
                .as("Eski casting paketiga yangi controller qo'shilgan. Yangi kod "
                        + "Cms/ yoki Admin/ paketida va /api/v1/app/** yo'lida "
                        + "bo'lishi kerak — shunda eski va yangi aralashmaydi.")
                .isEmpty();
    }

    @Test
    @DisplayName("Eski casting entitylari va jadval nomlari o'zgarmagan")
    void oldEntitiesAreUnchanged() throws IOException {
        for (Map.Entry<String, String> entry : FROZEN_ENTITIES.entrySet()) {
            Path file = ENTITIES.resolve(entry.getKey());

            assertThat(Files.isRegularFile(file))
                    .as("Eski casting entity o'chirilgan yoki ko'chirilgan: %s. "
                            + "Buyurtmachi talabi: mavjud casting funksiyalari "
                            + "o'chirilmasin.", entry.getKey())
                    .isTrue();

            String source = Files.readString(file);
            Matcher m = TABLE.matcher(source);

            assertThat(m.find())
                    .as("%s da @Table(name=...) yo'q — jadval nomi kutilmaganda "
                            + "o'zgarishi mumkin", entry.getKey())
                    .isTrue();

            assertThat(m.group(1))
                    .as("%s jadval nomi o'zgargan. Mavjud ma'lumot yo'qoladi.",
                            entry.getKey())
                    .isEqualTo(entry.getValue());
        }
    }

    @Test
    @DisplayName("Eski casting repozitoriylari joyida")
    void oldRepositoriesExist() {
        for (String repo : FROZEN_REPOS) {
            assertThat(Files.isRegularFile(REPOS.resolve(repo)))
                    .as("Eski repozitoriy yo'qolgan: %s", repo)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Bot xabarini o'qish imkoni saqlangan")
    void botMessageLookupSurvives() throws IOException {
        String repo = Files.readString(REPOS.resolve("MessageRepo.java"));

        // Bot xabarni AYNAN shu metod orqali topadi. U yo'qolsa yoki
        // nomi o'zgarsa, casting javoblari nomzodga yetib bormaydi.
        assertThat(repo)
                .as("MessageRepo.findByCastingUserId yo'qolgan — Telegram bot "
                        + "nomzodga javob yubora olmay qoladi")
                .contains("findByCastingUserId");
    }
}