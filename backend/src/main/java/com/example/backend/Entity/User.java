package com.example.backend.Entity;

import jakarta.persistence.*;
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

    private String password;
    private String name;

    /** Google hisobidan. */
    @Column(unique = true)
    private String email;

    /** Google'dagi barqaror identifikator (ID token'dagi "sub"). */
    @Column(unique = true)
    private String googleSub;

    private String avatarUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;

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
