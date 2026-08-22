package com.example.backend.Security;

import com.example.backend.Entity.RefreshToken;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RefreshTokenRepo;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Services.AuthService.RefreshTokenService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §61 (autentifikatsiya) va §62 (parol).
 *
 * <h2>Auditda topilgani</h2>
 * Mavjud auth qayta yozishni talab qilmasdi — BCrypt, env'dagi kalit,
 * IP bo'yicha rate limit joyida edi. Lekin uchta jiddiy kamchilik bor
 * edi va ularning har biri bitta o'g'irlangan tokenni uzoq muddatli
 * kirishga aylantirardi.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthSecurityTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private JwtService jwtService;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private RefreshTokenRepo refreshTokenRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LoginAttemptService loginAttempts;

    // ------------------------------------------------------------ token turi

    @Nested
    @DisplayName("Access va refresh token ajratilgan")
    class TokenType {

        @Test
        @DisplayName("Tokenlarda tur belgisi bor")
        void tokensCarryType() {
            User u = user();
            assertThat(jwtService.typeOf(jwtService.generateJwtToken(u)))
                    .isEqualTo(JwtService.TYPE_ACCESS);
            assertThat(jwtService.typeOf(
                    jwtService.generateJwtRefreshToken(u, UUID.randomUUID())))
                    .isEqualTo(JwtService.TYPE_REFRESH);
        }

        @Test
        @DisplayName("Refresh token API kaliti sifatida tanilmaydi")
        void refreshIsNotAnAccessToken() {
            User u = user();
            // ⚠️ Ilgari ikkalasi bir xil edi: o'g'irlangan refresh token
            // bilan 24 soat davomida hamma narsa qilish mumkin edi.
            assertThat(jwtService.isRefreshToken(
                    jwtService.generateJwtRefreshToken(u, UUID.randomUUID()))).isTrue();
            assertThat(jwtService.isRefreshToken(jwtService.generateJwtToken(u))).isFalse();
        }

        @Test
        @DisplayName("Access token bilan yangilab bo'lmaydi")
        void accessTokenCannotRefresh() {
            User u = user();
            // Aks holda o'g'irlangan access token muddatsiz uzaytirilardi.
            assertThatThrownBy(() -> refreshTokenService.rotate(
                    jwtService.generateJwtToken(u), request()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Access token muddati qisqa")
        void accessTokenIsShortLived() throws IOException {
            // 100 daqiqa «qisqa muddatli» degani emas.
            String src = Files.readString(Path.of(
                    "src/main/java/com/example/backend/Security/JwtService.java"));
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("access-token-ms:(\\d+)").matcher(src);
            assertThat(m.find()).isTrue();
            assertThat(Long.parseLong(m.group(1)))
                    .as("access token 30 daqiqadan oshmasin")
                    .isLessThanOrEqualTo(30 * 60 * 1000L);
        }
    }

    // ---------------------------------------------------------- rotatsiya

    @Nested
    @DisplayName("Rotatsiya va bekor qilish")
    class Rotation {

        @Test
        @DisplayName("Ishlatilgan token ikkinchi marta o'tmaydi")
        void tokenWorksOnce() {
            User u = user();
            String token = refreshTokenService.issue(u, request());

            refreshTokenService.rotate(token, request());

            assertThatThrownBy(() -> refreshTokenService.rotate(token, request()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Qayta ishlatilgan token butun zanjirni yopadi")
        void reuseRevokesEverything() {
            User u = user();
            String first = refreshTokenService.issue(u, request());
            String second = refreshTokenService.issue(u, request());
            refreshTokenService.rotate(first, request());

            // Bekor qilingani qayta keldi — nusxasi birovda bo'lishi
            // mumkin. Xavfsiz tomon: hamma sessiya yopiladi.
            assertThatThrownBy(() -> refreshTokenService.rotate(first, request()))
                    .isInstanceOf(BusinessException.class);

            assertThat(refreshTokenRepo.findById(jti(second)).orElseThrow().getRevokedAt())
                    .as("boshqa qurilmadagi sessiya ham yopilishi kerak")
                    .isNotNull();
        }

        @Test
        @DisplayName("Chiqish tokenni serverda o'chiradi")
        void logoutRevokes() {
            User u = user();
            String token = refreshTokenService.issue(u, request());

            refreshTokenService.revoke(token);

            // Chiqish klient tomonida qolsa, o'g'irlangan token muddati
            // tugaguncha ishlayverardi.
            assertThatThrownBy(() -> refreshTokenService.rotate(token, request()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Token matni bazada saqlanmaydi")
        void tokenTextIsNotStored() {
            User u = user();
            String token = refreshTokenService.issue(u, request());

            RefreshToken row = refreshTokenRepo.findById(jti(token)).orElseThrow();
            // Baza o'qilsa ham undan sessiyani tiklab bo'lmasin.
            assertThat(row.toString()).doesNotContain(token);
        }

        private UUID jti(String token) {
            return jwtService.jtiOf(token);
        }
    }

    // ------------------------------------------------- muvaffaqiyatsiz kirish

    @Nested
    @DisplayName("Hisob bo'yicha himoya")
    class Lockout {

        @Test
        @DisplayName("Ko'p xatodan keyin hisob vaqtincha yopiladi")
        void accountLocksAfterFailures() {
            String login = "+998900000" + SEQ.incrementAndGet();
            assertThat(loginAttempts.lockedMinutesLeft(login)).isZero();

            for (int i = 0; i < 5; i++) {
                loginAttempts.recordFailure(login);
            }

            // IP limiti bitta hisobga qaratilgan, ko'p manbali hujumni
            // to'xtata olmaydi — shuning uchun hisob bo'yicha ham sanaladi.
            assertThat(loginAttempts.lockedMinutesLeft(login)).isPositive();
        }

        @Test
        @DisplayName("Muvaffaqiyatli kirish hisobni tozalaydi")
        void successResetsCounter() {
            String login = "+998900001" + SEQ.incrementAndGet();
            for (int i = 0; i < 4; i++) {
                loginAttempts.recordFailure(login);
            }
            loginAttempts.recordSuccess(login);
            loginAttempts.recordFailure(login);

            assertThat(loginAttempts.lockedMinutesLeft(login))
                    .as("to'g'ri parol kiritgan foydalanuvchi jazolanmasin")
                    .isZero();
        }
    }

    // --------------------------------------------------------------- parol

    @Nested
    @DisplayName("ТЗ §62 — parol")
    class Passwords {

        @Test
        @DisplayName("BCrypt ishlatiladi va hash har safar boshqacha")
        void bcryptWithSalt() {
            String hash = passwordEncoder.encode("Parol123!");

            assertThat(hash).startsWith("$2");
            assertThat(hash).isNotEqualTo(passwordEncoder.encode("Parol123!"));
            assertThat(passwordEncoder.matches("Parol123!", hash)).isTrue();
        }

        @Test
        @DisplayName("Hech qayerda shifrlanmagan parol saqlanmaydi")
        void noPlainTextWrites() throws IOException {
            List<String> violations = new ArrayList<>();
            var call = java.util.regex.Pattern.compile("\\.setPassword\\(([^)]*)\\)");

            for (Path f : sources()) {
                var m = call.matcher(Files.readString(f));
                while (m.find()) {
                    String arg = m.group(1);
                    boolean encoded = arg.contains("encode(") || arg.trim().equals("\"\"");
                    if (!encoded) {
                        violations.add(f.getFileName() + " → setPassword(" + arg + ")");
                    }
                }
            }

            assertThat(violations)
                    .as("parol faqat encoder orqali saqlansin")
                    .isEmpty();
        }

        @Test
        @DisplayName("Parol hash'i javobda chiqmaydi")
        void hashIsNeverSerialized() throws IOException {
            String src = Files.readString(Path.of(
                    "src/main/java/com/example/backend/Entity/User.java"));
            // Ilgari buni bitta qo'lda yozilgan setPassword("") ushlab
            // turardi — yangi endpoint qo'shilsa hash sizib ketardi.
            assertThat(src).contains("WRITE_ONLY");
        }

        @Test
        @DisplayName("Qoida haqiqatan yiqila oladi")
        void ruleCanFail() throws IOException {
            var call = java.util.regex.Pattern.compile("\\.setPassword\\(([^)]*)\\)");
            assertThat(call.matcher("u.setPassword(request.getPassword());").find()).isTrue();
            assertThat(sources()).hasSizeGreaterThan(50);
        }

        private List<Path> sources() throws IOException {
            try (Stream<Path> s = Files.walk(Path.of("src/main/java/com/example/backend"))) {
                return s.filter(p -> p.toString().endsWith(".java")).toList();
            }
        }
    }

    // ------------------------------------------------------------ yordamchi

    private MockHttpServletRequest request() {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.addHeader("User-Agent", "JUnit");
        r.setRemoteAddr("127.0.0.1");
        return r;
    }

    @Transactional
    User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_ADMIN);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_ADMIN));
        }
        User u = new User();
        u.setPhone("+99890" + (9800000 + SEQ.incrementAndGet()));
        u.setPassword(passwordEncoder.encode("Parol123!"));
        u.setName("Auth sinovi " + SEQ.get());
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }
}
