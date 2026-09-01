package com.example.backend.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "users")
@Entity
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Google orqali kirgan foydalanuvchida telefon boshida bo'lmaydi,
     * shuning uchun nullable. Admin/parol login uchun avvalgidek ishlaydi.
     */
    @Column(unique = true)
    private String phone;

    /**
     * BCrypt hash (§62).
     *
     * {@code WRITE_ONLY}: hash javobda hech qachon chiqmaydi. Ilgari buni
     * bitta qo'lda yozilgan {@code setPassword("")} qatori ushlab
     * turardi — yangi endpoint qo'shilsa hash sizib ketardi. Hash parol
     * emas, lekin uni oflayn buzishga urinish mumkin.
     */
    @com.fasterxml.jackson.annotation.JsonProperty(
            access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String name;

    /**
     * Odam parolni O'ZI o'rnatganmi (V30).
     *
     * <h2>⚠️ Nega {@link #password} ning o'zi yetmaydi</h2>
     * U hech qachon bo'sh bo'lmaydi: Google va SMS orqali yaratilgan
     * hisobga tasodifiy UUID hash qilinadi — «paroli bor, lekin hech
     * kim bilmaydi». Bunday hisob egasi na kira olardi, na qayta
     * ro'yxatdan o'ta olardi.
     *
     * {@code false} — parol hali qo'yilmagan: mobil ro'yxatdan o'tish
     * shu hisobga parol o'rnatishi mumkin. {@code true} — raqam band,
     * kirish faqat parol bilan.
     */
    @Column(name = "password_set", nullable = false)
    private boolean passwordSet;

    /** Google hisobidan. */
    @Column(unique = true)
    private String email;

    /** Google'dagi barqaror identifikator (ID token'dagi "sub"). */
    @Column(unique = true)
    private String googleSub;

    private String avatarUrl;

    /**
     * ⚠️ {@code @BatchSize} — ro'yxatlar uchun (N+1).
     *
     * Rollar EAGER: Spring Security ularni har bir so'rovda talab qiladi,
     * shuning uchun bu to'g'ri tanlov va u O'ZGARTIRILMAYDI.
     *
     * Lekin admin panelida foydalanuvchilar RO'YXAT bo'lib yuklanadi —
     * izoh moderatsiyasi (§34), foydalanuvchilar ro'yxati (§35). O'shanda
     * har bir foydalanuvchining rollari alohida so'rov bilan olinardi:
     * 10 qatorlik sahifa 10 ta qo'shimcha so'rov degani edi.
     *
     * {@code @BatchSize} Hibernate'ga «bittalab emas, elliktalab yukla»
     * deydi. Bu FAQAT yuklash usuli — jadval, ustun va xatti-harakat
     * o'zgarmaydi, ya'ni eski casting kodi ta'sirlanmaydi.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<Role> roles;

    /**
     * Ro'yxatdan o'tgan sana (ТЗ §35, V17).
     *
     * <h2>Nima uchun {@code UserAccount.createdAt} yaramaydi</h2>
     * U bor, lekin BOSHQA narsani bildiradi: hisob satri dangasa
     * yaratiladi — faqat admin biror amal qilganda. Ya'ni ko'pchilik
     * foydalanuvchida u umuman yo'q, bo'lganda ham «admin birinchi marta
     * tekkan vaqt» ni ko'rsatadi.
     *
     * 2020-yilda ro'yxatdan o'tib 2026-yilda bloklangan odam ro'yxatda
     * «2026» bo'lib chiqardi — bo'sh katakdan ham yomon, chunki admin
     * raqamga ishonadi.
     *
     * <h2>Eski satrlar</h2>
     * {@code null} — ular qachon ro'yxatdan o'tgani BILINMAYDI va
     * o'ylab topilgan sana yozilmaydi.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Barcha yaratish yo'llari uchun bitta joy.
     *
     * Ro'yxatdan o'tish, Google orqali kirish, xodim yaratish — hammasi
     * shu yerdan o'tadi, ya'ni eski kodning birortasiga tegilmaydi.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public User(String phone, String password, String name, List<Role> roles) {
        this.phone = phone;
        this.password = password;
        this.name = name;
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Telefon yo'q bo'lsa (Google login) — email ishlatiladi,
     * aks holda Spring Security null username bilan yiqiladi.
     */
    @Override
    public String getUsername() {
        return phone != null ? phone : email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
