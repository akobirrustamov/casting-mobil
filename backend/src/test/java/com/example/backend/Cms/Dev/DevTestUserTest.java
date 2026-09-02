package com.example.backend.Cms.Dev;

import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Service.AppAccountService;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.UserRepo;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sozlamada ko'rsatilgan sinov hisobi HAQIQATAN kira olsinmi.
 *
 * <h2>Nima uchun bu test kirishni to'liq bosib ko'radi</h2>
 * «Foydalanuvchi bazada bor» degan tekshiruv bu yerda YETMAYDI —
 * aynan shu narsa ishlab turgan holda ham kirish ishlamasligi mumkin,
 * va ikkala sababning ham ko'rinadigan belgisi yo'q:
 *
 * <ol>
 *   <li>{@code AppAccountService.login()} raqamni normalizatsiya qiladi
 *       ({@code +998...} -> {@code 998...}). Seeder plyus bilan saqlasa,
 *       qator bazada TURADI, lekin kirish uni topa olmaydi va
 *       «bu raqam ro'yxatdan o'tmagan» deydi — ya'ni xato hisob YO'Q
 *       degan yolg'on javob bo'lib chiqadi.</li>
 *   <li>{@code passwordSet} bayrog'i qo'yilmasa, parol xato kiritilganda
 *       ilova «parol o'rnatilmagan» deb odamni ro'yxatdan o'tishga
 *       yuboradi.</li>
 * </ol>
 *
 * Ikkalasi ham «hisob yaratildimi» degan testdan o'tib ketardi.
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
    @Autowired private AppAccountService appAccountService;

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
     * ⚠️ Tartib MUHIM: xato parol testi muvaffaqiyatli kirishdan OLDIN
     * ishlashi kerak. Muvaffaqiyatli kirish {@code passwordSet}
     * bayrog'ini tuzatadi va keyin xato parol boshqa yo'ldan ketadi.
     */
    @Nested
    @DisplayName("Kirish")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Kirish {

        @Test
        @Order(2)
        @DisplayName("Telefon + parol bilan kiradi va token oladi")
        void logsIn() {
            Map<String, Object> session =
                    appAccountService.login(NORMALIZED, PASSWORD, new MockHttpServletRequest());

            assertThat(session).containsKeys("access_token", "refresh_token");
            assertThat((String) session.get("access_token")).isNotBlank();
        }

        @Test
        @Order(3)
        @DisplayName("Plyus bilan yozilgan raqam ham qabul qilinadi")
        void logsInWithPlusPrefix() {
            Map<String, Object> session =
                    appAccountService.login("+998945434230", PASSWORD, new MockHttpServletRequest());

            assertThat(session).containsKey("access_token");
        }

        /**
         * ⚠️ Eng birinchi ishlashi shart: muvaffaqiyatli kirish bayroqni
         * o'zi tuzatadi, ya'ni undan keyin bu tekshiruv seeder bayroqni
         * qo'ygan-qo'ymaganini umuman sezmay qoladi.
         */
        @Test
        @Order(0)
        @DisplayName("passwordSet=true — hali hech kim kirmagan holatda")
        void passwordFlagIsSetAtCreation() {
            var user = userRepo.findByPhone(NORMALIZED).orElseThrow();
            assertThat(user.isPasswordSet())
                    .as("bayroqsiz hisobda xato parol «parol o'rnatilmagan» xatosini beradi")
                    .isTrue();
        }

        @Test
        @Order(1)
        @DisplayName("Xato parolda «parol noto'g'ri» deydi, «parol o'rnatilmagan» emas")
        void wrongPasswordSaysWrongPassword() {
            assertThatThrownBy(() ->
                    appAccountService.login(NORMALIZED, "notogri-parol", new MockHttpServletRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
        }
    }
}
