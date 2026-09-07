package com.example.backend.Cms;

import com.example.backend.Cms.Dev.DevDataSeeder;
import com.example.backend.Enums.PlatformRole;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Security.RoleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lokal sinov hisoblari.
 *
 * <h2>⚠️ Nega bu test bor</h2>
 * Lokal stendda ishlash uchun HAR BIR rol ostida kirib ko'rish kerak:
 * panel bo'limlari, tugmalar va ruxsatlar rolga qarab boshqacha
 * ko'rinadi. Bitta rol uchun hisob bo'lmasa, o'sha rol ostida
 * dastur qanday ko'rinishini lokalda umuman sinab bo'lmaydi —
 * nosozlik faqat serverda, haqiqiy xodim shikoyat qilganda bilinadi.
 *
 * Ro'yxat qo'lda yozilgani uchun yangi rol qo'shgan odam hisob
 * ochishni unutishi juda oson va buni hech narsa aytmasdi. Endi
 * aytadi.
 */
class DevAccountsTest {

    /**
     * Panelga kira oladigan yoki ilovadan foydalanadigan har bir rol —
     * ya'ni {@link PlatformRole} ga bog'langan hammasi.
     */
    private static List<UserRoles> mappedRoles() {
        return Arrays.stream(UserRoles.values())
                .filter(r -> RoleMapper.toPlatformRole(r) != null)
                .toList();
    }

    @Test
    @DisplayName("Har bir haqiqiy rol uchun hisob bor")
    void everyMappedRoleHasAnAccount() {
        Set<UserRoles> seeded = DevDataSeeder.seededRoles();

        assertThat(mappedRoles())
                .as("bu rol ostida lokalda kirib ko'rib bo'lmaydi — "
                        + "DevDataSeeder.STAFF ga hisob qo'shing")
                .allSatisfy(role -> assertThat(seeded).contains(role));
    }

    /**
     * ⚠️ Eski universitet moduli rollari ({@code ROLE_REKTOR},
     * {@code ROLE_STUDENT}, {@code ROLE_TEACHER}, {@code ROLE_DEKAN})
     * ataylab qoldirilgan: ular {@link RoleMapper} da hech narsaga
     * bog'lanmagan, ya'ni bu mahsulotda hech qayerga kira olmaydi.
     *
     * Bu test ularga tasodifan hisob ochilib qo'yilishini ushlaydi —
     * aks holda ro'yxat ishlamaydigan hisoblar bilan shishardi.
     */
    @Test
    @DisplayName("Ishlatilmaydigan rollarga hisob ochilmaydi")
    void doesNotSeedUnmappedRoles() {
        Set<UserRoles> seeded = DevDataSeeder.seededRoles();

        assertThat(seeded)
                .as("bu rollar hech qayerga kira olmaydi — hisob ochish "
                        + "faqat ro'yxatni chalkashtiradi")
                .allSatisfy(role -> assertThat(RoleMapper.toPlatformRole(role)).isNotNull());
    }

    /**
     * ⚠️ Qoida haqiqatan yiqila olishiga ishonch.
     *
     * {@code mappedRoles()} bo'sh qaytsa yuqoridagi birinchi test
     * HAR DOIM yashil bo'lardi — hech narsa tekshirmay turib.
     */
    @Test
    @DisplayName("Qoida haqiqatan yiqila oladi")
    void ruleCanFail() {
        assertThat(mappedRoles()).hasSize(PlatformRole.values().length);
        assertThat(DevDataSeeder.seededRoles()).isNotEmpty();

        // Bog'lanmagan rol haqiqatan mavjud — ikkinchi test bo'sh emas.
        assertThat(RoleMapper.toPlatformRole(UserRoles.ROLE_STUDENT)).isNull();
    }
}
