package com.example.backend.Cms;

import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Sms.SmsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ilovaga PAROL bilan kirish va uch qadamli ro'yxatdan o'tish.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * Buyurtmachi ikkita alohida bo'lim so'radi: «ro'yxatdan o'tish»
 * (raqam → SMS kod → ism va parol) va «kirish» (raqam → parol). Bu testlar aynan o'sha ikki yo'lni va ular orasidagi
 * chegaralarni ushlab turadi:
 *
 * <ul>
 *   <li>band raqamga SMS YUBORILMAYDI — javob darhol 409, chunki
 *       ilova odamni «kirish» bo'limiga qaytarishi kerak;</li>
 *   <li>kod tasdiqlanmasdan parol qo'yib bo'lmaydi;</li>
 *   <li>SMS orqali yaratilgan eski, PAROLSIZ hisob egasi bloklanib
 *       qolmaydi — u shu oqim orqali o'z hisobiga parol qo'yadi
 *       («parolni unutdim» hali ishlamaydi);</li>
 *   <li>xodim hisobiga SMS orqali parol qo'yib bo'lmaydi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AppPasswordAuthTest {

    private static final String START = "/api/v1/app/auth/register/start";
    private static final String CONFIRM = "/api/v1/app/auth/register/confirm";
    private static final String COMPLETE = "/api/v1/app/auth/register/complete";
    private static final String LOGIN = "/api/v1/app/auth/login";

    /** Har testga o'z raqami: OTP holati xotirada va testlar orasida yashaydi. */
    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final Pattern CODE = Pattern.compile("(\\d{4})");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    /** Haqiqiy SMS o'rniga — matnini o'qib, kodni olamiz. */
    @MockBean private SmsClient smsClient;

    // ------------------------------------------------------------- yordamchi

    private String phone() {
        return "99890" + (7100000 + SEQ.incrementAndGet());
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private org.springframework.test.web.servlet.ResultActions call(String url, Map<String, ?> body)
            throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    /** SMS matnidan kodni oladi — mock shlyuzga aynan shu matn kelgan. */
    private String lastCode() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(smsClient, atLeastOnce()).send(anyString(), text.capture());
        Matcher m = CODE.matcher(text.getValue());
        assertThat(m.find()).as("SMS matnida 4 xonali kod bo'lishi kerak").isTrue();
        return m.group(1);
    }

    /** Raqamni SMS bosqichidan o'tkazadi va parol qo'yishga tayyorlaydi. */
    private String verifiedPhone() throws Exception {
        String phone = phone();
        call(START, Map.of("phone", phone)).andExpect(status().isOk());
        call(CONFIRM, Map.of("phone", phone, "code", lastCode())).andExpect(status().isOk());
        return phone;
    }

    private Role appRole() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        return role;
    }

    /** SMS yoki Google orqali yaratilgan hisob: paroli tasodifiy, egasi bilmaydi. */
    private User passwordlessUser(String phone) {
        User u = new User();
        u.setPhone(phone);
        u.setPassword(passwordEncoder.encode("tasodifiy-" + SEQ.incrementAndGet()));
        u.setRoles(new ArrayList<>(List.of(appRole())));
        return userRepo.save(u);
    }

    // ---------------------------------------------------------- ro'yxatdan o'tish

    @Nested
    @DisplayName("Ro'yxatdan o'tish")
    class Registration {

        @Test
        @DisplayName("Raqam → kod → parol: hisob yaratiladi va sessiya beriladi")
        void createsAccountAndSignsIn() throws Exception {
            String phone = verifiedPhone();

            call(COMPLETE, Map.of("phone", phone,
                    "name", "Aziz Karimov",
                    "password", "parol123",
                    "passwordConfirm", "parol123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.user.phone").value(phone))
                    .andExpect(jsonPath("$.user.name").value("Aziz Karimov"));

            User saved = userRepo.findByPhone(phone).orElseThrow();
            assertThat(saved.getName())
                    .as("ism saqlanmasa, profil va izohlar ostida bo'shliq qolardi")
                    .isEqualTo("Aziz Karimov");
            assertThat(saved.isPasswordSet())
                    .as("parol o'rnatilgani belgilanmasa, raqamni qayta ro'yxatdan "
                            + "o'tkazish mumkin bo'lib qolardi")
                    .isTrue();
            assertThat(passwordEncoder.matches("parol123", saved.getPassword())).isTrue();
        }

        @Test
        @DisplayName("Band raqamga SMS umuman yuborilmaydi")
        void busyPhoneGetsNoSms() throws Exception {
            String phone = verifiedPhone();
            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "parol123",
                    "passwordConfirm", "parol123")).andExpect(status().isOk());

            org.mockito.Mockito.reset(smsClient);

            call(START, Map.of("phone", phone))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PHONE_ALREADY_REGISTERED"));

            // ⚠️ SMS pul turadi. «Raqam band» javobini har chaqirishda
            // SMS bilan birga yuborish uni pullik takrorlash vositasiga
            // aylantirardi.
            verify(smsClient, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Parol takrori mos kelmasa — qabul qilinmaydi")
        void rejectsMismatchedConfirmation() throws Exception {
            String phone = verifiedPhone();

            call(COMPLETE, Map.of("phone", phone,
                    "name", "Aziz Karimov",
                    "password", "parol123",
                    "passwordConfirm", "parol124"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));

            assertThat(userRepo.findByPhone(phone)).isEmpty();
        }

        @Test
        @DisplayName("Ismsiz ro'yxatdan o'tib bo'lmaydi")
        void requiresName() throws Exception {
            String phone = verifiedPhone();

            call(COMPLETE, Map.of("phone", phone, "name", " ", "password", "parol123",
                    "passwordConfirm", "parol123"))
                    .andExpect(status().isUnprocessableEntity());

            assertThat(userRepo.findByPhone(phone)).isEmpty();
        }

        @Test
        @DisplayName("Ismdagi ortiqcha bo'shliqlar tozalanadi")
        void trimsName() throws Exception {
            String phone = verifiedPhone();

            call(COMPLETE, Map.of("phone", phone, "name", "  Aziz   Karimov  ",
                    "password", "parol123", "passwordConfirm", "parol123"))
                    .andExpect(status().isOk());

            assertThat(userRepo.findByPhone(phone).orElseThrow().getName())
                    .isEqualTo("Aziz Karimov");
        }

        @Test
        @DisplayName("Qisqa parol qabul qilinmaydi")
        void rejectsShortPassword() throws Exception {
            String phone = verifiedPhone();

            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "12345",
                    "passwordConfirm", "12345"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PASSWORD_TOO_SHORT"));
        }

        @Test
        @DisplayName("Kod tasdiqlanmasdan parol qo'yib bo'lmaydi")
        void requiresVerifiedPhone() throws Exception {
            String phone = phone();

            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "parol123",
                    "passwordConfirm", "parol123"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PHONE_NOT_VERIFIED"));

            assertThat(userRepo.findByPhone(phone)).isEmpty();
        }

        @Test
        @DisplayName("Bitta tasdiqlash — bitta parol")
        void verificationIsSingleUse() throws Exception {
            String phone = verifiedPhone();
            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "parol123",
                    "passwordConfirm", "parol123")).andExpect(status().isOk());

            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "boshqa123",
                    "passwordConfirm", "boshqa123"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PHONE_NOT_VERIFIED"));
        }

        /**
         * ⚠️ Bu eng qimmat holat: SMS orqali kirgan eski foydalanuvchi.
         *
         * Uning hisobi bor, paroli esa tasodifiy hash — ya'ni u
         * kira olmaydi. «Parolni unutdim» hali ishlamaydi. Agar
         * ro'yxatdan o'tish «raqam band» desa, odam butunlay
         * bloklanib qolardi.
         */
        @Test
        @DisplayName("Parolsiz eski hisobga parol qo'yish mumkin")
        void passwordlessAccountCanSetPassword() throws Exception {
            String phone = phone();
            java.util.UUID existingId = passwordlessUser(phone).getId();

            call(START, Map.of("phone", phone)).andExpect(status().isOk());
            call(CONFIRM, Map.of("phone", phone, "code", lastCode())).andExpect(status().isOk());
            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "parol123",
                    "passwordConfirm", "parol123"))
                    .andExpect(status().isOk());

            User saved = userRepo.findByPhone(phone).orElseThrow();
            assertThat(saved.getId())
                    .as("mavjud hisob qayta yaratilmasin — sevimlilar, xaridlar va "
                            + "obuna o'sha hisobga bog'langan")
                    .isEqualTo(existingId);
            assertThat(passwordEncoder.matches("parol123", saved.getPassword())).isTrue();
        }

        /**
         * ⚠️ Xodimning barcha satrlarida ham {@code password_set = false}
         * (V30 dan oldin yozilgan), lekin uning paroli HAQIQIY. Faqat
         * bayroqqa qaralsa, begona odam xodimning raqamiga SMS oldirib
         * panelga kirish parolini almashtira olardi.
         */
        @Test
        @DisplayName("Xodim hisobiga SMS orqali parol qo'yib bo'lmaydi")
        void staffAccountIsProtected() throws Exception {
            String phone = phone();

            Role admin = roleRepo.findByName(UserRoles.ROLE_ADMIN);
            if (admin == null) {
                int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
                admin = roleRepo.save(new Role(nextId, UserRoles.ROLE_ADMIN));
            }

            User staff = new User();
            staff.setPhone(phone);
            staff.setPassword(passwordEncoder.encode("AdminParol1!"));
            staff.setRoles(new ArrayList<>(List.of(admin)));
            userRepo.save(staff);

            call(START, Map.of("phone", phone))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PHONE_ALREADY_REGISTERED"));
        }
    }

    // ------------------------------------------------------------------ kirish

    @Nested
    @DisplayName("Kirish")
    class SignIn {

        @Test
        @DisplayName("Telefon va parol to'g'ri bo'lsa — sessiya beriladi")
        void signsInWithPassword() throws Exception {
            String phone = verifiedPhone();
            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "parol123",
                    "passwordConfirm", "parol123")).andExpect(status().isOk());

            call(LOGIN, Map.of("phone", phone, "password", "parol123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    // ⚠️ Refresh token HAR DOIM: mobil ilovada «meni eslab
                    // qol» degan tanlov yo'q, sessiya uzoq yashashi kerak.
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.user.phone").value(phone));
        }

        @Test
        @DisplayName("Raqamni +998 bilan tersa ham kiradi")
        void acceptsAnyPhoneFormat() throws Exception {
            String phone = verifiedPhone();
            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "parol123",
                    "passwordConfirm", "parol123")).andExpect(status().isOk());

            call(LOGIN, Map.of("phone", "+" + phone, "password", "parol123"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Parol xato bo'lsa — 401")
        void rejectsWrongPassword() throws Exception {
            String phone = verifiedPhone();
            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov", "password", "parol123",
                    "passwordConfirm", "parol123")).andExpect(status().isOk());

            call(LOGIN, Map.of("phone", phone, "password", "parol124"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Ro'yxatdan o'tmagan raqam — aniq aytiladi")
        void tellsWhenPhoneIsUnknown() throws Exception {
            call(LOGIN, Map.of("phone", phone(), "password", "parol123"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("PHONE_NOT_REGISTERED"));
        }

        /**
         * «Parol noto'g'ri» deyish bu holatda yolg'on bo'lardi: parol
         * umuman qo'yilmagan va odam uni hech qachon topa olmaydi.
         */
        @Test
        @DisplayName("Parolsiz hisob — ro'yxatdan o'tishga yo'naltiriladi")
        void tellsWhenPasswordWasNeverSet() throws Exception {
            String phone = phone();
            passwordlessUser(phone);

            call(LOGIN, Map.of("phone", phone, "password", "parol123"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("PASSWORD_NOT_SET"));
        }

        /**
         * V30 dan oldingi haqiqiy parolli hisob: bayrog'i yo'q edi.
         * Muvaffaqiyatli kirish uni to'g'rilaydi — aks holda uning
         * raqamiga SMS oldirib parolini almashtirish mumkin bo'lardi.
         */
        @Test
        @DisplayName("Eski parolli hisob birinchi kirishda belgilanadi")
        void marksLegacyAccountOnFirstLogin() throws Exception {
            String phone = phone();
            User legacy = passwordlessUser(phone);
            legacy.setPassword(passwordEncoder.encode("EskiParol1"));
            userRepo.save(legacy);

            call(LOGIN, Map.of("phone", phone, "password", "EskiParol1"))
                    .andExpect(status().isOk());

            assertThat(userRepo.findByPhone(phone).orElseThrow().isPasswordSet()).isTrue();

            call(START, Map.of("phone", phone))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PHONE_ALREADY_REGISTERED"));
        }
    }
}
