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
                 or lower(u.name)  like lower(concat('%', :q, '%')))
            """)
    Page<User> findAppUsers(@Param("q") String q, Pageable pageable);
}
