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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ilovaga kirish: raqam → SMS kod → (yangi bo'lsa) ism.
 *
 * <h2>Nima qo'riqlanadi</h2>
 * Buyurtmachi (04.09.2026) parolni butunlay bekor qildi va bitta oqim
 * so'radi. Bu testlar o'sha oqimning uchta muhim qirrasini ushlab
 * turadi:
 *
 * <ul>
 *   <li>eski va yangi foydalanuvchi BIR XIL boshlanadi — ikkalasiga
 *       ham kod ketadi, «bu raqam band» degan xato endi yo'q;</li>
 *   <li>yangi odamga kod tasdiqlangani bilan token BERILMAYDI: hisob
 *       ism bilan birga tug'iladi, ismsiz satr qolmaydi;</li>
 *   <li>tasdiqlanmagan raqamga ism yozib hisob yasab bo'lmaydi.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
// ⚠️ IP bo'yicha cheklov o'chirilgan: hamma test bitta «IP» dan keladi
// va oltinchi so'rovda 429 olardi. Cheklovning o'zi RateLimitFilter
// testlarida tekshiriladi — bu yerda esa u oqim mantig'ini yashirib
// qo'yardi.
@TestPropertySource(properties = "app.ratelimit.enabled=false")
class AppOtpAuthTest {

    private static final String SEND = "/api/v1/app/auth/otp/send";
    private static final String VERIFY = "/api/v1/app/auth/otp/verify";
    private static final String COMPLETE = "/api/v1/app/auth/otp/complete";

    /** Har testga o'z raqami: OTP holati xotirada va testlar orasida yashaydi. */
    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final Pattern CODE = Pattern.compile("(\\d{4})");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    /** Haqiqiy SMS o'rniga — matnini o'qib, kodni olamiz. */
    @MockBean private SmsClient smsClient;

    // ------------------------------------------------------------- yordamchi

    private String phone() {
        return "99890" + (7100000 + SEQ.incrementAndGet());
    }

    private org.springframework.test.web.servlet.ResultActions call(String url, Map<String, ?> body)
            throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /** SMS matnidan kodni oladi — mock shlyuzga aynan shu matn kelgan. */
    private String lastCode() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(smsClient, atLeastOnce()).send(anyString(), text.capture());
        Matcher m = CODE.matcher(text.getValue());
        assertThat(m.find()).as("SMS matnida 4 xonali kod bo'lishi kerak").isTrue();
        return m.group(1);
    }

    private Role appRole() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        return role;
    }

    private User existingUser(String phone, String name) {
        User u = new User();
        u.setPhone(phone);
        u.setName(name);
        u.setRoles(new ArrayList<>(List.of(appRole())));
        return userRepo.save(u);
    }

    // -------------------------------------------------------- yangi foydalanuvchi

    @Nested
    @DisplayName("Yangi foydalanuvchi")
    class NewUser {

        @Test
        @DisplayName("Raqam → kod → ism: hisob yaratiladi va sessiya beriladi")
        void createsAccountAfterName() throws Exception {
            String phone = phone();

            call(SEND, Map.of("phone", phone)).andExpect(status().isOk());
            call(VERIFY, Map.of("phone", phone, "code", lastCode()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name_required").value(true))
                    // ⚠️ Token bo'lmasligi SHART: bo'lsa, u ismsiz hisobga
                    // tegishli bo'lardi.
                    .andExpect(jsonPath("$.access_token").doesNotExist());

            assertThat(userRepo.findByPhone(phone))
                    .as("hisob kod bosqichida YARATILMASIN — ismsiz satr qolardi")
                    .isEmpty();

            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name_required").value(false))
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.user.phone").value(phone))
                    .andExpect(jsonPath("$.user.name").value("Aziz Karimov"));

            assertThat(userRepo.findByPhone(phone).orElseThrow().getName())
                    .isEqualTo("Aziz Karimov");
        }

        @Test
        @DisplayName("Kalta ism qabul qilinmaydi va hisob yaratilmaydi")
        void rejectsShortName() throws Exception {
            String phone = phone();

            call(SEND, Map.of("phone", phone)).andExpect(status().isOk());
            call(VERIFY, Map.of("phone", phone, "code", lastCode())).andExpect(status().isOk());

            call(COMPLETE, Map.of("phone", phone, "name", "A"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("NAME_INVALID"));

            assertThat(userRepo.findByPhone(phone)).isEmpty();
        }
    }

    // -------------------------------------------------------- mavjud foydalanuvchi

    @Nested
    @DisplayName("Mavjud foydalanuvchi")
    class ExistingUser {

        @Test
        @DisplayName("Ismli hisob kod bilan DARHOL kiradi — ism so'ralmaydi")
        void signsInWithoutName() throws Exception {
            String phone = phone();
            existingUser(phone, "Dilnoza Yusupova");

            // ⚠️ Band raqamga ham SMS ketadi: «bu raqam allaqachon
            // ro'yxatdan o'tgan» degan xato oqimdan olib tashlandi.
            call(SEND, Map.of("phone", phone)).andExpect(status().isOk());

            call(VERIFY, Map.of("phone", phone, "code", lastCode()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name_required").value(false))
                    .andExpect(jsonPath("$.access_token").isNotEmpty())
                    .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                    .andExpect(jsonPath("$.user.name").value("Dilnoza Yusupova"));
        }

        @Test
        @DisplayName("Ismsiz eski hisob ismini bir marta kiritadi va tuzaladi")
        void asksNameForNamelessAccount() throws Exception {
            String phone = phone();
            // Eski `/otp/verify` va Google orqali kirish shunday satr
            // qoldirardi: raqami bor, ismi yo'q.
            existingUser(phone, null);

            call(SEND, Map.of("phone", phone)).andExpect(status().isOk());
            call(VERIFY, Map.of("phone", phone, "code", lastCode()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name_required").value(true));

            call(COMPLETE, Map.of("phone", phone, "name", "Bekzod Aliyev"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.name").value("Bekzod Aliyev"));

            assertThat(userRepo.findByPhone(phone).orElseThrow().getName())
                    .as("ikkinchi hisob emas, o'shaning o'zi yangilansin")
                    .isEqualTo("Bekzod Aliyev");
            assertThat(userRepo.findAll().stream().filter(u -> phone.equals(u.getPhone())).count())
                    .isEqualTo(1);
        }
    }

    // ------------------------------------------------------------------ chegara

    @Nested
    @DisplayName("Chegaralar")
    class Boundaries {

        @Test
        @DisplayName("Kodsiz ism yuborib hisob yasab bo'lmaydi")
        void nameWithoutVerifiedPhone() throws Exception {
            String phone = phone();

            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PHONE_NOT_VERIFIED"));

            assertThat(userRepo.findByPhone(phone)).isEmpty();
        }

        @Test
        @DisplayName("Bitta tasdiqlash — bitta hisob: ikkinchi so'rov rad etiladi")
        void verifiedMarkIsConsumedOnce() throws Exception {
            String phone = phone();

            call(SEND, Map.of("phone", phone)).andExpect(status().isOk());
            call(VERIFY, Map.of("phone", phone, "code", lastCode())).andExpect(status().isOk());
            call(COMPLETE, Map.of("phone", phone, "name", "Aziz Karimov"))
                    .andExpect(status().isOk());

            call(COMPLETE, Map.of("phone", phone, "name", "Boshqa Odam"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PHONE_NOT_VERIFIED"));

            assertThat(userRepo.findByPhone(phone).orElseThrow().getName())
                    .isEqualTo("Aziz Karimov");
        }

        @Test
        @DisplayName("Noto'g'ri kod kirgizmaydi")
        void wrongCodeRejected() throws Exception {
            String phone = phone();

            call(SEND, Map.of("phone", phone)).andExpect(status().isOk());

            String wrong = lastCode().equals("0000") ? "1111" : "0000";
            call(VERIFY, Map.of("phone", phone, "code", wrong))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("OTP_INVALID"));
        }
    }
}
