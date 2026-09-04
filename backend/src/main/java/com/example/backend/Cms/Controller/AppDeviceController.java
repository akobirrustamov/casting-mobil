package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.UserDevice;
import com.example.backend.Cms.Service.DeviceService;
import com.example.backend.Entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * «Mening qurilmalarim» — foydalanuvchining o'zi boshqaradi.
 *
 * <h2>⚠️ Qaysi nosozlik tuzatilyapti</h2>
 * Buyurtmachi talabi — bitta hisobdan 2 tadan ortiq qurilma bo'lmasin —
 * bazada ({@code cms_user_device}), sozlamada
 * ({@code account.device.limit}) va admin panelda bor edi. Faqat bitta
 * narsa yo'q edi: <b>ilova qurilmani ro'yxatga olmasdi</b>. Ya'ni limit
 * yozilgan, lekin hech qachon ishlamagan.
 *
 * Bundan tashqari qurilmani chiqarish faqat adminda bor edi. Odam o'z
 * hisobidan begona telefonni o'zi chiqara olmasdi — panelga kira
 * olmaydigan foydalanuvchi uchun bu «hech qanday yo'l yo'q» degani.
 *
 * <h2>Nima uchun kirish oqimidan keyin ro'yxatga olinadi</h2>
 * Limit kirishning O'ZIDA tekshirilsa, rad javobi token bermasdi va
 * ilova qurilmalar ro'yxatini so'ray olmasdi — odam «limitga
 * yetdingiz» degan xabarni ko'rardi-yu, qaysi qurilmani chiqarishni
 * bilmasdi.
 *
 * Shuning uchun tartib boshqacha: kirish token beradi, keyin ilova
 * qurilmani ro'yxatga olishga uradi. {@code 409} kelsa ilova o'zini
 * KIRGAN deb hisoblamaydi — tokenni saqlaydi, lekin qurilma tanlash
 * ekranini ko'rsatadi va bo'sh o'rin paydo bo'lgach davom etadi.
 *
 * Haqiqiy qo'riqchi esa boshqa joyda: chiqarilgan qurilmada refresh
 * ishlamaydi ({@code RefreshTokenService.ensureDeviceActive}). Ya'ni
 * ilova bu qadamni chetlab o'tsa ham, sessiya uzoq yashamaydi.
 *
 * <h2>Har javob — TO'LIQ ro'yxat</h2>
 * {@code FavoriteController} dagi bilan bir xil qoida: qo'shish ham,
 * chiqarish ham yangilangan ro'yxatni qaytaradi. Klient o'z nusxasini
 * o'zi hisoblab yurmaydi va bitta yo'qolgan javob ikki tomonni
 * ajratib yubormaydi.
 */
@RestController
@RequestMapping("/api/v1/app/devices")
@RequiredArgsConstructor
public class AppDeviceController {

    private final DeviceService deviceService;

    /** Mening faol qurilmalarim. */
    @GetMapping
    public ResponseEntity<DevicesResponse> list(HttpServletRequest request) {
        User user = CurrentUser.get();
        return ResponseEntity.ok(response(user, DeviceService.deviceIdOf(request)));
    }

    /**
     * Qurilmani ro'yxatga oladi.
     *
     * Ilova buni kirgandan keyin VA har ochilishida chaqiradi: birinchisi
     * limitni qo'llaydi, ikkinchisi «oxirgi faollik» ni yangilaydi. Takroriy
     * chaqiruv xavfsiz — tanish qurilma uchun hech narsa o'zgarmaydi.
     *
     * ⚠️ Limit oshsa {@code 409 DEVICE_LIMIT_REACHED}. Klient bu holatda
     * {@code GET} bilan ro'yxatni so'raydi va odamga qaysi birini
     * chiqarishni taklif qiladi.
     */
    @PostMapping("/register")
    public ResponseEntity<DevicesResponse> register(@RequestBody(required = false) RegisterRequest body,
                                                     HttpServletRequest request) {
        User user = CurrentUser.get();

        // Sarlavha ustun: u har so'rovda yuboriladi va token berishda ham
        // aynan shu qiymat yozilgan. Tana — sarlavhani qo'sha olmaydigan
        // klientlar uchun zaxira yo'l.
        String header = DeviceService.deviceIdOf(request);
        String deviceId = header != null ? header : (body == null ? null : body.getDeviceId());

        deviceService.register(user.getId(), deviceId,
                body == null ? null : body.getName(),
                body == null ? null : body.getPlatform());

        return ResponseEntity.ok(response(user, deviceId));
    }

