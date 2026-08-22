package com.example.backend.Admin.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.RequirePermission;
import com.example.backend.Admin.Dto.AdminUserDto;
import com.example.backend.Admin.Dto.PageResponse;
import com.example.backend.Admin.Dto.StaffCreateRequest;
import com.example.backend.Admin.Dto.StaffPasswordRequest;
import com.example.backend.Admin.Dto.StaffUpdateRequest;
import com.example.backend.Admin.Service.AdminUserMapper;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Cms.Enums.StaffStatus;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.RoleMapper;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.Services.PermissionService.PermissionService;
import com.example.backend.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Xodimlarni boshqarish.
 *
 * <b>Ierarxiya qat'iy:</b> HYPER_ADMIN -> SuperAdmin/Admin/Worker,
 * SUPER_ADMIN -> Admin/Worker, ADMIN -> faqat Worker, WORKER -> hech kim.
 * Tekshiruv {@link PlatformRole#canCreate} da, ya'ni bitta joyda.
 *
 * Privilege escalation'ning ikki yo'li yopilgan:
 * 1) o'zidan yuqori yoki teng rol yaratib bo'lmaydi;
 * 2) o'zida bo'lmagan ruxsatni boshqaga berib bo'lmaydi.
 */
@RestController
@RequestMapping("/api/v1/app/admin/staff")
@RequiredArgsConstructor
public class StaffController {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final AdminUserMapper mapper;
    private final AuditService auditService;
    private final com.example.backend.Cms.Service.StaffService staffService;

