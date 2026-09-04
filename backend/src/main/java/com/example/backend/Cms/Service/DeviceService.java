package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.UserDevice;
import com.example.backend.Cms.Repository.UserDeviceRepo;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Services.AuthService.RefreshTokenService;
import com.example.backend.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Qurilma qoidalarining YAGONA uyi.
 *
 * <h2>⚠️ Qaysi nosozlik tuzatilyapti</h2>
 * Buyurtmachi «bitta hisobdan 2 tadan ortiq qurilma bo'lmasin» degan va
 * kod ham yozilgan edi — {@code UserAdminService.registerDevice()}.
 * Lekin u <b>hech qayerdan chaqirilmasdi</b>: ilova kirganda qurilmani
 * ro'yxatga olmasdi. Ya'ni limit bazada, sozlamada va admin panelda bor
 * edi, amalda esa yo'q — istalgancha qurilmadan kirish mumkin edi.
 *
 * Metod {@code UserAdminService} dan SHU YERGA ko'chirildi. U yerda
 * turishi noto'g'ri edi: qurilmani ro'yxatga oladigan tomon — mobil
 * ilova, admin emas. Admin xizmati endi shu yerga murojaat qiladi,
 * ya'ni qoida ikki nusxada emas.
 *
 * <h2>Nima uchun limit kodda emas</h2>
 * {@code account.device.limit} — {@code cms_platform_setting} da.
 * Buyurtmachi 2 dan 3 ga o'tkazmoqchi bo'lsa deploy kutmaydi.
 */
@Service
@RequiredArgsConstructor
public class DeviceService {

    /**
     * Klient qurilmasini bildiradigan sarlavha.
     *
     * <h2>Nima uchun sarlavha, tanadagi maydon emas</h2>
     * Qurilma HAR SO'ROVDA kerak: token berishda ham
     * ({@code RefreshTokenService.issue}), ro'yxatda «bu qurilma» ni
     * belgilashda ham. Uni har bir so'rov tanasiga qo'shish har bir DTO
     * ga bir xil maydonni yozishni anglatardi.
     *
     * ⚠️ Bu foydalanuvchini aniqlamaydi va unga ISHONILMAYDI: qurilma
     * har doim tokendagi egasi bilan birga qaraladi. Begona qiymat
     * yuborish faqat o'z hisobidagi qurilmalar ro'yxatiga ta'sir qiladi.
     */
    public static final String DEVICE_HEADER = "X-Device-Id";

    /**
     * «Oxirgi faollik» ni qanchada bir yangilash.
     *
     * ⚠️ Har so'rovda yozish mumkin emas: ilova qurilmani har ochilishda
     * ro'yxatdan o'tkazadi va har qadamda {@code update} yuborilardi.
     * Ro'yxatda esa «bugun» bilan «3 soat oldin» orasidagi farq odamga
     * hech narsa bermaydi — «kecha» bilan «bir hafta oldin» beradi.
     */
    private static final Duration TOUCH_INTERVAL = Duration.ofHours(24);

    private static final int MAX_DEVICE_ID = 128;
    private static final int MAX_NAME = 255;
    private static final int MAX_PLATFORM = 32;

    private final UserDeviceRepo deviceRepo;
    private final UserRepo userRepo;
    private final SettingsService settingsService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Brauzer platformasining nomi.
     *
     * ⚠️ Klient yuboradigan qiymat: mobil ilova {@code ios}/{@code android},
     * veb-klient {@code web} yuboradi. Ishonch darajasi past — lekin bu
     * yerda xavf yo'q: yolg'on qiymat faqat O'Z hisobidagi hisobni
     * o'zgartiradi va limitni CHETLAB O'TA olmaydi, chunki ikkala
     * chelakning ham o'z chegarasi bor.
     */
    private static final String WEB_PLATFORM = "web";

    /**
     * Qurilma qaysi chelakka tushadi.
     *
     * Ikki chelak: brauzerlar va qolgan hammasi. Buyurtmachi qarori —
     * veb alohida sanaladi.
     */
    private static boolean isWeb(String platform) {
        return WEB_PLATFORM.equalsIgnoreCase(platform == null ? null : platform.trim());
    }

    /** Mobil qurilmalar chegarasi. Sozlama o'qilmasa — buyurtmachi aytgan 2. */
    public int limit() {
        return settingsService.getInt(SettingKeys.DEVICE_LIMIT, 2);
    }

    /** Brauzerlar chegarasi — mobil bilan bir xil emas. */
    public int webLimit() {
        return settingsService.getInt(SettingKeys.DEVICE_LIMIT_WEB, 2);
    }

