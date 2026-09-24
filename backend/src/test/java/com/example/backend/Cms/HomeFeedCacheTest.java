package com.example.backend.Cms;

import com.example.backend.Cms.Dto.HomeFeedDto;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Service.HomeFeedService;
import com.example.backend.Cms.Service.UserAdminService;
import com.example.backend.Config.OpenInViewConfig;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bosh sahifa keshi va oqim yo'lining `open-in-view` dan chiqarilishi.
 *
 * <h2>Nima uchun bu ikkisi BIRGA</h2>
 * Ikkalasi ham bir xil turdagi nosozlikni oldini oladi: hech narsa
 * yiqilmaydi, xato logga tushmaydi, lekin yuk ortganda sayt sekinlashadi.
 * Shuning uchun ularni faqat test ushlab turadi — ko'z bilan ko'rib
 * bo'lmaydi.
 */
@SpringBootTest
@ActiveProfiles("test")
class HomeFeedCacheTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private HomeFeedService homeFeedService;
    @Autowired private UserAdminService userAdminService;
    @Autowired private UserRepo userRepo;
    @Autowired private RoleRepo roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    /**
     * ⚠️ Kesh — singleton maydon, ya'ni testlar orasida SAQLANIB QOLADI.
     * Tozalanmasa, oldingi test qoldirgan lenta keyingisiga tushib,
     * natija tasodifiy bo'lardi.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void tozala() {
        ((Map<Object, Object>) ReflectionTestUtils.getField(homeFeedService, "feedCache")).clear();
        ReflectionTestUtils.setField(homeFeedService, "cacheSeconds", 30L);
    }

    @Transactional
    User yangiFoydalanuvchi() {
        int n = SEQ.incrementAndGet();
        Role role = roleRepo.findByName(UserRoles.ROLE_USER);
        if (role == null) {
            int nextId = roleRepo.findAll().stream().mapToInt(Role::getId).max().orElse(0) + 1;
            role = roleRepo.save(new Role(nextId, UserRoles.ROLE_USER));
        }
        return userRepo.save(User.builder()
                .phone(String.format("+99893%07d", 2000000 + n))
                .name("Kesh sinovi " + n)
                .password(passwordEncoder.encode("12345678"))
                .roles(new ArrayList<>(List.of(role)))
                .build());
    }

    @Nested
    @DisplayName("Bosh sahifa keshi")
    class Kesh {

        @Test
        @DisplayName("muddat ichida lenta qayta QURILMAYDI")
        void keshdanQaytadi() {
            HomeFeedDto birinchi = homeFeedService.buildCached(null, Locale.UZ);
            HomeFeedDto ikkinchi = homeFeedService.buildCached(null, Locale.UZ);

            // Aynan bitta obyekt — ya'ni ikkinchi so'rov bazaga bormagan.
            assertThat(ikkinchi).isSameAs(birinchi);
        }

        @Test
        @DisplayName("⚠️ premium foydalanuvchiga reklamali lenta BERILMAYDI")
        @Transactional
        void premiumGaReklamaliKeshTushmaydi() {
            // Avval anonim so'rov — reklamali lenta keshga tushadi.
            HomeFeedDto anonim = homeFeedService.buildCached(null, Locale.UZ);
            assertThat(anonim.getShowAds()).isTrue();

            User user = yangiFoydalanuvchi();
            userAdminService.grantPremium(null, user.getId(), 1, null);

            HomeFeedDto premium = homeFeedService.buildCached(user, Locale.UZ);

            // ⚠️ Kalitdan `showAds` tushib qolsa, bu yerda anonim nusxa
            // qaytardi va pul to'lagan odam reklama ko'rardi. Test aynan
            // shuni ushlaydi — boshqa hech narsa ushlamaydi.
            assertThat(premium.getShowAds()).isFalse();
            assertThat(premium).isNotSameAs(anonim);
        }

        @Test
        @DisplayName("nol muddat — kesh butunlay o'chadi")
        void nolMuddatOchiradi() {
            ReflectionTestUtils.setField(homeFeedService, "cacheSeconds", 0L);

            HomeFeedDto birinchi = homeFeedService.buildCached(null, Locale.UZ);
            HomeFeedDto ikkinchi = homeFeedService.buildCached(null, Locale.UZ);

            assertThat(ikkinchi).isNotSameAs(birinchi);
        }

    }

    /**
     * Video oqimi baza ulanishini band qilib turmasligi.
     *
     * ⚠️ Bu yerda registratsiya TEKSHIRILADI, konfiguratsiya fayli emas.
     * `@Bean` olib tashlansa Spring Boot o'z interceptor'ini HAMMA yo'lga
     * qo'yardi — fayl joyida turgani holda istisno ishlamasdi.
     */
    @Nested
    @DisplayName("open-in-view va video oqimi")
    class OqimYoli {

        @Test
        @DisplayName("oqim yo'li sessiyani ochiq tutMAYDI")
        void oqimYoliIstisno() {
            InterceptorRegistry registry = new InterceptorRegistry();
            new OpenInViewConfig().addInterceptors(registry);

            List<?> hammasi = ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
            assertThat(hammasi).hasSize(1);

            // ⚠️ Tur tekshiruvi ataylab ALOHIDA: istisno olib tashlansa
            // Spring interceptor'ni yo'lsiz ro'yxatga oladi va u boshqa
            // turda bo'ladi. Tekshiruvsiz bu `ClassCastException` bo'lib
            // chiqardi — sababi o'rniga stek izi.
            assertThat(hammasi.get(0))
                    .as("interceptor yo'l shabloni bilan ro'yxatga olinishi kerak")
                    .isInstanceOf(MappedInterceptor.class);

            MappedInterceptor mapped = (MappedInterceptor) hammasi.get(0);

            assertThat(mapped.matches(sorov("/api/v1/app/media/5/raw"))).isFalse();
            assertThat(mapped.matches(sorov("/api/v1/app/home"))).isTrue();
        }

        /**
         * ⚠️ Yo'l oldindan tahlil qilinishi SHART: Spring 6 da
         * `MappedInterceptor` tayyor `RequestPath` ni kutadi va usiz
         * moslikni umuman tekshirmaydi.
         */
        private HttpServletRequest sorov(String yol) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", yol);
            ServletRequestPathUtils.parseAndCache(request);
            return request;
        }
    }
}