    /**
     * Xodimlar ro'yxati.
     *
     * <h2>Filtrlar</h2>
     * Hammasi ixtiyoriy va birga ishlaydi: rol, holat, qidiruv (ism yoki
     * telefon bo'yicha), yaratilgan sana oralig'i.
     *
     * <h2>Nega xotirada filtrlanadi</h2>
     * Rol eski {@code users_roles} jadvalidan, holat esa yangi
     * {@code cms_staff_profile} dan keladi — ular orasida JPA bog'lanishi
     * yo'q (eski sxema muzlatilgan). Xodimlar soni o'nlab, ko'pi bilan
     * yuzlab bo'ladi, shuning uchun bu yerda xotirada filtrlash arzon.
     *
     * ⚠️ Agar xodimlar soni minglarga yetsa — buni SQL'ga ko'chirish kerak.
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserDto>> list(
            @RequestParam(required = false) PlatformRole role,
            @RequestParam(required = false) StaffStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User actor = CurrentUser.get();
        PlatformRole actorRole = permissionService.roleOf(actor);
        if (actorRole == null || !actorRole.isAtLeast(PlatformRole.ADMIN)) {
            throw BusinessException.accessDenied("Xodimlar ro'yxati uchun ruxsat yo'q");
        }

        // KO'RISH va BOSHQARISH ajratilgan.
        //
        // HYPER_ADMIN — platformadagi eng yuqori rol — BARCHA xodim
        // hisoblarini ko'radi, jumladan o'ziga teng rollarni va o'zini.
        // Sabab: ilgari u boshqa HYPER_ADMIN hisobini umuman ko'ra olmasdi.
        // Ya'ni `AutoRun` yaratgan master hisob hamma narsaga qodir bo'lib,
        // hech kimning ro'yxatida chiqmasdi — bu amalda backdoor edi.
        //
        // Quyi rollar uchun eski xatti-harakat saqlanadi: ular faqat
        // o'zidan quyi rollarni ko'radi.
        //
        // ⚠️ Ko'rish huquqi BOSHQARISH huquqini bermaydi. Har bir amalda
        // `canManageUser` alohida tekshiriladi.
        List<User> visible = staffService.visibleStaff(actor);

        // Profillarni BIR so'rovda olamiz - har bir satr uchun alohida
        // so'rov ketmasin (N+1).
        var profiles = staffService.profilesOf(
                visible.stream().map(User::getId).toList());

        // Yaratuvchi ismini ko'rsatish uchun: id ro'yxatda foydasiz.
        var names = new java.util.HashMap<java.util.UUID, String>();
        visible.forEach(u -> names.put(u.getId(), u.getName()));

        String needle = q == null ? null : q.trim().toLowerCase();

        List<AdminUserDto> staff = visible.stream()
                .map(u -> {
                    AdminUserDto dto = mapper.toDto(u);
                    dto.setManageable(!u.getId().equals(actor.getId())
                            && permissionService.canManageUser(actor, u));

                    var profile = profiles.get(u.getId());
                    // Profilsiz eski hisoblar ACTIVE hisoblanadi.
                    dto.setStatus(profile == null ? StaffStatus.ACTIVE : profile.getStatus());
                    if (profile != null) {
                        dto.setCreatedBy(profile.getCreatedBy());
                        dto.setCreatedByName(names.get(profile.getCreatedBy()));
                        dto.setCreatedAt(profile.getCreatedAt());
                        dto.setLastLoginAt(profile.getLastLoginAt());
                        dto.setStatusReason(profile.getStatusReason());
                    }
                    return dto;
                })
                .filter(d -> role == null || d.getRole() == role)
                .filter(d -> status == null || d.getStatus() == status)
                .filter(d -> needle == null || needle.isEmpty()
                        || (d.getName() != null && d.getName().toLowerCase().contains(needle))
                        || (d.getPhone() != null && d.getPhone().toLowerCase().contains(needle))
                        || (d.getEmail() != null && d.getEmail().toLowerCase().contains(needle)))
                .filter(d -> createdFrom == null || d.getCreatedAt() == null
                        || !d.getCreatedAt().toLocalDate().isBefore(createdFrom))
                .filter(d -> createdTo == null || d.getCreatedAt() == null
                        || !d.getCreatedAt().toLocalDate().isAfter(createdTo))
                .sorted(Comparator.comparing((AdminUserDto d) -> d.getRole().getLevel()).reversed())
                .toList();

        // ⚠️ Sahifa XOTIRADA kesiladi.
        //
        // Filtrlash yuqorida xotirada bajariladi (eski sxema muzlatilgan
        // va xodimlar soni o'nlab). Sahifani SQL'da kesish uchun butun
        // filtrni ham SQL'ga ko'chirish kerak bo'lardi — bu esa katta
        // o'zgarish, foydasi esa yo'q: kesiladigan ro'yxat allaqachon
        // xotirada.
        //
        // ⚠️ Xodimlar soni minglarga yetsa — ikkalasini ham SQL'ga
        // ko'chirish kerak.
        return ResponseEntity.ok(PageResponse.ofList(staff, page, Math.min(Math.max(size, 1), 200)));
    }

    @PostMapping
    @Transactional
    // Bazaviy talab: xodim yaratish faqat ADMIN va undan yuqorisiga.
    // Aynan QAYSI rolni yarata olishi tanaga bog'liq, shuning uchun
    // u metod ichida (canCreateRole) tekshiriladi.
    @RequirePermission(role = PlatformRole.ADMIN)
    public ResponseEntity<AdminUserDto> create(@Valid @RequestBody StaffCreateRequest request) {
        User actor = CurrentUser.get();

        if (!permissionService.canCreateRole(actor, request.getRole())) {
            throw BusinessException.accessDenied(
                    "Sizning rolingiz " + request.getRole() + " yarata olmaydi");
        }

        String phone = request.getPhone().replaceAll("\\s", "");
        if (userRepo.findByPhone(phone).isPresent()) {
            throw BusinessException.duplicate("DUPLICATE_PHONE",
                    "Bu telefon raqami allaqachon ro'yxatdan o'tgan");
        }

        UserRoles dbRole = RoleMapper.toUserRole(request.getRole());
        User created = userRepo.save(User.builder()
                .name(request.getName().trim())
                .phone(phone)
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(List.of(ensureRole(dbRole)))
                .build());

        // Xodim profili: holat, kim yaratgani, qachon.
        staffService.register(created, actor);

        // Ruxsatlar faqat WORKER uchun. replacePermissions o'zi tekshiradi:
        // yaratuvchi o'zida bo'lmagan ruxsatni bera olmaydi.
        if (request.getRole() == PlatformRole.WORKER) {
            permissionService.replacePermissions(actor, created.getId(),
                    request.getPermissions() == null ? Set.of() : request.getPermissions());
        }

        auditService.log(actor, AuditAction.STAFF_CREATED, "User", created.getId(),
                null, java.util.Map.of("role", request.getRole(), "phone", phone));

        return ResponseEntity.ok(mapper.toDto(created));
    }

    // ------------------------------------------------------- ruxsatlar

    /**
     * Xodim ruxsatlarini almashtiradi.
     *
     * <h2>Nega bu endpoint kerak edi</h2>
     * {@code PermissionService.replacePermissions} servisda bor edi, lekin
     * uni chaqiradigan endpoint YO'Q edi. Ya'ni WORKER yaratilgandan keyin
     * uning huquqlarini o'zgartirib bo'lmasdi — faqat yangi hisob ochish
     * qolardi.
     *
     * <h2>Huquq oshirishga qarshi</h2>
     * Tekshiruv servisda: aktor <b>o'zida bo'lmagan</b> ruxsatni bera
     * olmaydi. Bundan tashqari faqat o'zidan quyi rolli xodimga tegishi
     * mumkin ({@link PlatformRole#canManage}).
     */
    @PutMapping("/{userId}/permissions")
    @RequirePermission(role = PlatformRole.ADMIN)
    @Transactional
    public ResponseEntity<AdminUserDto> replacePermissions(
            @PathVariable java.util.UUID userId,
            @RequestBody Set<com.example.backend.Enums.Permission> permissions) {

        User actor = CurrentUser.get();
        User target = manageableTarget(actor, userId);

        permissionService.replacePermissions(actor, userId, permissions);

        auditService.log(actor, AuditAction.PERMISSION_CHANGED, "User", userId, null,
                java.util.Map.of("permissions", String.valueOf(permissions)));

        return ResponseEntity.ok(mapper.toDto(target));
    }

