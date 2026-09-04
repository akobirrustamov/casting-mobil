package com.example.backend.Cms.Service;

import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Services.AuthService.RefreshTokenService;
import com.example.backend.Sms.OtpService;
import com.example.backend.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mobil ilovaga kirish: telefon raqam va SMS-kod. Boshqa yo'l yo'q.
 *
 * <h2>Bitta oqim, ikkiga bo'linmaydi</h2>
 * Odam raqamini kiritadi, kodni kiritadi — va tugadi. «Kirish» va
 * «ro'yxatdan o'tish» degan ikki bo'lim endi YO'Q: raqam kiritilayotgan
 * paytda odam qaysi bo'limga tegishli ekani hech kimga kerak emas, va
 * uni shu tanlov oldida ushlab turish ma'nosiz edi. Farq faqat OXIRIDA
 * chiqadi: hisobi bori darhol kiradi, yangisidan bitta narsa — ismi
 * so'raladi.
 *
 * <h2>Uch qadam, uchta so'rov</h2>
 * <ol>
 *   <li>{@link #startLogin} — raqamga SMS kod (hisob bor-yo'qligidan
 *       qat'i nazar);</li>
 *   <li>{@link #verifyLogin} — kod to'g'ri bo'lsa: hisob bor va ismli
 *       bo'lsa sessiya, aks holda «ism kerak»;</li>
 *   <li>{@link #completeLogin} — ism saqlanadi, hisob yaratiladi va
 *       sessiya beriladi.</li>
 * </ol>
 *
 * <h2>⚠️ Nega hisob 2-qadamda YARATILMAYDI</h2>
 * Yaratilsa, ismini kiritmay chiqib ketgan odam bazada ismsiz satr
 * bo'lib qolardi — profilda va izohlar ostida bo'shliq. Hisob ism bilan
 * BIRGA tug'iladi. Kod tasdiqlangani esa {@link OtpService} da 15
 * daqiqaga belgilanadi, ya'ni 3-qadamga borishga vaqt yetadi.
 *
 * <h2>⚠️ Parol butunlay olib tashlandi</h2>
 * Ilgari bu yerda parolli ro'yxatdan o'tish ({@code register/start} →
 * {@code confirm} → {@code complete}) va parolli kirish
 * ({@code /app/auth/login}) turardi. Buyurtmachi (04.09.2026) ularni
 * bekor qildi: raqam baribir SMS bilan tasdiqlanardi, ya'ni parol
 * ikkinchi qulf emas, shunchaki unutiladigan ikkinchi qadam edi — va
 * «parolni unutdim» hech qachon yozilmagan.
 *
 * Bazadagi {@code password} va {@code password_set} ustunlari joyida
 * qoldi: admin panel ular bilan ishlaydi, mobil oqim esa ularga
 * umuman tegmaydi.
 */
@Service
@RequiredArgsConstructor
public class AppAccountService {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * 1-qadam: raqamga SMS kod.
     *
     * <h2>⚠️ Bu yerda «raqam band» degan xato YO'Q</h2>
     * Ilgari ro'yxatdan o'tish oqimi band raqamni {@code 409
     * PHONE_ALREADY_REGISTERED} bilan qaytarardi. Endi band raqam — bu
     * shunchaki kirayotgan odam, va unga ham ayni shu kod boradi.
     *
     * @return kod necha soniya amal qilishi
     */
    public int startLogin(String rawPhone) {
        return otpService.send(OtpService.normalize(rawPhone));
    }

    /**
     * 2-qadam: kodni tekshiradi.
     *
     * <p>Ismli hisobi bor odam SHU YERDA kiradi — javobda tayyor
     * sessiya bo'ladi. Hisobi yo'q (yoki ismi yo'q) odam uchun javob
     * {@code name_required=true} bo'ladi va sessiya BERILMAYDI: aks
     * holda token ismsiz hisobga tegishli bo'lib qolardi.
     *
     * <p>⚠️ Ismi bo'sh MAVJUD hisob ham shu yo'ldan o'tadi. Bunday
     * satrlar bazada bor: eski {@code /otp/verify} va Google orqali
     * kirish ismsiz hisob yaratardi. Ular endi ismini bir marta
     * kiritadi va tuzalib ketadi.
     */
    @Transactional
    public Map<String, Object> verifyLogin(String rawPhone, String code, HttpServletRequest request) {
        String phone = OtpService.normalize(rawPhone);
        otpService.verify(phone, code);

        Optional<User> found = userRepo.findByPhone(phone);
        if (found.isPresent() && hasName(found.get())) {
            Map<String, Object> response = session(found.get(), request);
            response.put("name_required", false);
            return response;
        }

        // Kod «yeyildi», lekin raqam 15 daqiqaga tasdiqlangan deb
        // belgilanadi — 3-qadam ikkinchi SMS so'ramaydi.
        int expiresIn = otpService.markVerified(phone);
        return Map.of("name_required", true, "expiresInSeconds", expiresIn);
    }

    /**
     * 3-qadam: ism. Hisob AYNAN shu yerda yaratiladi va sessiya beriladi.
     *
     * ⚠️ Raqam tasdiqlangan bo'lishi majburiy: {@code consumeVerified}
     * belgini tekshiradi va darhol «yeydi». Ya'ni bitta SMS — bitta
     * hisob: bir marta olingan kod bilan ikkinchi so'rov yuborib
     * bo'lmaydi.
     */
    @Transactional
    public Map<String, Object> completeLogin(String rawPhone, String rawName, HttpServletRequest request) {
        String phone = OtpService.normalize(rawPhone);
        // ⚠️ Qoida {@link PersonName} da — profilni tahrirlash ham
        // aynan shu tekshiruvdan o'tadi. Ikki nusxa bir kun ajralib
        // ketardi va odam profilda qo'ygan ismi bilan kira olmasdi.
        String name = PersonName.validate(rawName);

        otpService.consumeVerified(phone);

        User user = userRepo.findByPhone(phone)
                .orElseGet(() -> User.builder()
                        .phone(phone)
                        .roles(appUserRole())
                        .build());

        // ⚠️ Ism mavjud hisobda ham YANGILANADI: odam uni hozirgina
        // o'zi kiritdi va bu eng so'nggi haqiqat. Google'dan olingan
        // eski ism uning o'z yozuvidan ustun turmaydi.
        user.setName(name);

        Map<String, Object> response = session(userRepo.save(user), request);
        response.put("name_required", false);
        return response;
    }

    /** Bo'sh satr ham «ismi yo'q» degani — u profilda bo'shliq bo'lib ko'rinadi. */
    private boolean hasName(User user) {
        return user.getName() != null && !user.getName().isBlank();
    }

    private List<Role> appUserRole() {
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        return role == null ? List.of() : List.of(role);
    }

    /**
     * Javob shakli {@code /auth/google} va {@code /app/auth/refresh}
     * bilan BIR XIL — klient uchun bu bitta «sessiya» tushunchasi.
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