    /**
     * O'z qurilmamni chiqarish.
     *
     * ⚠️ Joriy qurilmani ham chiqarish mumkin — bu «chiqish» degani va
     * taqiqlash uchun sabab yo'q. Ilova javobdagi ro'yxatda o'zini
     * ko'rmasa, foydalanuvchini kirish ekraniga qaytaradi.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DevicesResponse> revoke(@PathVariable Long id,
                                                   HttpServletRequest request) {
        User user = CurrentUser.get();
        deviceService.revoke(user.getId(), id, user.getId());
        return ResponseEntity.ok(response(user, DeviceService.deviceIdOf(request)));
    }

    // ------------------------------------------------------------------ DTO

    private DevicesResponse response(User user, String currentDeviceId) {
        List<UserDevice> active = deviceService.active(user.getId());

        List<DeviceDto> devices = active.stream()
                .map(d -> DeviceDto.from(d, currentDeviceId))
                .toList();

        // ⚠️ `limit` — SHU klientning chelagi uchun. Brauzer va telefon
        // alohida sanaladi (buyurtmachi qarori), ya'ni bitta umumiy son
        // ikkalasi uchun ham yolg'on bo'lardi.
        //
        // Joriy qurilma ro'yxatda bo'lmasligi mumkin — u hali ro'yxatga
        // olinmagan bo'lishi mumkin. Bunda mobil chegarasi olinadi:
        // ilova bu endpointning asosiy chaqiruvchisi.
        String platform = active.stream()
                .filter(d -> d.getDeviceId().equals(currentDeviceId))
                .map(UserDevice::getPlatform)
                .findFirst()
                .orElse(null);

        return DevicesResponse.builder()
                .limit(deviceService.limitFor(platform))
                .devices(devices)
                .build();
    }

    @Data
    public static class RegisterRequest {
        private String deviceId;
        /** Odam taniydigan nom: «iPhone 13», «Samsung A54». */
        private String name;
        /** {@code ios} / {@code android} / {@code web}. */
        private String platform;
    }

    @Data
    @Builder
    public static class DevicesResponse {
        /**
         * Hozirgi chegara — ro'yxat ustidagi «2 tadan 2 tasi» yozuvi uchun.
         *
         * ⚠️ Klientda qotirilmaydi: sozlama admin paneldan o'zgaradi va
         * ilova yangilanishini kutmasligi kerak.
         */
        private int limit;
        private List<DeviceDto> devices;
    }

    @Data
    @Builder
    public static class DeviceDto {
        /** Chiqarishda ishlatiladigan qator identifikatori. */
        private Long id;
        private String name;
        private String platform;
        private LocalDateTime lastActiveAt;
        private LocalDateTime createdAt;

        /**
         * Shu qurilmami.
         *
         * ⚠️ Server hal qiladi, klient emas: ro'yxatda o'z qurilmasining
         * qaysi ekanini bilmagan odam noto'g'risini chiqarib, o'zini
         * tizimdan uzib qo'yardi.
         */
        private boolean current;

        static DeviceDto from(UserDevice device, String currentDeviceId) {
            return DeviceDto.builder()
                    .id(device.getId())
                    .name(device.getDeviceName())
                    .platform(device.getPlatform())
                    .lastActiveAt(device.getLastActiveAt())
                    .createdAt(device.getCreatedAt())
                    .current(currentDeviceId != null
                            && currentDeviceId.equals(device.getDeviceId()))
                    .build();
        }
    }
}
