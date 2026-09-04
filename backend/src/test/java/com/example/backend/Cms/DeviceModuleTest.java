package com.example.backend.Cms;

import com.example.backend.Cms.Entity.UserDevice;
import com.example.backend.Cms.Repository.UserDeviceRepo;
import com.example.backend.Cms.Service.DeviceService;
import com.example.backend.Entity.RefreshToken;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RefreshTokenRepo;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Services.AuthService.RefreshTokenService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Qurilma chegarasi — buyurtmachi talabi: bitta hisobdan 2 tadan ortiq emas.
 *
 * <h2>⚠️ Qaysi nosozlik qo'riqlanadi</h2>
 * Model, sozlama ({@code account.device.limit}), admin ro'yxati va hatto
 * limit mantiqining o'zi ham yozilgan edi. Bitta narsa yetishmasdi:
 * {@code registerDevice()} ni <b>hech kim chaqirmasdi</b>. Ya'ni talab
 * bajarilgandek ko'rinardi, amalda esa istalgancha qurilmadan kirish
 * mumkin edi.
 *
 * Bunday nosozlikni faqat testda tutish mumkin: kodni o'qib chiqqan odam
 * limitni ko'radi va u ishlaydi deb o'ylaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeviceModuleTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final String DEVICES = "/api/v1/app/devices";
    private static final String REGISTER = DEVICES + "/register";

    @Autowired private MockMvc mockMvc;
    @Autowired private DeviceService deviceService;
    @Autowired private UserDeviceRepo deviceRepo;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private RefreshTokenRepo refreshTokenRepo;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;

    // ---------------------------------------------------------------- limit

    @Nested
    @DisplayName("Chegara")
    class Limit {

        @Test
        @DisplayName("Uchinchi qurilma rad etiladi")
        void thirdDeviceRejected() {
            User u = user();
            deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            deviceService.register(u.getId(), "qurilma-2", "Samsung", "android");

            assertThatThrownBy(() ->
                    deviceService.register(u.getId(), "qurilma-3", "Redmi", "android"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("DEVICE_LIMIT_REACHED");
        }

        /**
         * ⚠️ Ilova qurilmani HAR OCHILISHIDA ro'yxatdan o'tkazadi.
         * Takroriy chaqiruv o'rin band qilsa, ikkinchi ochilishda odam
         * o'z telefonidan chiqib qolardi.
         */
        @Test
        @DisplayName("Tanish qurilmani qayta ro'yxatga olish o'rin egallamaydi")
        void reRegisterIsIdempotent() {
            User u = user();
            deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");

            assertThat(deviceService.active(u.getId())).hasSize(1);

            // Ikkinchi qurilma uchun o'rin hali ham bor.
            deviceService.register(u.getId(), "qurilma-2", "Samsung", "android");
            assertThat(deviceService.active(u.getId())).hasSize(2);
        }

        @Test
        @DisplayName("Chiqarilgan qurilma o'rin bo'shatadi")
        void revokeFreesSlot() {
            User u = user();
            UserDevice first = deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            deviceService.register(u.getId(), "qurilma-2", "Samsung", "android");

            deviceService.revoke(u.getId(), first.getId(), u.getId());

            // Endi uchinchisi kira oladi — aynan shu odam limitga
            // yetganda qiladigan harakat.
            deviceService.register(u.getId(), "qurilma-3", "Redmi", "android");
            assertThat(deviceService.active(u.getId())).hasSize(2);
        }

        /**
         * ⚠️ Ko'chirishdan oldingi kod chiqarilgan qurilmani shartsiz
         * {@code active = true} qilardi. Ya'ni chiqarib yuborilgan
         * telefon ilovani qayta ochishi bilan o'zini tiklab olardi va
         * «chiqarish» hech qanday ma'no bermasdi.
         */
        @Test
        @DisplayName("Chiqarilgan qurilma o'zini tiklab ololmaydi")
        void revokedDeviceCannotSneakBack() {
            User u = user();
            UserDevice first = deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            deviceService.register(u.getId(), "qurilma-2", "Samsung", "android");

            deviceService.revoke(u.getId(), first.getId(), u.getId());
            // Bo'shagan o'rinni boshqa qurilma egalladi.
            deviceService.register(u.getId(), "qurilma-3", "Redmi", "android");

            assertThatThrownBy(() ->
                    deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("DEVICE_LIMIT_REACHED");
        }

        /**
         * ⚠️ Buyurtmachi qarori: veb alohida sanaladi.
         *
         * Aks holda kompyuterda saytni ochish odamning telefonini
         * chiqarib yuborardi — u esa hech qanday qurilma qo'shmagandek
         * his qilardi: u shunchaki brauzerga kirdi.
         */
        @Test
        @DisplayName("Brauzer telefon o'rnini egallamaydi")
        void webDoesNotConsumeMobileSlots() {
            User u = user();
            deviceService.register(u.getId(), "telefon-1", "iPhone", "ios");
            deviceService.register(u.getId(), "telefon-2", "Samsung", "android");

            // Mobil chelak to'lgan, lekin brauzer boshqa chelakda.
            deviceService.register(u.getId(), "brauzer-1", "Chrome", "web");

            assertThat(deviceService.active(u.getId())).hasSize(3);
        }

        @Test
        @DisplayName("Brauzerlar ham o'z chegarasiga ega")
        void webHasItsOwnLimit() {
            User u = user();
            deviceService.register(u.getId(), "brauzer-1", "Chrome", "web");
            deviceService.register(u.getId(), "brauzer-2", "Firefox", "web");

            assertThatThrownBy(() ->
                    deviceService.register(u.getId(), "brauzer-3", "Safari", "web"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("DEVICE_LIMIT_REACHED");
        }

        @Test
        @DisplayName("Telefon brauzer o'rnini egallamaydi")
        void mobileDoesNotConsumeWebSlots() {
            User u = user();
            deviceService.register(u.getId(), "brauzer-1", "Chrome", "web");
            deviceService.register(u.getId(), "brauzer-2", "Firefox", "web");

            deviceService.register(u.getId(), "telefon-1", "iPhone", "ios");

            assertThat(deviceService.active(u.getId())).hasSize(3);
        }
        @Test
        @DisplayName("Chegara sozlamadan olinadi, koddan emas")
        void limitComesFromSettings() {
            // Sozlama o'qilmasa buyurtmachi aytgan 2 ishlaydi.
            assertThat(deviceService.limit()).isEqualTo(2);
        }
    }

    // -------------------------------------------------------------- sessiya

    @Nested
    @DisplayName("Sessiya")
    class Sessions {

        /**
         * ⚠️ Eng muhim tekshiruv: ilgari chiqarish faqat yozuvni
         * nofaol qilardi va o'sha telefondagi token o'z muddatigacha
         * ishlayverardi.
         */
        @Test
        @DisplayName("Qurilma chiqarilganda uning tokeni ham bekor qilinadi")
        void revokeKillsDeviceToken() {
            User u = user();
            UserDevice device = deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            String token = refreshTokenService.issue(u, requestFrom("qurilma-1"));

            deviceService.revoke(u.getId(), device.getId(), u.getId());

            RefreshToken row = refreshTokenRepo.findById(jwtService.jtiOf(token)).orElseThrow();
            assertThat(row.getRevokedAt()).isNotNull();
        }

        /**
         * ⚠️ Odam eski telefonini chiqarganda qo'lidagisidan chiqib
         * ketmasligi kerak — aks holda «tartibga solaman» degan harakat
         * o'zini jazolash bo'lardi.
         */
        @Test
        @DisplayName("Boshqa qurilmalarning sessiyasiga tegilmaydi")
        void revokeSparesOtherDevices() {
            User u = user();
            UserDevice old = deviceService.register(u.getId(), "eski", "iPhone 8", "ios");
            deviceService.register(u.getId(), "yangi", "iPhone 15", "ios");

            String keep = refreshTokenService.issue(u, requestFrom("yangi"));
            deviceService.revoke(u.getId(), old.getId(), u.getId());

            RefreshToken row = refreshTokenRepo.findById(jwtService.jtiOf(keep)).orElseThrow();
            assertThat(row.getRevokedAt()).isNull();
        }

        @Test
        @DisplayName("Chiqarilgan qurilmada yangilash ishlamaydi")
        void rotateRejectsRevokedDevice() {
            User u = user();
            UserDevice device = deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            String token = refreshTokenService.issue(u, requestFrom("qurilma-1"));

            // Yozuvni nofaol qilamiz, lekin tokenga TEGMAYMIZ — bu
            // migratsiyagacha berilgan tokenlar va boshqa yo'llar bilan
            // nofaol qilingan qurilmalar holati.
            device.setActive(false);
            deviceRepo.save(device);

            assertThatThrownBy(() -> refreshTokenService.rotate(token, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo("DEVICE_REVOKED");
        }

        /**
         * ⚠️ V32 dan oldin berilgan tokenlarda qurilma noma'lum.
         * Ularni rad etish migratsiya kunida hamma foydalanuvchini
         * tizimdan chiqarib yuborardi.
         */
        @Test
        @DisplayName("Qurilmasi noma'lum eski token ishlayveradi")
        void rotateAllowsTokenWithoutDevice() {
            User u = user();
            String token = refreshTokenService.issue(u, null);

            assertThat(refreshTokenService.rotate(token, null)).isEqualTo(u.getId());
        }
    }

    // ------------------------------------------------------------- egalik

    @Nested
    @DisplayName("Egalik")
    class Ownership {

        @Test
        @DisplayName("O'zganing qurilmasini chiqarib bo'lmaydi")
        void cannotRevokeSomeoneElsesDevice() {
            User owner = user();
            User stranger = user();
            UserDevice device = deviceService.register(owner.getId(), "qurilma-1", "iPhone", "ios");

            // Begona odam o'z hisobi orqali murojaat qiladi — qator
            // boshqa foydalanuvchiniki bo'lgani uchun rad etiladi.
            assertThatThrownBy(() ->
                    deviceService.revoke(stranger.getId(), device.getId(), stranger.getId()))
                    .isInstanceOf(BusinessException.class);

            assertThat(deviceRepo.findById(device.getId()).orElseThrow().getActive()).isTrue();
        }

        @Test
        @DisplayName("Qurilmalar faqat o'z hisobida ko'rinadi")
        void listIsPerUser() {
            User first = user();
            User second = user();
            deviceService.register(first.getId(), "qurilma-1", "iPhone", "ios");

            assertThat(deviceService.active(second.getId())).isEmpty();
        }
    }

    // ------------------------------------------------------------ endpoint

    @Nested
    @DisplayName("Endpoint")
    class Endpoints {

        @Test
        @DisplayName("Ro'yxatga olish va ro'yxat bitta shaklda qaytadi")
        void registerReturnsFullList() throws Exception {
            User u = user();

            mockMvc.perform(post(REGISTER)
                            .header("Authorization", token(u))
                            .header(DeviceService.DEVICE_HEADER, "qurilma-1")
                            .contentType("application/json")
                            .content("{\"name\":\"iPhone 13\",\"platform\":\"ios\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.limit").value(2))
                    .andExpect(jsonPath("$.devices.length()").value(1))
                    .andExpect(jsonPath("$.devices[0].name").value("iPhone 13"))
                    // Server o'zi belgilaydi: odam qaysi qatorni
                    // chiqarayotganini bilishi kerak.
                    .andExpect(jsonPath("$.devices[0].current").value(true));
        }

        @Test
        @DisplayName("Limitga yetganda 409 va aniq kod qaytadi")
        void limitReturnsConflict() throws Exception {
            User u = user();
            deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            deviceService.register(u.getId(), "qurilma-2", "Samsung", "android");

            mockMvc.perform(post(REGISTER)
                            .header("Authorization", token(u))
                            .header(DeviceService.DEVICE_HEADER, "qurilma-3")
                            .contentType("application/json")
                            .content("{\"name\":\"Redmi\",\"platform\":\"android\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DEVICE_LIMIT_REACHED"));
        }

        /**
         * ⚠️ Rad javobidan KEYIN ham ro'yxat ochiq bo'lishi shart:
         * ilova aynan shu ro'yxatdan «qaysi birini chiqaramiz?» degan
         * ekranni yasaydi.
         */
        @Test
        @DisplayName("Limitdan keyin ro'yxat baribir o'qiladi")
        void listReadableAfterLimit() throws Exception {
            User u = user();
            deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            deviceService.register(u.getId(), "qurilma-2", "Samsung", "android");

            mockMvc.perform(get(DEVICES)
                            .header("Authorization", token(u))
                            .header(DeviceService.DEVICE_HEADER, "qurilma-3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.devices.length()").value(2))
                    // Hech biri joriy emas — bu qurilma hali ro'yxatda yo'q.
                    .andExpect(jsonPath("$.devices[0].current").value(false));
        }

        @Test
        @DisplayName("Chiqarish yangilangan ro'yxatni qaytaradi")
        void deleteReturnsUpdatedList() throws Exception {
            User u = user();
            UserDevice first = deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            deviceService.register(u.getId(), "qurilma-2", "Samsung", "android");

            mockMvc.perform(delete(DEVICES + "/" + first.getId())
                            .header("Authorization", token(u))
                            .header(DeviceService.DEVICE_HEADER, "qurilma-2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.devices.length()").value(1))
                    .andExpect(jsonPath("$.devices[0].current").value(true));
        }

        @Test
        @DisplayName("Token yo'q bo'lsa 401")
        void anonymousRejected() throws Exception {
            mockMvc.perform(get(DEVICES))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ------------------------------------------------------------ faollik

    @Nested
    @DisplayName("Oxirgi faollik")
    class LastActive {

        /**
         * ⚠️ Ilova qurilmani har ochilishida yuboradi. Har safar
         * yozilsa, jadval bekorga yangilanib turardi — ro'yxatda esa
         * «bugun» bilan «3 soat oldin» farqi ko'rinmaydi.
         */
        @Test
        @DisplayName("Sutka ichida qayta yozilmaydi")
        void notWrittenMoreThanOncePerDay() {
            User u = user();
            UserDevice device = deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");
            LocalDateTime first = device.getLastActiveAt();

            deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");

            assertThat(deviceRepo.findById(device.getId()).orElseThrow().getLastActiveAt())
                    .isEqualTo(first);
        }

        @Test
        @DisplayName("Sutkadan keyin yangilanadi")
        void writtenAfterADay() {
            User u = user();
            UserDevice device = deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");

            LocalDateTime stale = LocalDateTime.now().minusDays(3);
            device.setLastActiveAt(stale);
            deviceRepo.save(device);

            deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");

            assertThat(deviceRepo.findById(device.getId()).orElseThrow().getLastActiveAt())
                    .isAfter(stale);
        }

        /**
         * Telefon nomi o'zgarishi mumkin. Ro'yxatda eskisi tursa, odam
         * qaysi qatorni chiqarayotganini bilmasdi.
         */
        @Test
        @DisplayName("Nom o'zgarsa darhol yangilanadi")
        void nameUpdatesImmediately() {
            User u = user();
            UserDevice device = deviceService.register(u.getId(), "qurilma-1", "iPhone", "ios");

            deviceService.register(u.getId(), "qurilma-1", "Ali telefoni", "ios");

            assertThat(deviceRepo.findById(device.getId()).orElseThrow().getDeviceName())
                    .isEqualTo("Ali telefoni");
        }
    }

    // ---------------------------------------------------------- yordamchi

    private User user() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99890" + (9300000 + n));
        u.setPassword("xesh-" + n);
        u.setName("Tomoshabin " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        return userRepo.save(u);
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }

    /** Qurilma sarlavhasi qo'yilgan so'rov — token berishda ishlatiladi. */
    private MockHttpServletRequest requestFrom(String deviceId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(DeviceService.DEVICE_HEADER, deviceId);
        return request;
    }
}
