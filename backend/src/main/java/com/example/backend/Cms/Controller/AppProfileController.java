package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * «Men kimman» — mobil ilova uchun profil.
 *
 * <h2>⚠️ Nima uchun bu endpoint kerak bo'ldi</h2>
 * Ilovada bunday so'rov YO'Q edi. Profil kirish javobidan olinib,
 * telefonda saqlanardi va boshqa hech qachon yangilanmasdi
 * ({@code mobile/src/features/auth/store.ts} da bu izohda ochiq
 * yozilgan edi).
 *
 * Natijada:
 *
 * <ul>
 *   <li>ism yoki avatar panelda o'zgartirilsa — ilovada eskisi
 *       qolaverardi;</li>
 *   <li>Premium berilsa yoki tugasa — ilova bilmasdi;</li>
 *   <li>hisob bloklansa — ilova buni faqat birinchi rad javobidan
 *       tushunardi.</li>
 * </ul>
 *
 * <h2>⚠️ Refresh token oqimi buni JIDDIYLASHTIRDI</h2>
 * Ilgari sessiya 15 daqiqada tugab, odam qaytadan kirardi — ya'ni
 * profil kuniga bir necha marta o'z-o'zidan yangilanardi. Endi
 * sessiya kunlab yashaydi va eskirgan profil ham shuncha turadi.
 *
 * Ya'ni tuzatilgan nosozlik ikkinchisini ko'rinadigan qildi.
 *
 * <h2>Nima QAYTARILMAYDI</h2>
 * Balans bu yerda yo'q — u {@code /api/v1/app/donations/balance} da
 * va ilova uni allaqachon so'raydi. Ikkinchi joyda takrorlash ikki
 * manba yasardi va ular bir kuni ajralib ketardi.
 */
@RestController
@RequestMapping("/api/v1/app/me")
@RequiredArgsConstructor
public class AppProfileController {

    private final UserAccountRepo accountRepo;

    /**
     * ⚠️ Premium holati SHU YERDAN so'raladi.
     *
     * «Obuna faolmi» degan qaror bitta joyda turishi shart (ТЗ §37).
     * Kontroller uni o'zi hisoblasa qoidaning ikkinchi nusxasi paydo
     * bo'lardi — {@code PremiumRightsTest} aynan shuni qo'riqlaydi va
     * bu kod yozilganda uni darhol ushladi.
     */
    private final AccessService accessService;

    /**
     * Joriy foydalanuvchi.
     *
     * ⚠️ Token talab qilinadi. Anonim uchun «profil» tushunchasi
     * yo'q — bu 401, bo'sh javob emas: klient farqni bilishi kerak.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<MeResponse> me() {
        User user = CurrentUser.get();
        UserAccount account = accountRepo.findByUserId(user.getId()).orElse(null);

        return ResponseEntity.ok(MeResponse.builder()
                .id(user.getId().toString())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleNames(user))
                // ⚠️ Hisob yozuvi bo'lmasligi mumkin: eski
                // foydalanuvchilar `cms_user_account` paydo bo'lishidan
                // oldin yaratilgan. Bu xato emas — sukut qiymatlar
                // beriladi.
                .language(account == null ? Locale.UZ : account.getLanguage())
                .status(account == null ? UserStatus.ACTIVE : account.getStatus())
                .blockedReason(account == null ? null : account.getBlockedReason())
                .premium(premiumOf(user))
                .build());
    }

    /**
     * ⚠️ Shakl kirish javobidagi bilan AYNAN bir xil: {@code [{name}]}.
     *
     * {@code AuthServiceImpl} rollarni shunday qaytaradi va mobil
     * ilovada ularni o'qiydigan {@code toRole()} allaqachon yozilgan.
     * Bu yerda oddiy satrlar ro'yxati berilsa, o'sha funksiya ikkinchi
     * shaklni ham tushunishi kerak bo'lardi — bitta maydon uchun ikki
     * xil ishlov.
     */
    private List<RoleDto> roleNames(User user) {
        List<Role> roles = user.getRoles();
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(Role::getName)
                .filter(java.util.Objects::nonNull)
                .map(name -> RoleDto.builder().name(name.name()).build())
                .toList();
    }

    private PremiumDto premiumOf(User user) {
        AccessService.PremiumStatus status = accessService.premiumStatus(user);
        return PremiumDto.builder()
                .active(status.active())
                .until(status.until())
                .build();
    }

    // ------------------------------------------------------------------- DTO

    @Data
    @Builder
    public static class MeResponse {
        private String id;
        private String name;
        private String phone;
        private String email;
        private String avatarUrl;

        /** {@code [{"name":"ROLE_USER"}]} — kirish javobidagi shakl. */
        private List<RoleDto> roles;

        /** Foydalanuvchi tanlagan til — push xabar shu tilda yuboriladi. */
        private Locale language;

        /** {@code ACTIVE} yoki {@code BLOCKED}. */
        private UserStatus status;

        /** Faqat {@code BLOCKED} da to'ldiriladi. */
        private String blockedReason;

        private PremiumDto premium;
    }

    @Data
    @Builder
    public static class RoleDto {
        private String name;
    }

    @Data
    @Builder
    public static class PremiumDto {
        private boolean active;

        /**
         * Qachongacha. {@code null} — obuna umuman bo'lmagan.
         *
         * ⚠️ Muddati o'tgan obunada sana SAQLANADI va
         * {@code active=false} bo'ladi: ilova «obunangiz tugadi»
         * deb aniq ayta oladi, «obuna yo'q» emas.
         */
        private LocalDateTime until;
    }
}
