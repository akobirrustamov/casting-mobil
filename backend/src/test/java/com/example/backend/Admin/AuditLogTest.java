package com.example.backend.Admin;

import com.example.backend.Entity.AuditLog;
import com.example.backend.Entity.User;
import com.example.backend.Repository.AuditLogRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §59 — audit jurnali.
 *
 * <h2>Auditning o'ziga xosligi</h2>
 * Oddiy ro'yxatda noto'g'ri filtr — noqulaylik. Audit jurnalida esa
 * <b>noto'g'ri xulosa</b>: tergovchi «bunday hodisa bo'lmagan» deb
 * o'ylaydi, aslida esa filtr uni ko'rsatmagan. Shuning uchun bu yerdagi
 * testlar ko'proq «javob to'liqmi?» degan savolga qaraydi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private AuditService auditService;
    @Autowired private AuditLogRepo auditLogRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private com.example.backend.Repository.RoleRepo roleRepo;

    // ------------------------------------------------------------- filtrlar

    @Nested
    @DisplayName("Filtrlar birga ishlaydi")
    class Filters {

        @Test
        @DisplayName("action va actorId birga qo'llanadi")
        void actionAndActorCombine() {
            User a = actor();
            User b = actor();
            auditService.log(a, AuditAction.PREMIUM_GRANTED, "User", 1L);
            auditService.log(b, AuditAction.PREMIUM_GRANTED, "User", 2L);
            auditService.log(a, AuditAction.USER_BLOCKED, "User", 3L);

            var found = search(AuditAction.PREMIUM_GRANTED, a.getId(), null, null);

            // Ilgari ternary faqat actorId ni ko'rardi va a ning
            // USER_BLOCKED yozuvi ham javobga tushardi.
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getEntityId()).isEqualTo("1");
        }

        @Test
        @DisplayName("entityType action bilan birga ishlaydi")
        void entityTypeCombinesWithAction() {
            User a = actor();
            auditService.log(a, AuditAction.CONTENT_UPDATED, "Content", 10L);
            auditService.log(a, AuditAction.CONTENT_UPDATED, "Episode", 10L);

            var found = search(AuditAction.CONTENT_UPDATED, a.getId(), "Episode", null);

            assertThat(found).hasSize(1);
            assertThat(found.get(0).getEntityType()).isEqualTo("Episode");
        }

        @Test
        @DisplayName("action qismiy matn bo'yicha topiladi")
        void actionMatchesPartially() {
            User a = actor();
            auditService.log(a, AuditAction.CONTENT_PUBLISHED, "Content", 20L);

            // Panelda bu maydon qidiruv darchasi: admin «content» deb
            // yozadi, «CONTENT_PUBLISHED» deb emas.
            assertThat(search("content", a.getId(), null, null)).hasSize(1);
            assertThat(search("CONTENT", a.getId(), null, null)).hasSize(1);
        }

        @Test
        @DisplayName("sana oralig'i chetlarini ham oladi")
        void dateRangeIncludesEdges() {
            User a = actor();
            auditService.log(a, AuditAction.TARIFF_CHANGED, "Tariff", 1L);

            LocalDateTime today = LocalDateTime.now();
            var found = auditLogRepo.search(null, a.getId(), null, null,
                    today.toLocalDate().atStartOfDay(),
                    today.toLocalDate().atTime(java.time.LocalTime.MAX),
                    PageRequest.of(0, 50)).getContent();

            // Bugun yozilgan yozuv «bugundan bugungacha» oralig'iga
            // tushmasa, kunlik tekshiruv umuman ishlamasdi.
            assertThat(found).hasSize(1);
        }

        @Test
        @DisplayName("filtrsiz so'rov hammasini qaytaradi")
        void noFilterReturnsAll() {
            User a = actor();
            auditService.log(a, AuditAction.USER_BLOCKED, "User", 1L);
            auditService.log(a, AuditAction.USER_UNBLOCKED, "User", 1L);

            assertThat(search(null, a.getId(), null, null)).hasSize(2);
        }

        private List<AuditLog> search(String action, UUID actorId,
                                      String entityType, String entityId) {
            return auditLogRepo.search(action, actorId, entityType, entityId,
                    null, null, PageRequest.of(0, 50)).getContent();
        }
    }

    // ------------------------------------------------------------- maxfiylik

    @Nested
    @DisplayName("Maxfiy qiymatlar jurnalga tushmaydi")
    class Secrets {

        @Test
        @DisplayName("password maydoni o'chiriladi")
        void passwordIsRedacted() {
            User a = actor();
            auditService.log(a, AuditAction.STAFF_PASSWORD_RESET, "User", 1L,
                    null, Map.of("password", "juda-maxfiy-parol", "name", "Ali"));

            AuditLog entry = last(a);
            assertThat(entry.getAfterState())
                    .as("parol ochiq matnda saqlanmasligi kerak")
                    .doesNotContain("juda-maxfiy-parol")
                    .contains("***")
                    // Voqea ko'rinib turishi kerak: qaysi maydon o'zgargani
                    .contains("password")
                    // Maxfiy bo'lmagan maydon o'z holida qoladi
                    .contains("Ali");
        }

        @Test
        @DisplayName("token, secret, apiKey ham o'chiriladi")
        void otherSecretsAreRedacted() {
            User a = actor();
            auditService.log(a, AuditAction.SETTING_CHANGED, "Setting", 1L,
                    null, Map.of("refreshToken", "tok-123",
                            "client_secret", "sec-456",
                            "apiKey", "key-789"));

            String json = last(a).getAfterState();
            assertThat(json).doesNotContain("tok-123", "sec-456", "key-789");
        }

        @Test
        @DisplayName("ichma-ich obyektda ham o'chiriladi")
        void nestedSecretsAreRedacted() {
            User a = actor();
            auditService.log(a, AuditAction.STAFF_UPDATED, "User", 1L,
                    null, Map.of("staff", Map.of("password", "ichki-parol")));

            assertThat(last(a).getAfterState()).doesNotContain("ichki-parol");
        }

        @Test
        @DisplayName("oddiy qiymatlar buzilmaydi")
        void ordinaryValuesSurvive() {
            User a = actor();
            auditService.log(a, AuditAction.TARIFF_CHANGED, "Tariff", 1L,
                    Map.of("price", "10000"), Map.of("price", "20000"));

            AuditLog entry = last(a);
            assertThat(entry.getBeforeState()).contains("10000");
            assertThat(entry.getAfterState()).contains("20000");
        }
    }

    // --------------------------------------------------------- to'liq yozuv

    @Nested
    @DisplayName("ТЗ maydonlari")
    class Fields {

        @Test
        @DisplayName("actorId, actorRole, action, entity, vaqt yoziladi")
        void requiredFieldsArePersisted() {
            User a = actor();
            auditService.log(a, AuditAction.CONTENT_CREATED, "Content", 42L);

            AuditLog entry = last(a);
            assertThat(entry.getActorId()).isEqualTo(a.getId());
            assertThat(entry.getActorRole()).isNotBlank();
            assertThat(entry.getAction()).isEqualTo(AuditAction.CONTENT_CREATED);
            assertThat(entry.getEntityType()).isEqualTo("Content");
            assertThat(entry.getEntityId()).isEqualTo("42");
            assertThat(entry.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("DTO userAgent'ni ham qaytaradi")
        void dtoExposesUserAgent() throws Exception {
            // Entity uni yozadi, lekin DTO'da bo'lmasa panel hech qachon
            // ko'rmasdi — ТЗ ro'yxatidagi maydon o'qilmas bo'lib qolardi.
            Set<String> dtoFields = new LinkedHashSet<>();
            for (Field f : Class.forName(
                    "com.example.backend.Admin.Controller.AuditLogController$AuditLogDto")
                    .getDeclaredFields()) {
                dtoFields.add(f.getName());
            }
            assertThat(dtoFields).contains("userAgent", "ip", "beforeState", "afterState");
        }
    }

    // ------------------------------------------------------- manba qoidalari

    @Nested
    @DisplayName("Manba qoidalari")
    class SourceRules {

        @Test
        @DisplayName("barcha audit chaqiruvlari e'lon qilingan konstantani ishlatadi")
        void allActionsAreDeclared() throws IOException {
            Set<String> declared = new LinkedHashSet<>();
            Matcher d = Pattern.compile("String (\\w+) = \"(\\w+)\"")
                    .matcher(Files.readString(Path.of(
                            "src/main/java/com/example/backend/Services/AuditService/AuditAction.java")));
            while (d.find()) {
                declared.add(d.group(2));
            }

            // Xom satr: bitta harf xatosi ("CONTNET_UPDATED") hech qanday
            // xatolik bermaydi, lekin filtr uni hech qachon topmaydi.
            List<String> raw = new ArrayList<>();
            Pattern call = Pattern.compile("auditService\\.log\\([^;]*?\"([A-Z_]+)\"", Pattern.DOTALL);
            for (Path f : sources()) {
                Matcher m = call.matcher(Files.readString(f));
                while (m.find()) {
                    raw.add(f.getFileName() + " → " + m.group(1));
                }
            }

            assertThat(raw)
                    .as("action nomi AuditAction'da e'lon qilinsin")
                    .isEmpty();
            assertThat(declared)
                    .as("ТЗ §59 ro'yxatidagi nomlar")
                    .contains("ADMIN_CREATED", "WORKER_CREATED", "ROLE_CHANGED",
                            "PERMISSION_CHANGED", "CONTENT_CREATED", "CONTENT_UPDATED",
                            "CONTENT_PUBLISHED", "CONTENT_ARCHIVED",
                            "ADVERTISEMENT_CREATED", "ADVERTISEMENT_UPDATED",
                            "PREMIUM_GRANTED", "PREMIUM_REVOKED", "TARIFF_CHANGED",
                            "COMMENT_HIDDEN", "NOTIFICATION_SENT");
        }

        @Test
        @DisplayName("jurnal o'chirilmaydi va tahrirlanmaydi")
        void journalIsAppendOnly() throws IOException {
            List<String> violations = new ArrayList<>();
            Pattern write = Pattern.compile("auditLogRepo\\s*\\.\\s*(delete\\w*|saveAll)\\s*\\(");

            for (Path f : sources()) {
                String src = Files.readString(f);
                Matcher m = write.matcher(src);
                while (m.find()) {
                    violations.add(f.getFileName() + " → " + m.group());
                }
                // O'chirish endpointi ham bo'lmasin
                if (f.getFileName().toString().equals("AuditLogController.java")) {
                    assertThat(src)
                            .as("audit controllerida yozuv endpointi bo'lmasligi kerak")
                            .doesNotContain("@DeleteMapping")
                            .doesNotContain("@PutMapping")
                            .doesNotContain("@PostMapping");
                }
            }

            assertThat(violations)
                    .as("ТЗ: audit log oddiy Admin tomonidan o'chirilmasin")
                    .isEmpty();
        }

        @Test
        @DisplayName("qoidalar haqiqatan yiqila oladi")
        void rulesCanFail() throws IOException {
            Pattern call = Pattern.compile("auditService\\.log\\([^;]*?\"([A-Z_]+)\"", Pattern.DOTALL);
            assertThat(call.matcher("auditService.log(actor, \"XOM_SATR\", \"User\", id);").find())
                    .isTrue();

            Pattern write = Pattern.compile("auditLogRepo\\s*\\.\\s*(delete\\w*|saveAll)\\s*\\(");
            assertThat(write.matcher("auditLogRepo.deleteAll();").find()).isTrue();
            assertThat(write.matcher("auditLogRepo.deleteById(1L);").find()).isTrue();

            assertThat(sources()).hasSizeGreaterThan(50);
        }

        private List<Path> sources() throws IOException {
            try (Stream<Path> s = Files.walk(Path.of("src/main/java/com/example/backend"))) {
                return s.filter(p -> p.toString().endsWith(".java")).toList();
            }
        }
    }

    // ----------------------------------------------------------- yordamchi

    private AuditLog last(User actor) {
        return auditLogRepo.search(null, actor.getId(), null, null, null, null,
                PageRequest.of(0, 1)).getContent().get(0);
    }

    private User actor() {
        com.example.backend.Entity.Role role =
                roleRepo.findByName(com.example.backend.Enums.UserRoles.ROLE_ADMIN);
        if (role == null) {
            int nextId = roleRepo.findAll().stream()
                    .mapToInt(com.example.backend.Entity.Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new com.example.backend.Entity.Role(
                    nextId, com.example.backend.Enums.UserRoles.ROLE_ADMIN));
        }
        User u = new User();
        u.setPhone("+99890" + (9600000 + SEQ.incrementAndGet()));
        u.setPassword("x");
        u.setName("Audit aktyori " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }
}