    /** Berilgan platforma uchun chegara. */
    public int limitFor(String platform) {
        return isWeb(platform) ? webLimit() : limit();
    }

    /** So'rovdan qurilma identifikatorini oladi. Yo'q bo'lsa {@code null}. */
    public static String deviceIdOf(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String raw = request.getHeader(DEVICE_HEADER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.length() > MAX_DEVICE_ID ? trimmed.substring(0, MAX_DEVICE_ID) : trimmed;
    }

    /**
     * Foydalanuvchining qurilmalari — yangi faollik birinchi.
     *
     * Chiqarilganlari ham qaytariladi: ular o'chirilmaydi, faqat
     * nofaol bo'ladi ({@code active = false}) va tarixda qoladi.
     * Kimga ko'rsatishni chaqiruvchi hal qiladi.
     */
    @Transactional(readOnly = true)
    public List<UserDevice> all(UUID userId) {
        return deviceRepo.findAllByUserIdOrderByLastActiveAtDesc(userId);
    }

    /** Faqat faollari — ilovaga shu ko'rsatiladi. */
    @Transactional(readOnly = true)
    public List<UserDevice> active(UUID userId) {
        return all(userId).stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .sorted(Comparator.comparing(UserDevice::getLastActiveAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Qurilmani ro'yxatga oladi yoki tanigan bo'lsa faolligini yangilaydi.
     *
     * <h2>Uch holat</h2>
     * <ul>
     *   <li><b>Tanish va faol</b> — hech narsa o'zgarmaydi, faqat
     *       {@code lastActiveAt} sutkada bir marta yangilanadi.</li>
     *   <li><b>Tanish, lekin chiqarilgan</b> — bu YANGI qurilmadek
     *       qaraladi va limit qaytadan tekshiriladi. ⚠️ Ilgari kod uni
     *       shartsiz {@code active = true} qilardi: ya'ni chiqarilgan
     *       qurilma ilovani qayta ochishi bilan o'zini tiklab olardi va
     *       «chiqarib yuborish» ma'nosini yo'qotardi.</li>
     *   <li><b>Yangi</b> — limit tekshiriladi.</li>
     * </ul>
     *
     * @throws BusinessException {@code DEVICE_LIMIT_REACHED} (409) —
     *         chaqiruvchi bu holatda foydalanuvchiga qurilmalar
     *         ro'yxatini ko'rsatishi va birini chiqarishni taklif
     *         qilishi kerak
     */
    @Transactional
    public UserDevice register(UUID userId, String deviceId, String name, String platform) {
        String id = required(deviceId);

        Optional<UserDevice> known = deviceRepo.findByUserIdAndDeviceId(userId, id);
        if (known.isPresent()) {
            UserDevice device = known.get();
            if (Boolean.TRUE.equals(device.getActive())) {
                return touch(device, name, platform);
            }
            // Chiqarilgan qurilma qaytib keldi — o'rin bo'shmi?
            // ⚠️ Chelak SAQLANGAN platforma bo'yicha: yangi so'rovda u
            // yuborilmagan bo'lishi mumkin.
            ensureRoom(userId, platform == null ? device.getPlatform() : platform);
            device.setActive(true);
            device.setLastActiveAt(LocalDateTime.now());
            applyLabels(device, name, platform);
            return deviceRepo.save(device);
        }

        ensureRoom(userId, platform);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        UserDevice fresh = UserDevice.builder()
                .user(user)
                .deviceId(id)
                .deviceName(trim(name, MAX_NAME))
                .platform(trim(platform, MAX_PLATFORM))
                .build();
        return deviceRepo.save(fresh);
    }

    /**
     * Qurilmani chiqarib yuboradi.
     *
     * <h2>⚠️ Nima uchun token ham bekor qilinadi</h2>
     * Ilgari bu metod faqat {@code active = false} qilardi. O'sha
     * telefondagi refresh token esa tegilmasdi va o'z muddatigacha
     * ishlayverardi — ya'ni «chiqarib yuborish» ekranda bajarilgandek
     * ko'rinardi, aslida chiqarilgan qurilma tomosha qilishda davom
     * etardi.
     *
     * @param requesterId kim so'rayapti — {@code null} bo'lsa admin
     *                    (u boshqaning qurilmasini chiqara oladi)
     */
    @Transactional
    public UserDevice revoke(UUID ownerId, Long deviceRowId, UUID requesterId) {
        UserDevice device = deviceRepo.findById(deviceRowId)
                .orElseThrow(() -> BusinessException.notFound("Device", deviceRowId));

        if (!device.getUser().getId().equals(ownerId)) {
            throw BusinessException.validation("Bu qurilma boshqa foydalanuvchiga tegishli");
        }
        if (requesterId != null && !requesterId.equals(ownerId)) {
            throw BusinessException.accessDenied("Faqat o'z qurilmangizni chiqarish mumkin");
        }

        device.setActive(false);

        // ⚠️ `saveAndFlush`, `save` emas — va bu didga bog'liq emas.
        //
        // Keyingi qator ommaviy JPQL `update` bajaradi
        // (`revokeAllForDevice`), u esa `clearAutomatically = true`
        // bilan birinchi darajali keshni TOZALAYDI. Hibernate boshqa
        // jadvalga tegadigan so'rovdan oldin bu o'zgarishni yuvishi
        // SHART emas, ya'ni tozalash uni shunchaki yo'q qilardi:
        // token bekor bo'lardi, qurilma esa faol qolardi.
        //
        // Bu taxmin emas — `DeviceModuleTest` ning uchta testi aynan
        // shu tartibda yiqildi.
        UserDevice saved = deviceRepo.saveAndFlush(device);

        // Yozuv nofaol bo'ldi — endi sessiya ham yopilsin.
        refreshTokenService.revokeForDevice(ownerId, device.getDeviceId());
        return saved;
    }

    // ------------------------------------------------------------ ichki

    /**
     * O'rin bormi — FAQAT o'z chelagida.
     *
     * <h2>⚠️ Nima uchun chelak bo'yicha</h2>
     * Ilgari bu metod barcha faol qurilmalarni sanardi va brauzer
     * telefon bilan bitta hisobga tushardi. Natijada kompyuterda
     * saytni ochish odamning telefonini chiqarib yuborardi — u esa
     * hech qanday qurilma qo'shmagandek his qilardi.
     *
     * Buyurtmachi qarori: veb alohida sanaladi.
     *
     * ⚠️ Xabar aniq bo'lishi kerak: odam nima qilishini bilsin.
     * «Xatolik yuz berdi» bu yerda ish bermaydi — u nima uchun kira
     * olmayotganini tushunmaydi va qo'llab-quvvatlashga yozadi.
     */
    private void ensureRoom(UUID userId, String platform) {
        boolean web = isWeb(platform);
        int limit = web ? webLimit() : limit();

        long used = deviceRepo.findAllByUserIdAndActiveTrueOrderByLastActiveAtAsc(userId).stream()
                .filter(d -> isWeb(d.getPlatform()) == web)
                .count();

        if (used >= limit) {
            throw new BusinessException("DEVICE_LIMIT_REACHED",
                    (web ? "Bitta hisobdan " + limit + " tadan ortiq brauzerdan kirish mumkin emas. "
                         : "Bitta hisobdan " + limit + " tadan ortiq qurilmaga kirish mumkin emas. ")
                            + "Eskisini chiqarib, qaytadan urinib ko'ring.",
                    HttpStatus.CONFLICT);
        }
    }

    /** Faollikni yangilaydi — lekin sutkada bir martadan ko'p emas. */
    private UserDevice touch(UserDevice device, String name, String platform) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = device.getLastActiveAt();

        boolean stale = last == null || last.isBefore(now.minus(TOUCH_INTERVAL));
        boolean renamed = applyLabels(device, name, platform);

        if (!stale && !renamed) {
            return device;
        }
        if (stale) {
            device.setLastActiveAt(now);
        }
        return deviceRepo.save(device);
    }

    /**
     * Nom va platformani yangilaydi.
     *
     * Telefon nomi o'zgarishi mumkin («iPhone» → «Ali telefoni») va
     * ro'yxatda eski nom turishi odamni chalkashtirardi: u qaysi
     * qatorni chiqarayotganini bilmasdi.
     *
     * @return biror narsa o'zgardimi
     */
    private boolean applyLabels(UserDevice device, String name, String platform) {
        boolean changed = false;

        String newName = trim(name, MAX_NAME);
        if (newName != null && !newName.equals(device.getDeviceName())) {
            device.setDeviceName(newName);
            changed = true;
        }

        String newPlatform = trim(platform, MAX_PLATFORM);
        if (newPlatform != null && !newPlatform.equals(device.getPlatform())) {
            device.setPlatform(newPlatform);
            changed = true;
        }
        return changed;
    }

    private static String required(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw BusinessException.validation("Qurilma identifikatori yuborilmadi");
        }
        String trimmed = deviceId.trim();
        return trimmed.length() > MAX_DEVICE_ID ? trimmed.substring(0, MAX_DEVICE_ID) : trimmed;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
