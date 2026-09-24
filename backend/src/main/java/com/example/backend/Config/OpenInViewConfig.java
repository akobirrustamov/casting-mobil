package com.example.backend.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * `open-in-view` — video oqimidan TASHQARI hamma joyda.
 *
 * <h2>Nima jim buziladi</h2>
 * `spring.jpa.open-in-view=true` bo'lsa, Spring Boot Hibernate sessiyasini
 * BUTUN so'rov davomida ochiq tutadi. Oddiy so'rov uchun bu muammo emas —
 * u millisekundlarda tugaydi.
 *
 * Lekin video oqimi millisekundda tugamaydi. `/api/v1/app/media/{id}/raw`
 * javobi foydalanuvchi videoni tomosha qilgunicha ochiq turadi — daqiqalar.
 * Har bir tomoshabin shu vaqt davomida bittadan baza ulanishini band qilib
 * turardi. Hovuzda o'ntacha ulanish bor: o'nta odam film ko'ra boshlagach,
 * o'n birinchi so'rov — kirish, qidiruv, admin paneli, hammasi — ulanish
 * kutib turib qolardi. Tashqaridan bu «sayt osilib qoldi» ko'rinadi, va
 * loglarda hech qanday xato bo'lmaydi: hech narsa yiqilmagan, shunchaki
 * hamma navbatda turibdi.
 *
 * <h2>Nega shunday yozilgan</h2>
 * Boot'ning o'z `JpaWebConfiguration` si interceptor'ni `@ConditionalOnMissingBean`
 * ostida ro'yxatdan o'tkazadi. Quyidagi `@Bean` e'lon qilingani uchun Boot
 * chekinadi va ro'yxatga olish SHU YERDAN boradi — ya'ni istisnolar bilan.
 *
 * ⚠️ `@Bean` ni olib tashlab, faqat `addInterceptors` qoldirib bo'lmaydi:
 * u holda Boot o'zining interceptor'ini HAM ro'yxatdan o'tkazadi va sessiya
 * oqim yo'lida baribir ochiq qolardi — ya'ni tuzatish ko'rinib turib
 * ishlamasdi.
 */
@Configuration
@ConditionalOnProperty(name = "spring.jpa.open-in-view", havingValue = "true", matchIfMissing = true)
public class OpenInViewConfig implements WebMvcConfigurer {

    /**
     * Sessiya ochiq TUTILMAYDIGAN yo'llar.
     *
     * ⚠️ Bu yerga faqat javobi UZOQ oqiladigan yo'l qo'shiladi. Oddiy
     * endpoint qo'shilsa, uning ichidagi `lazy` bog'lanishlar
     * `LazyInitializationException` bilan yiqiladi.
     */
    static final String[] STREAMING_PATHS = { "/api/v1/app/media/*/raw" };

    @Bean
    public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
        return new OpenEntityManagerInViewInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addWebRequestInterceptor(openEntityManagerInViewInterceptor())
                .excludePathPatterns(STREAMING_PATHS);
    }
}
