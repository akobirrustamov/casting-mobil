package com.example.backend.Cms;

import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.NotificationTranslation;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Entity.UserDevice;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.NotificationAudience;
import com.example.backend.Cms.Enums.NotificationStatus;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.NotificationRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Repository.UserDeviceRepo;
import com.example.backend.Cms.Service.DeviceService;
import com.example.backend.Cms.Service.NotificationAdminService;
import com.example.backend.Cms.Service.Push.NotificationPushService;
import com.example.backend.Cms.Service.Push.PushGateway;
import com.example.backend.Cms.Service.Push.PushGateway.PushMessage;
import com.example.backend.Cms.Service.Push.PushGateway.PushResult;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Push bildirishnomalar — token saqlash va Expo orqali yuborish.
 *
 * Expo o'rniga soxta {@link PushGateway}: testdan haqiqiy {@code exp.host}
 * ga so'rov ketmaydi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationPushTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mockMvc;
    @Autowired private NotificationPushService pushService;
    @Autowired private NotificationAdminService notificationService;
    @Autowired private DeviceService deviceService;
    @Autowired private NotificationRepo notificationRepo;
    @Autowired private UserDeviceRepo deviceRepo;
    @Autowired private UserAccountRepo accountRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private JwtService jwtService;

    @MockBean private PushGateway gateway;

    @BeforeEach
    void enableGateway() {
        when(gateway.isEnabled()).thenReturn(true);
        // Sukut: hammasi qabul qilindi.
        when(gateway.send(anyList())).thenAnswer(inv -> {
            List<PushMessage> msgs = inv.getArgument(0);
            return msgs.stream().map(m -> PushResult.accepted()).toList();
        });
    }

    // ------------------------------------------------------------- token

    @Nested
    @DisplayName("Tokenni saqlash")
    class TokenRegistration {

        @Test
        @DisplayName("Joriy qurilmaga yoziladi")
        void savedOnCurrentDevice() throws Exception {
            User u = user(null);
            UserDevice d = device(u, null);

            mockMvc.perform(put("/api/v1/app/devices/push-token")
                            .header("Authorization", token(u))
                            .header(DeviceService.DEVICE_HEADER, d.getDeviceId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"ExponentPushToken[abc-" + d.getId() + "]\"}"))
                    .andExpect(status().isNoContent());

            assertThat(deviceRepo.findById(d.getId()).orElseThrow().getPushToken())
                    .isEqualTo("ExponentPushToken[abc-" + d.getId() + "]");
        }

        @Test
        @DisplayName("⚠️ Telefonda boshqa odam kirsa, token avvalgisidan olinadi")
        void tokenMovesToNewOwner() {
            String tok = "ExponentPushToken[shared-" + SEQ.incrementAndGet() + "]";
            User first = user(null);
            UserDevice firstDevice = device(first, tok);

            User second = user(null);
            UserDevice secondDevice = device(second, null);
            deviceService.savePushToken(second.getId(), secondDevice.getDeviceId(), tok);

            // Aks holda birinchi odamga mo'ljallangan xabar ikkinchisining
            // telefoniga chiqardi.
            assertThat(deviceRepo.findById(firstDevice.getId()).orElseThrow().getPushToken()).isNull();
            assertThat(deviceRepo.findById(secondDevice.getId()).orElseThrow().getPushToken()).isEqualTo(tok);
        }

        @Test
        @DisplayName("Chiqishda token o'chiriladi")
        void clearedOnLogout() throws Exception {
            User u = user(null);
            UserDevice d = device(u, "ExponentPushToken[out-" + SEQ.incrementAndGet() + "]");

            mockMvc.perform(delete("/api/v1/app/devices/push-token")
                            .header("Authorization", token(u))
                            .header(DeviceService.DEVICE_HEADER, d.getDeviceId()))
                    .andExpect(status().isNoContent());

            assertThat(deviceRepo.findById(d.getId()).orElseThrow().getPushToken()).isNull();
        }

        @Test
        @DisplayName("Qurilma chiqarilsa token ham ketadi")
        void revokedDeviceLosesToken() {
            User u = user(null);
            UserDevice d = device(u, "ExponentPushToken[rev-" + SEQ.incrementAndGet() + "]");

            deviceService.revoke(u.getId(), d.getId(), u.getId());

            assertThat(deviceRepo.findById(d.getId()).orElseThrow().getPushToken()).isNull();
        }
    }

    // ---------------------------------------------------------- yuborish

    @Nested
    @DisplayName("Yuborish")
    class Delivery {

        @Test
        @DisplayName("Foydalanuvchi o'z tilida oladi, havola ma'lumotda")
        void userGetsOwnLanguage() {
            User ru = user(Locale.RU);
            String tok = "ExponentPushToken[ru-" + SEQ.incrementAndGet() + "]";
            device(ru, tok);
            Notification n = sent(NotificationAudience.ALL);

            pushService.deliver(n.getId());

            PushMessage m = messageFor(tok);
            assertThat(m.title()).isEqualTo("Sarlavha RU");
            assertThat(m.data()).containsEntry("notificationId", n.getId());
        }

        @Test
        @DisplayName("PREMIUM_ONLY — premiumsizga push ketmaydi")
        void audienceIsRespected() {
            String plainTok = "ExponentPushToken[plain-" + SEQ.incrementAndGet() + "]";
            device(user(null), plainTok);
            String premiumTok = "ExponentPushToken[prem-" + SEQ.incrementAndGet() + "]";
            User premium = user(Locale.UZ);
            UserAccount acc = accountRepo.findByUserId(premium.getId()).orElseThrow();
            acc.setPremiumUntil(LocalDateTime.now().plusDays(10));
            accountRepo.save(acc);
            device(premium, premiumTok);

            pushService.deliver(sent(NotificationAudience.PREMIUM_ONLY).getId());

            List<String> tokens = sentMessages().stream().map(PushMessage::token).toList();
            assertThat(tokens).contains(premiumTok).doesNotContain(plainTok);
        }

        @Test
        @DisplayName("O'chirilgan hisob va nofaol qurilmaga ketmaydi")
        void deletedAndRevokedAreSkipped() {
            User deleted = user(Locale.UZ);
            UserAccount acc = accountRepo.findByUserId(deleted.getId()).orElseThrow();
            acc.setStatus(UserStatus.DELETED);
            accountRepo.save(acc);
            String deletedTok = "ExponentPushToken[del-" + SEQ.incrementAndGet() + "]";
            device(deleted, deletedTok);

            String inactiveTok = "ExponentPushToken[off-" + SEQ.incrementAndGet() + "]";
            UserDevice off = device(user(null), inactiveTok);
            off.setActive(false);
            deviceRepo.save(off);

            // Kamida bitta oluvchi bo'lsin — aks holda `send` chaqirilmaydi.
            String okTok = "ExponentPushToken[ok-" + SEQ.incrementAndGet() + "]";
            device(user(null), okTok);

            pushService.deliver(sent(NotificationAudience.ALL).getId());

            List<String> tokens = sentMessages().stream().map(PushMessage::token).toList();
            assertThat(tokens).contains(okTok).doesNotContain(deletedTok, inactiveTok);
        }

        @Test
        @DisplayName("Natija xabarga yoziladi va hisobotda chiqadi")
        void resultIsRecorded() {
            String tok = "ExponentPushToken[res-" + SEQ.incrementAndGet() + "]";
            device(user(null), tok);
            Notification n = sent(NotificationAudience.ALL);

            NotificationPushService.Result r = pushService.deliver(n.getId());

            var report = notificationService.report(n.getId());
            assertThat(report.getSent().getAvailable()).isTrue();
            assertThat(report.getSent().getValue()).isEqualTo(r.accepted());
            assertThat(report.getFailed().getValue()).isEqualTo(r.failed());
        }

        @Test
        @DisplayName("⚠️ «DeviceNotRegistered» — token bazadan tozalanadi")
        void goneTokensAreCleared() {
            String tok = "ExponentPushToken[gone-" + SEQ.incrementAndGet() + "]";
            UserDevice d = device(user(null), tok);
            when(gateway.send(anyList())).thenAnswer(inv -> {
                List<PushMessage> msgs = inv.getArgument(0);
                return msgs.stream()
                        .map(m -> m.token().equals(tok)
                                ? new PushResult(false, true, "DeviceNotRegistered")
                                : PushResult.accepted())
                        .toList();
            });

            pushService.deliver(sent(NotificationAudience.ALL).getId());

            assertThat(deviceRepo.findById(d.getId()).orElseThrow().getPushToken()).isNull();
        }

        @Test
        @DisplayName("Push o'chirilgan bo'lsa hech narsa yuborilmaydi")
        void disabledSendsNothing() {
            when(gateway.isEnabled()).thenReturn(false);
            device(user(null), "ExponentPushToken[dis-" + SEQ.incrementAndGet() + "]");

            assertThat(pushService.deliver(sent(NotificationAudience.ALL).getId())).isNull();
            verify(gateway, never()).send(anyList());
        }
    }

    // ---------------------------------------------------------- yordamchi

    @SuppressWarnings("unchecked")
    private List<PushMessage> sentMessages() {
        ArgumentCaptor<List<PushMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(gateway).send(captor.capture());
        return captor.getValue();
    }

    private PushMessage messageFor(String token) {
        return sentMessages().stream()
                .filter(m -> m.token().equals(token))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Token uchun xabar yo'q: " + token));
    }

    private Notification sent(NotificationAudience audience) {
        Notification n = Notification.builder()
                .status(NotificationStatus.SENT)
                .audience(audience)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        for (Locale l : List.of(Locale.UZ, Locale.RU, Locale.EN)) {
            n.getTranslations().add(NotificationTranslation.builder()
                    .notification(n)
                    .locale(l)
                    .title("Sarlavha " + l.name())
                    .body("Matn " + l.name())
                    .build());
        }
        return notificationRepo.save(n);
    }

    private UserDevice device(User u, String pushToken) {
        return deviceRepo.save(UserDevice.builder()
                .user(u)
                .deviceId("dev-" + SEQ.incrementAndGet())
                .platform("android")
                .pushToken(pushToken)
                .build());
    }

    /** @param language {@code null} — hisob (UserAccount) yaratilmaydi */
    private User user(Locale language) {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        User u = new User();
        int n = SEQ.incrementAndGet();
        u.setPhone("+99891" + (7300000 + n));
        u.setPassword("xesh-" + n);
        u.setName("Push " + n);
        u.setRoles(new ArrayList<>(List.of(role)));
        u = userRepo.save(u);
        if (language != null) {
            accountRepo.save(UserAccount.builder()
                    .user(u)
                    .language(language)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return u;
    }

    private String token(User u) {
        return "Bearer " + jwtService.generateJwtToken(u);
    }
}
