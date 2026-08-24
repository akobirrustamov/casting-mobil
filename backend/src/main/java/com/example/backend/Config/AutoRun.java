package com.example.backend.Config;

import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.Bootstrap.BootstrapPasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AutoRun implements CommandLineRunner {
    private final RoleRepo roleRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    // --- Master hisoblar. Hech qanday UI/endpoint'da ko'rinmaydi. ---
    //
    // ⚠️ PAROLNING STANDART QIYMATI ATAYLAB YO'Q.
    //
    // Ilgari bu yerda `:00000000` turardi va natijada har bir o'rnatishda
    // `gipersuperadmin / 00000000` hisobi paydo bo'lardi — platformadagi ENG
    // YUQORI rol, paroli esa manba kodda. Uni bilgan har kim HYPER_ADMIN
    // sifatida kirib, istalgan hisob yarata olardi.
    //
    // Endi parol faqat environment orqali beriladi:
    //   APP_SUPERADMIN_PHONE / APP_SUPERADMIN_PASSWORD
    //   APP_GIPERSUPERADMIN_PHONE / APP_GIPERSUPERADMIN_PASSWORD
    //
    // Berilmasa yoki zaif bo'lsa — hisob YARATILMAYDI va ogohlantirish yoziladi.
    @Value("${app.superadmin.phone:superadmin}")
    private String superAdminPhone;
    @Value("${app.superadmin.password:}")
    private String superAdminPassword;

    @Value("${app.gipersuperadmin.phone:gipersuperadmin}")
    private String giperSuperAdminPhone;
    @Value("${app.gipersuperadmin.password:}")
    private String giperSuperAdminPassword;

    /**
     * Lokal ishlab chiqishda qulaylik uchun eski xatti-harakat.
     *
     * Faqat `dev` profilida `true`. Prod'da yoqilsa — bu ataylab qilingan
     * qaror bo'ladi va konfiguratsiyada ko'rinib turadi.
     */
    @Value("${app.bootstrap.allow-weak-password:false}")
    private boolean allowWeakPassword;

    /** Eski casting admin hisoblari uchun parol. Standart qiymati yo'q. */
    @Value("${app.legacy-admin.password:}")
    private String legacyAdminPassword;

    // ------------------------------------------- panel hisoblari
    //
    // UZCASTING admin paneli uchun ADMIN va WORKER hisoblari.
    //
    // ⚠️ Parol berilmasa hisob YARATILMAYDI. Bu ataylab: bo'sh yoki
    // standart parolli admin hisobi ochiq eshikdan yomonroq, chunki
    // uni hech kim ko'rmaydi.
    //
    // Environment ustun turadi:
    //   APP_ADMIN_PHONE / APP_ADMIN_PASSWORD
    //   APP_WORKER_PHONE / APP_WORKER_PASSWORD

    @Value("${app.admin.phone:admin}")
    private String adminPhone;
    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.worker.phone:worker}")
    private String workerPhone;
    @Value("${app.worker.password:}")
    private String workerPassword;

    @Override
    public void run(String... args) throws Exception {
        // 1) Rollarni ta'minlaymiz (bo'sh DB'da ham, mavjud DB'da ham idempotent)
        if (roleRepo.findAll().isEmpty()) {
            saveRoles();
        }
        ensureRole(UserRoles.ROLE_SUPERADMIN);
        ensureRole(UserRoles.ROLE_GIPERSUPERADMIN);
        // UZCASTING admin paneli uchun. Worker hisoblarini Admin/SuperAdmin yaratadi,
        // shuning uchun bu yerda faqat rolning o'zi ta'minlanadi.
        ensureRole(UserRoles.ROLE_WORKER);

        // 2) Eski casting admin hisoblari
        //
        // ⚠️ O'zgaruvchi nomi `legacyPhone` — `adminPhone` EMAS. U
        // maydon nomi bilan bir xil bo'lsa uni SOYA qilardi va
        // pastdagi `ensurePanelUser` sozlamadagi telefonni emas,
        // `admin1234` ni olardi. Natijada panel ADMIN hisobi jimgina
        // yaratilmasdi: hisob allaqachon mavjud deb hisoblanardi va
        // logga ham hech narsa yozilmasdi.
        String legacyPhone = "admin1234";
        saveUser(legacyPhone, userRepo.findByPhone(legacyPhone));

        // 3) Master hisoblar - hech qayerda ro'yxatga chiqmaydi
        ensureHiddenUser(superAdminPhone, superAdminPassword, UserRoles.ROLE_SUPERADMIN);
        ensureHiddenUser(giperSuperAdminPhone, giperSuperAdminPassword, UserRoles.ROLE_GIPERSUPERADMIN);

        // 4) Panel hisoblari — ADMIN va WORKER
        //
        // ⚠️ Bular master hisoblardan FARQLI: ular panelning xodimlar
        // ro'yxatida KO'RINADI va ular ustida odatdagi amallar
        // bajariladi (rol o'zgartirish, bloklash, parol tiklash).
        ensurePanelUser(adminPhone, adminPassword, UserRoles.ROLE_ADMIN);
        ensurePanelUser(workerPhone, workerPassword, UserRoles.ROLE_WORKER);
    }

    /**
     * Panel hisobi — xodimlar ro'yxatida ko'rinadigan ADMIN yoki WORKER.
     *
     * <h2>Master hisobdan farqi</h2>
     * {@code ensureHiddenUser} yaratgan hisoblar ro'yxatga chiqmaydi va
     * ular tizim egasining zaxira kaliti. Bular esa oddiy xodim:
     * panelda ko'rinadi, tahrirlanadi, bloklanadi.
     *
     * ⚠️ WORKER hech qanday RUXSATSIZ yaratiladi. Bu to'g'ri: ТЗ §12
     * bo'yicha ruxsatni Admin yoki SuperAdmin beradi va bu amal
     * auditga tushadi. Bu yerda avtomatik ruxsat berish o'sha izni
     * hech kim bermagan holga keltirardi.
     */
    private void ensurePanelUser(String phone, String rawPassword, UserRoles role) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        if (userRepo.findByPhone(phone).isPresent()) {
            return;
        }
        if (!passwordAccepted(rawPassword, phone)) {
            return;
        }

        userRepo.save(User.builder()
                .phone(phone)
                .password(passwordEncoder.encode(rawPassword))
                .name(defaultNameFor(role))
                .roles(List.of(roleRepo.findByName(role)))
                .build());

        log.info("Panel hisobi yaratildi: {} ({})", phone, role);
    }

    /** Xodimlar ro'yxatida bo'sh ustun turmasligi uchun. */
    private String defaultNameFor(UserRoles role) {
        return role == UserRoles.ROLE_ADMIN ? "Administrator" : "Xodim";
    }


    /**
     * Eski casting admin hisoblari.
     *
     * ⚠️ Ilgari bular ham `00000000` paroli bilan yaratilardi. Endi parol
     * `app.legacy-admin.password` orqali beriladi; berilmasa yaratilmaydi.
     *
     * MAVJUD o'rnatishlarga ta'sir qilmaydi: hisob allaqachon bazada bo'lsa,
     * bu metod umuman ishlamaydi.
     */
    private void saveUser(String legacyPhone, Optional<User> userByPhone) {
        if (userByPhone.isPresent()) {
            return;
        }
        if (!passwordAccepted(legacyAdminPassword, legacyPhone)) {
            return;
        }
        for (String phone : List.of(legacyPhone, legacyPhone + "5")) {
            userRepo.save(User.builder()
                    .phone(phone)
                    .password(passwordEncoder.encode(legacyAdminPassword))
                    .roles(List.of(roleRepo.findByName(UserRoles.ROLE_ADMIN)))
                    .build());
        }
        log.info("Eski admin hisoblari yaratildi: {}, {}5", legacyPhone, legacyPhone);
    }

    /**
     * Parol qabul qilinadimi. Parolning O'ZI hech qachon logga yozilmaydi.
     */
    private boolean passwordAccepted(String password, String phone) {
        if (BootstrapPasswordPolicy.isAcceptable(password)) {
            return true;
        }
        if (allowWeakPassword && password != null && !password.isBlank()) {
            log.warn("'{}' hisobi ZAIF parol bilan yaratilmoqda "
                    + "(app.bootstrap.allow-weak-password=true). "
                    + "Bu faqat lokal ishlab chiqish uchun.", phone);
            return true;
        }
        log.warn("'{}' hisobi YARATILMADI: {}. "
                        + "Yaratish uchun parolni environment orqali bering.",
                phone, BootstrapPasswordPolicy.rejectionReason(password));
        return false;
    }

    private void saveRoles() {
        roleRepo.saveAll(List.of(
                new Role(1, UserRoles.ROLE_ADMIN),
                new Role(2, UserRoles.ROLE_STUDENT),
                new Role(3, UserRoles.ROLE_USER),
                new Role(4, UserRoles.ROLE_TEACHER)
        ));
    }

    /** Rol mavjud bo'lmasa yaratadi. Role id auto-generate emas, shuning uchun keyingi bo'sh id beriladi. */
    private Role ensureRole(UserRoles name) {
        Role existing = roleRepo.findByName(name);
        if (existing != null) {
            return existing;
        }
        int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
        return roleRepo.save(new Role(nextId, name));
    }

    /**
     * Yashirin foydalanuvchi. Faqat o'z roli beriladi (ROLE_ADMIN emas),
     * shuning uchun admin ro'yxatlari yoki UI'da umuman ko'rinmaydi. Idempotent.
     */
    private void ensureHiddenUser(String phone, String rawPassword, UserRoles role) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        if (userRepo.findByPhone(phone).isPresent()) {
            return;
        }
        if (!passwordAccepted(rawPassword, phone)) {
            return;
        }
        User user = User.builder()
                .phone(phone)
                .password(passwordEncoder.encode(rawPassword))
                .roles(List.of(ensureRole(role)))
                .build();
        userRepo.save(user);
    }
}