    // ------------------------------------------------------------- rol

    /**
     * Xodim rolini o'zgartiradi.
     *
     * Ikki tomonlama tekshiriladi: aktor NIShONNI boshqara olishi kerak
     * (hozirgi roli bo'yicha) VA yangi rolni yarata olishi kerak. Aks holda
     * ADMIN o'z xodimini SUPER_ADMIN qilib, keyin o'sha hisob orqali
     * huquqini oshirib olardi.
     */
    @PutMapping("/{userId}/role")
    @RequirePermission(role = PlatformRole.ADMIN)
    @Transactional
    public ResponseEntity<AdminUserDto> changeRole(@PathVariable java.util.UUID userId,
                                                   @RequestParam PlatformRole role) {
        User actor = CurrentUser.get();
        User target = manageableTarget(actor, userId);
        PlatformRole before = permissionService.roleOf(target);

        // Yangi rol ham aktorning yaratish doirasida bo'lishi SHART.
        if (!permissionService.canCreateRole(actor, role)) {
            throw BusinessException.accessDenied(
                    "Sizning rolingiz " + role + " tayinlay olmaydi");
        }

        // ⚠️ MUTABLE ro'yxat bo'lishi SHART. List.of(...) o'zgarmas va
        // Hibernate mavjud entity'ning to'plamini boshqarishga urinib
        // UnsupportedOperationException tashlaydi.
        target.setRoles(new java.util.ArrayList<>(
                List.of(ensureRole(RoleMapper.toUserRole(role)))));
        userRepo.save(target);

        auditService.log(actor, AuditAction.ROLE_CHANGED, "User", userId,
                java.util.Map.of("role", String.valueOf(before)),
                java.util.Map.of("role", role.name()));

        return ResponseEntity.ok(mapper.toDto(target));
    }

    // ------------------------------------------------------- tahrirlash

