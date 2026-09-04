package com.example.backend.Cms.Dev;

import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.DTO.UserDTO;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Services.AuthService.AuthService;
import com.example.backend.exceptions.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sozlamada ko'rsatilgan sinov hisobi HAQIQATAN kira olsinmi.
 *
 * <h2>Nima uchun bu test kirishni to'liq bosib ko'radi</h2>
 * «Foydalanuvchi bazada bor» degan tekshiruv bu yerda YETMAYDI: qator
 * bazada TURGAN holda ham kirish ishlamasligi mumkin, va buning
 * ko'rinadigan belgisi yo'q.
 *
 * Sabab — raqam SHAKLI. Dev-kirish ({@code mobile/.../devLogin.ts})
 * eski {@code POST /api/v1/auth/login} ga boradi, u esa raqamni
 * normalizatsiya QILMAYDI: bazada nima yozilgan bo'lsa, so'rovda ham
 * aynan o'sha bo'lishi kerak. Seeder raqamni plyus bilan saqlab
 * qo'ysa, kirish uni topa olmaydi va «login yoki parol xato» deydi —
 * ya'ni xato hisob YO'Q degan yolg'on javob bo'lib chiqadi.
 *
 * <h2>⚠️ Nega bu yerda AppAccountService yo'q</h2>
 * Ilovaning o'z kirishi 04.09.2026 dan buyon FAQAT SMS-kod bilan
 * ({@code AppOtpAuthTest}), parolli {@code login()} olib tashlandi.
 * Dev-hisob esa parol bilan qoladi: uning butun ma'nosi Eskiz yo'q
 * mahalliy konturda ilovaga kira olishda, ya'ni u ataylab boshqa,
 * eski eshikdan kiradi.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.dev.seed=true",
        // ⚠️ ATAYLAB plyus bilan: seeder uni o'zi to'g'rilashi kerak.
        "app.dev.test-user.phone=+998945434230",
        "app.dev.test-user.password=akow8434",
        "app.dev.test-user.name=Sinov Foydalanuvchi"
})
class DevTestUserTest {

    private static final String NORMALIZED = "998945434230";
    private static final String PASSWORD = "akow8434";

    @Autowired private UserRepo userRepo;
    @Autowired private UserAccountRepo userAccountRepo;
    @Autowired private AuthService authService;

    @Nested
    @DisplayName("Hisob yaratilishi")
    class Yaratilish {

        @Test
        @DisplayName("Raqam PLYUSSIZ saqlanadi — kirish shu ko'rinishda qidiradi")
        void phoneIsStoredNormalized() {
            assertThat(userRepo.findByPhone(NORMALIZED))
                    .as("sozlamada +998945434230 yozilgan, bazada 998945434230 bo'lishi kerak")
                    .isPresent();

            assertThat(userRepo.findByPhone("+998945434230"))
                    .as("plyus bilan saqlansa kirish uni topa olmaydi")
                    .isEmpty();
        }

        @Test
        @DisplayName("ROLE_USER — ilova foydalanuvchisi, panelga kirmaydi")
        void hasAppRoleOnly() {
            var user = userRepo.findByPhone(NORMALIZED).orElseThrow();
            assertThat(user.getRoles()).extracting("name").containsExactly(UserRoles.ROLE_USER);
        }

        @Test
        @DisplayName("UserAccount bor — obuna va balans shunga bog'lanadi")
        void hasAccount() {
            var user = userRepo.findByPhone(NORMALIZED).orElseThrow();
            assertThat(userAccountRepo.findByUserId(user.getId())).isPresent();
        }
    }

    /**
     * Dev-hisob HAQIQATAN kira oladimi — o'sha eshikdan, qaysinisidan
     * ilova kirsa.
     */
    @Nested
    @DisplayName("Kirish")
    class Kirish {

        @Test
        @DisplayName("Telefon + parol bilan kiradi va token oladi")
        void logsIn() {
            Map<String, Object> session = (Map<String, Object>)
                    authService.login(new UserDTO(NORMALIZED, PASSWORD, false)).getBody();

            assertThat(session).isNotNull();
            assertThat(session).containsKey("access_token");
            assertThat((String) session.get("access_token")).isNotBlank();
        }

        /**
         * ⚠️ Bu test SEEDER uchun: u raqamni plyussiz saqlashi SHART.
         *
         * Eski endpoint raqamni o'zi to'g'irlamaydi — plyusli qator
         * bazada yotgani bilan kirish uni topa olmaydi. Ya'ni bu yerdagi
         * «xato» aslida to'g'ri xatti-harakat, va u seederning
         * normalizatsiyasi buzilsa darhol ko'rinadigan bo'lib qoladi.
         */
        @Test
        @DisplayName("Plyusli raqam bilan kira olmaydi — shuning uchun seeder plyussiz saqlaydi")
        void plusPrefixIsNotFound() {
            assertThatThrownBy(() ->
                    authService.login(new UserDTO("+998945434230", PASSWORD, false)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("Xato parol kirgizmaydi")
        void wrongPasswordRejected() {
            assertThatThrownBy(() ->
                    authService.login(new UserDTO(NORMALIZED, "notogri-parol", false)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }
}
