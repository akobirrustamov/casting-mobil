package com.example.backend.Cms.Service;

import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Security.LoginAttemptService;
import com.example.backend.Services.AuthService.RefreshTokenService;
import com.example.backend.Sms.OtpService;
import com.example.backend.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mobil ilovada telefon + PAROL bilan ro'yxatdan o'tish va kirish.
 *
 * <h2>Nega bu OTP oqimining yonida turadi</h2>
 * {@code /otp/verify} bitta so'rovda ham ro'yxatdan o'tkazadi, ham
 * kirgizadi — parol umuman yo'q. Buyurtmachi esa ikkita alohida bo'lim
 * so'radi: «ro'yxatdan o'tish» (raqam → SMS kod → parol va uning
 * takrori) va «kirish» (raqam → parol). Ya'ni SMS endi FAQAT raqamni
 * tasdiqlaydi, kundalik kirish esa parol bilan bo'ladi.
 *
 * OTP endpointlari o'z joyida qoladi: ulardan boshqa oqimlar
 * foydalanishi mumkin va ularni sindirish uchun sabab yo'q.
 *
 * <h2>Uch qadam, uchta so'rov</h2>
 * <ol>
 *   <li>{@link #startRegistration} — raqam bo'shligini tekshiradi va SMS yuboradi;</li>
 *   <li>{@link #confirmRegistration} — kodni tekshiradi va raqamni «tasdiqlangan» deb belgilaydi;</li>
 *   <li>{@link #completeRegistration} — parolni saqlaydi va tokenlarni beradi.</li>
 * </ol>
 *
 * Qadamlar ajratilgan, chunki parol o'ylab topish vaqt oladi: hammasini
 * bitta so'rovga yig'ish kodning 3 daqiqalik muddatiga tiqilib qolardi.
 */
@Service
@RequiredArgsConstructor
public class AppAccountService {

    /**
     * Eng qisqa parol.
     *
     * ⚠️ Ko'proq talab (katta harf, raqam, belgi) ATAYLAB qo'yilmagan:
     * bu ommaviy ilova va «parolni unutdim» hali ishlamaydi — murakkab
     * qoida odamni parolni yozib qo'yishga yoki hisobini yo'qotishga
     * olib kelardi. Uzunlik esa eng foydali yagona chegara.
     */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * ⚠️ BCrypt 72 BAYTDAN keyingisini o'qimaydi — undan uzun parol
     * jimgina qirqilardi. Shuning uchun uzunini qabul qilmaymiz.
     */
    private static final int MAX_PASSWORD_BYTES = 72;

    /**
     * Ism uzunligi chegaralari.
     *
     * Pastki chegara bitta harfli «ism» dan himoya qiladi, yuqorigisi —
     * profil va izohlar ostida sig'maydigan matndan. Ism formatiga
     * boshqa talab yo'q: dunyoda ism qanday yozilishi haqidagi har
     * qanday qoida kimningdir haqiqiy ismini rad etadi.
     */
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 60;

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttempts;

    /**
     * 1-qadam: raqamga SMS kod.
     *
     * @return kod necha soniya amal qilishi
     * @throws BusinessException raqam allaqachon parolli hisobga tegishli bo'lsa
     */
    public int startRegistration(String rawPhone) {
        String phone = OtpService.normalize(rawPhone);

        userRepo.findByPhone(phone).ifPresent(user -> {
            if (!canSetPassword(user)) {
                throw alreadyRegistered();
            }
        });

        // ⚠️ Band raqamga SMS YUBORILMAYDI: tekshiruv oldinda turibdi.
        // Aks holda «raqam band» xabari pul turadigan SMS bilan birga
        // kelardi va uni takror-takror chaqirish mumkin bo'lardi.
        return otpService.send(phone);
    }

    /**
     * 2-qadam: kod to'g'riligini tekshiradi.
     *
     * @return «tasdiqlangan» belgisi necha soniya amal qilishi
     */
    public int confirmRegistration(String rawPhone, String code) {
        String phone = OtpService.normalize(rawPhone);
        otpService.verify(phone, code);
        return otpService.markVerified(phone);
    }

    /**
     * 3-qadam: ism va parol saqlanadi, odam darhol kirgiziladi.
     *
     * Hisob AYNAN shu yerda yaratiladi — kod tasdiqlangani bilan emas.
     * Parolsiz yaratilgan hisob keyin kirib bo'lmaydigan bo'lib
     * qolardi.
     */
    @Transactional
    public Map<String, Object> completeRegistration(String rawPhone,
                                                    String rawName,
                                                    String password,
                                                    String passwordConfirm,
                                                    HttpServletRequest request) {
        String phone = OtpService.normalize(rawPhone);
        String name = validateName(rawName);
        validatePassword(password, passwordConfirm);

        // ⚠️ Belgi shu yerda «yeyiladi»: bitta tasdiqlash — bitta parol.
        otpService.consumeVerified(phone);

        User user = userRepo.findByPhone(phone)
                .map(existing -> {
                    // 1-qadamdan beri vaqt o'tdi — raqam shu orada band
                    // bo'lgan bo'lishi mumkin, tekshiruv qaytariladi.
                    if (!canSetPassword(existing)) {
                        throw alreadyRegistered();
                    }
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .phone(phone)
                        .roles(appUserRole())
                        .build());

        // ⚠️ Ism mavjud hisobda ham YANGILANADI: odam uni hozirgina
        // o'zi kiritdi va bu eng so'nggi haqiqat. Google'dan olingan
        // eski ism uning o'z yozuvidan ustun turmaydi.
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordSet(true);

        return session(userRepo.save(user), request);
    }

    /**
     * Kirish: telefon + parol.
     *
     * <h2>Xato xabarlari nega ajratilgan</h2>
     * Admin panelda «foydalanuvchi yo'q» va «parol xato» ATAYLAB bir xil
     * javob beradi — u yerda kimning hisobi borligini aniqlab olish
     * xavfi bor. Bu yerda esa ro'yxatdan o'tish oqimining o'zi «bu
     * raqam band» deyishga majbur (buyurtmachi talabi), ya'ni
     * yashiradigan narsa qolmaydi. Shu sababli odamga nima qilish
     * kerakligi aniq aytiladi.
     */
    @Transactional
    public Map<String, Object> login(String rawPhone, String password, HttpServletRequest request) {
        String phone = OtpService.normalize(rawPhone);

        long locked = loginAttempts.lockedMinutesLeft(phone);
        if (locked > 0) {
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Ko'p marta xato urinildi. " + locked + " daqiqadan keyin qayta urinib ko'ring.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        Optional<User> found = userRepo.findByPhone(phone);
        if (found.isEmpty()) {
            throw new BusinessException("PHONE_NOT_REGISTERED",
                    "Bu raqam ro'yxatdan o'tmagan", HttpStatus.UNAUTHORIZED);
        }

        User user = found.get();
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            loginAttempts.recordFailure(phone);

            // Google yoki SMS orqali yaratilgan hisobda parol o'rniga
            // tasodifiy hash turadi — u HECH QACHON mos kelmaydi.
            // «Parol noto'g'ri» deyish odamni cheksiz urinishga majbur
            // qilardi.
            if (!user.isPasswordSet()) {
                throw new BusinessException("PASSWORD_NOT_SET",
                        "Bu raqamda parol o'rnatilmagan, ro'yxatdan o'tish bo'limida parol qo'ying",
                        HttpStatus.UNAUTHORIZED);
            }

            throw new BusinessException("INVALID_CREDENTIALS",
                    "Telefon yoki parol noto'g'ri", HttpStatus.UNAUTHORIZED);
        }

        loginAttempts.recordSuccess(phone);

        // V30 dan oldingi hisob: paroli haqiqiy, lekin bayrog'i yo'q
        // edi. Muvaffaqiyatli kirish — buning eng ishonchli dalili.
        // Bayroq to'g'rilanmasa, begona odam SMS orqali uning parolini
        // almashtira olardi.
        if (!user.isPasswordSet()) {
            user.setPasswordSet(true);
            user = userRepo.save(user);
        }

        return session(user, request);
    }

    private BusinessException alreadyRegistered() {
        return BusinessException.duplicate("PHONE_ALREADY_REGISTERED",
                "Bu raqam allaqachon ro'yxatdan o'tgan, kirish bo'limidan foydalaning");
    }

    /**
     * Mavjud hisobga SMS orqali parol qo'yish MUMKINmi.
     *
     * <h2>⚠️ Nega rol ham tekshiriladi</h2>
     * V30 dan oldingi hamma satrda {@code password_set = false} — ya'ni
     * xodimning ham. Faqat bayroqqa qarab tursak, kimdir xodimning
     * raqamiga SMS oldirib, uning parolini ALMASHTIRIB qo'yardi.
     * Xodim hisobi bu oqimga umuman kirmaydi: uning paroli panel
     * orqali beriladi.
     */
    private boolean canSetPassword(User user) {
        if (user.isPasswordSet()) {
            return false;
        }
        List<Role> roles = user.getRoles();
        return roles == null || roles.stream()
                .allMatch(role -> role.getName() == UserRoles.ROLE_USER);
    }

    /** @return tozalangan ism — ortiqcha bo'shliqlarsiz */
    private String validateName(String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");

        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("NAME_INVALID",
                    "Ismni to'liq kiriting (" + MIN_NAME_LENGTH + "-" + MAX_NAME_LENGTH
                            + " ta belgi)",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return name;
    }

    private void validatePassword(String password, String passwordConfirm) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("PASSWORD_TOO_SHORT",
                    "Parol kamida " + MIN_PASSWORD_LENGTH + " ta belgidan iborat bo'lsin",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessException("PASSWORD_TOO_LONG",
                    "Parol juda uzun", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (passwordConfirm != null && !password.equals(passwordConfirm)) {
            throw new BusinessException("PASSWORD_MISMATCH",
                    "Parollar mos kelmadi", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private List<Role> appUserRole() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        return role == null ? List.of() : List.of(role);
    }

    /**
     * Javob shakli {@code /otp/verify}, {@code /auth/google} va
     * {@code /app/auth/refresh} bilan BIR XIL — klient uchun bu bitta
     * «sessiya» tushunchasi.
     */
    private Map<String, Object> session(User user, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", jwtService.generateJwtToken(user));
        response.put("refresh_token", refreshTokenService.issue(user, request));
        response.put("roles", user.getRoles());
        response.put("user", Map.of(
                "id", user.getId().toString(),
                "name", user.getName() == null ? "" : user.getName(),
                "phone", user.getPhone() == null ? "" : user.getPhone()));
        return response;
    }
}