    /**
     * Xodim ma'lumotlarini tahrirlaydi.
     *
     * Rol, ruxsat va parol bu yerda O'ZGARMAYDI — ular alohida
     * endpointlarda, chunki har birining o'z xavfsizlik qoidasi bor.
     */
    @PutMapping("/{userId}")
    @RequirePermission(role = PlatformRole.ADMIN)
    @Transactional
    public ResponseEntity<AdminUserDto> update(@PathVariable java.util.UUID userId,
                                               @Valid @RequestBody StaffUpdateRequest request) {
        User actor = CurrentUser.get();
        User target = manageableTarget(actor, userId);

        String phone = request.getPhone().replaceAll("\\s", "");
        userRepo.findByPhone(phone).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw BusinessException.duplicate("DUPLICATE_PHONE",
                        "Bu telefon raqami boshqa hisobga tegishli");
            }
        });

        target.setName(request.getName().trim());
        target.setPhone(phone);
        target.setEmail(request.getEmail());
        target.setAvatarUrl(request.getAvatarUrl());
        userRepo.save(target);

        auditService.log(actor, AuditAction.STAFF_UPDATED, "User", userId, null,
                java.util.Map.of("name", target.getName(), "phone", phone));

        return ResponseEntity.ok(dto(actor, target));
    }

    /**
     * Parolni tiklaydi.
     *
     * ⚠️ Parol javobda QAYTARILMAYDI va auditga YOZILMAYDI — jurnalga
     * faqat «parol tiklandi» faktining o'zi tushadi.
     */
    @PutMapping("/{userId}/password")
    @RequirePermission(role = PlatformRole.ADMIN)
    @Transactional
    public ResponseEntity<Void> resetPassword(@PathVariable java.util.UUID userId,
                                              @Valid @RequestBody StaffPasswordRequest request) {
        User actor = CurrentUser.get();
        User target = manageableTarget(actor, userId);

        target.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepo.save(target);

        auditService.log(actor, AuditAction.STAFF_PASSWORD_RESET, "User", userId);

        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------- holat

    /**
     * Faolsizlantirish — HARD DELETE o'rnida.
     *
     * Hisob qoladi, kirish yopiladi. Sabab: audit jurnalidagi yozuvlar
     * {@code actor_id} saqlaydi va xodim o'chirilsa, o'tmishdagi amallarni
     * kimga bog'lash noma'lum bo'lib qolardi.
     */
    @PostMapping("/{userId}/deactivate")
    @RequirePermission(role = PlatformRole.ADMIN)
    @Transactional
    public ResponseEntity<AdminUserDto> deactivate(@PathVariable java.util.UUID userId,
                                                   @RequestParam(required = false) String reason) {
        User actor = CurrentUser.get();
        User target = manageableTarget(actor, userId);
        staffService.deactivate(actor, target, reason);
        return ResponseEntity.ok(dto(actor, target));
    }

    @PostMapping("/{userId}/activate")
    @RequirePermission(role = PlatformRole.ADMIN)
    @Transactional
    public ResponseEntity<AdminUserDto> activate(@PathVariable java.util.UUID userId) {
        User actor = CurrentUser.get();
        User target = manageableTarget(actor, userId);
        staffService.activate(actor, target);
        return ResponseEntity.ok(dto(actor, target));
    }

    /**
     * Bloklash — VAQTINCHA to'xtatish.
     *
     * Faolsizlantirishdan farqi: bu qaytariladigan chora (masalan tergov
     * davomida). Ikkalasida ham kirish yopiladi va MAVJUD token darhol
     * kuchsizlanadi — tekshiruv har so'rovda bazadan o'qiladi.
     */
    @PostMapping("/{userId}/block")
    @RequirePermission(role = PlatformRole.ADMIN)
    @Transactional
    public ResponseEntity<AdminUserDto> block(@PathVariable java.util.UUID userId,
                                              @RequestParam(required = false) String reason) {
        User actor = CurrentUser.get();
        User target = manageableTarget(actor, userId);
        staffService.block(actor, target, reason);
        return ResponseEntity.ok(dto(actor, target));
    }

    @PostMapping("/{userId}/unblock")
    @RequirePermission(role = PlatformRole.ADMIN)
    @Transactional
    public ResponseEntity<AdminUserDto> unblock(@PathVariable java.util.UUID userId) {
        User actor = CurrentUser.get();
        User target = manageableTarget(actor, userId);
        staffService.unblock(actor, target);
        return ResponseEntity.ok(dto(actor, target));
    }

    // ------------------------------------------------------- ichki qism

    /**
     * Nishon xodimni topadi va aktor uni boshqara olishini tekshiradi.
     *
     * O'zini o'zi boshqarish ham taqiqlanadi: aks holda admin o'z rolini
     * oshirib yoki o'zini bloklab qo'yishi mumkin edi.
     */
    private User manageableTarget(User actor, java.util.UUID userId) {
        if (actor.getId().equals(userId)) {
            throw BusinessException.accessDenied("O'z hisobingizni o'zgartira olmaysiz");
        }
        User target = userRepo.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        if (!permissionService.canManageUser(actor, target)) {
            // "Topilmadi": yuqori rolli hisob borligini ham oshkor qilmaymiz.
            throw BusinessException.notFound("User", userId);
        }
        return target;
    }

    /** Profil maydonlari bilan to'ldirilgan DTO. */
    private AdminUserDto dto(User actor, User target) {
        AdminUserDto dto = mapper.toDto(target);
        dto.setManageable(!target.getId().equals(actor.getId())
                && permissionService.canManageUser(actor, target));
        var profile = staffService.profileOf(target);
        dto.setStatus(profile.getStatus());
        dto.setCreatedBy(profile.getCreatedBy());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setLastLoginAt(profile.getLastLoginAt());
        dto.setStatusReason(profile.getStatusReason());
        return dto;
    }

    /** Role.id auto-generate emas - keyingi bo'sh id beriladi. */
    private Role ensureRole(UserRoles name) {
        Role existing = roleRepo.findByName(name);
        if (existing != null) {
            return existing;
        }
        int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
        return roleRepo.save(new Role(nextId, name));
    }
}
