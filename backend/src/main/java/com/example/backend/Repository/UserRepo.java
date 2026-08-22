package com.example.backend.Repository;

import com.example.backend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

import org.springframework.data.repository.query.Param;

public interface UserRepo extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmail(String email);

    @Query(value = "SELECT u.* FROM users u JOIN users_roles ur ON u.id = ur.user_id JOIN role r ON ur.roles_id = r.id WHERE r.name = 'ROLE_ADMIN'", nativeQuery = true)
    List<User> findAllAdminsByRole();

    @Query(value = "DELETE FROM users_roles WHERE user_id = :id", nativeQuery = true)
    void deleteRole(UUID id);


    @Query(value = "SELECT u.* FROM users u JOIN users_roles ur ON u.id = ur.user_id JOIN role r ON ur.roles_id = r.id WHERE r.name = 'ROLE_DEKAN'", nativeQuery = true)
    List<User> findAllDeanByRoleId();

    /**
     * Ilova foydalanuvchilari — admin paneldagi ro'yxat uchun (ТЗ §35).
     *
     * <h2>Nima uchun so'rov, xotirada filtrlash emas</h2>
     * Ilgari bu ro'yxat {@code findAll()} bilan olinardi va xodimlar
     * Java'da ajratilardi — ya'ni panelni ochish BUTUN foydalanuvchi
     * jadvalini xotiraga tortardi. 100 000 ta foydalanuvchida bu har bir
     * sahifa ochilishida 100 000 satr degani, chegara esa faqat
     * shundan keyin qo'llanardi.
     *
     * <h2>Xodimlar nima uchun chiqarib tashlanadi</h2>
     * Xodimlar alohida ekranda boshqariladi (ТЗ §12). Bu ro'yxat esa
     * ilova foydalanuvchilari uchun — ular aralashsa, admin o'zini
     * bloklab qo'yishi mumkin edi.
     *
     * <h2>ID bo'yicha qidiruv</h2>
     * ТЗ §38 premium sovg'a qilish uchun foydalanuvchini <b>telefon,
     * email yoki ID</b> orqali topishni talab qiladi. ID — UUID, uni
     * {@code like} bilan qidirib bo'lmaydi (turlar mos kelmaydi), shuning
     * uchun alohida parametr: qidiruv matni to'g'ri UUID bo'lsa
     * to'ldiriladi, aks holda {@code null} va shart o'chadi.
     */
    @Query("""
            select u from User u
            where not exists (
                select r from User su join su.roles r
                where su.id = u.id and r.name <> com.example.backend.Enums.UserRoles.ROLE_USER
            )
            and (:q is null
                 or lower(u.phone) like lower(concat('%', :q, '%'))
                 or lower(u.email) like lower(concat('%', :q, '%'))
                 or lower(u.name)  like lower(concat('%', :q, '%'))
                 or (:exactId is not null and u.id = :exactId))
            """)
    Page<User> findAppUsers(@Param("q") String q,
                            @Param("exactId") UUID exactId,
                            Pageable pageable);

    /**
     * Ilova foydalanuvchilari SONI — bildirishnoma auditoriyasi uchun (§33).
     *
     * Xodimlar chiqarib tashlanadi: ular ilova foydalanuvchisi emas va
     * push xabar olmaydi.
     */
    @Query("""
            select count(u) from User u
            where not exists (
                select r from User su join su.roles r
                where su.id = u.id and r.name <> com.example.backend.Enums.UserRoles.ROLE_USER
            )
            """)
    long countAppUsers();

    /**
     * Faol premiumga ega ilova foydalanuvchilari soni.
     *
     * Premium muddati {@code UserAccount} da saqlanadi. Hisobi yo'q
     * foydalanuvchi premiumsiz hisoblanadi.
     */
    @Query("""
            select count(u) from User u
            where not exists (
                select r from User su join su.roles r
                where su.id = u.id and r.name <> com.example.backend.Enums.UserRoles.ROLE_USER
            )
            and exists (
                select a from UserAccount a
                where a.user.id = u.id and a.premiumUntil > :moment
            )
            """)
    long countPremiumAppUsers(@Param("moment") java.time.LocalDateTime moment);

    /**
     * Berilgan sanadan keyin ro'yxatdan o'tgan ilova foydalanuvchilari (§45).
     *
     * ⚠️ V17 dan oldin ro'yxatdan o'tganlarda {@code created_at} bo'sh —
     * ular bu sanoqqa kirmaydi. Bu TO'G'RI: ular qachon qo'shilganini
     * bilmaymiz va taxmin qilmaymiz.
     */
    @Query("""
            select count(u) from User u
            where u.createdAt >= :since
            and not exists (
                select r from User su join su.roles r
                where su.id = u.id and r.name <> com.example.backend.Enums.UserRoles.ROLE_USER
            )
            """)
    long countAppUsersCreatedAfter(@Param("since") java.time.LocalDateTime since);

    /** Xodimlar soni — ilova foydalanuvchilaridan tashqari (§45). */
    @Query("""
            select count(u) from User u
            where exists (
                select r from User su join su.roles r
                where su.id = u.id and r.name <> com.example.backend.Enums.UserRoles.ROLE_USER
            )
            """)
    long countStaff();

    /**
     * Foydalanuvchi o'sishi — kunlik (ТЗ §48 grafigi).
     *
     * ⚠️ V17 dan oldin ro'yxatdan o'tganlar bu grafikda YO'Q: ularning
     * {@code created_at} maydoni bo'sh. Bu to'g'ri — sana bilinmaydi va
     * taxmin qilinmaydi. Grafik «shu sanadan boshlab» degan ma'noni
     * bildiradi, «hamma vaqt» degani emas.
     */
    @Query("""
            select cast(u.createdAt as date) as day, count(u) as value
            from User u
            where u.createdAt >= :from
            and not exists (
                select r from User su join su.roles r
                where su.id = u.id and r.name <> com.example.backend.Enums.UserRoles.ROLE_USER
            )
            group by cast(u.createdAt as date)
            order by cast(u.createdAt as date)
            """)
    List<DayCount> userGrowth(@Param("from") java.time.LocalDateTime from);

    /** Oxirgi ro'yxatdan o'tganlar (ТЗ §48 jadvali). */
    @Query("""
            select u from User u
            where not exists (
                select r from User su join su.roles r
                where su.id = u.id and r.name <> com.example.backend.Enums.UserRoles.ROLE_USER
            )
            order by u.createdAt desc nulls last, u.id desc
            """)
    List<User> findLatestAppUsers(org.springframework.data.domain.Pageable pageable);

    /** Kunlik sanoq — grafik uchun proyeksiya. */
    interface DayCount {
        java.time.LocalDate getDay();
        Long getValue();
    }
}
