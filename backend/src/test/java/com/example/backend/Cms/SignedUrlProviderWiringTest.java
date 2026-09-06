package com.example.backend.Cms;

import com.example.backend.Cms.Service.Storage.S3Properties;
import com.example.backend.Cms.Service.Video.CdnUrlService;
import com.example.backend.Cms.Service.Video.PresignedUrlProvider;
import com.example.backend.Cms.Service.Video.SecureTokenUrlProvider;
import com.example.backend.Cms.Service.Video.SignedUrlProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Qaysi imzo ta'minotchisi tanlanishi.
 *
 * <h2>⚠️ Nega bu test yozildi</h2>
 * {@code SecureTokenUrlProvider} qo'shilgach imzo ta'minotchisi IKKITA
 * bo'ldi. Ularni oluvchi joylar esa bittasini kutadi:
 *
 * <pre>
 *   private final Optional&lt;SignedUrlProvider&gt; signedUrls;
 * </pre>
 *
 * Ikkita nomzod bo'lganda Spring qaysi birini berishni bilmay
 * {@code NoUniqueBeanDefinitionException} bilan yiqiladi — va bu
 * DASTUR UMUMAN ISHGA TUSHMAYDI degani. Nosozlik kompilyatsiyada
 * ko'rinmaydi, faqat serverda chiqadi va butun sayt o'chib qoladi.
 *
 * {@code @Primary} shuni hal qiladi. Lekin annotatsiya tasodifan
 * o'chirilsa yoki ikkinchi ta'minotchi qo'shilsa, xato yana qaytadi —
 * shuning uchun tekshiruv testga aylantirildi.
 *
 * <h2>Nega {@code ApplicationContextRunner}</h2>
 * Butun ilovani ko'tarish shart emas — bizga faqat shu ikki bean
 * o'rtasidagi tanlov kerak. Runner bazani ham, Flyway'ni ham talab
 * qilmaydi va millisekundlarda ishlaydi.
 */
class SignedUrlProviderWiringTest {

    /**
     * ⚠️ Haqiqiy {@code S3Presigner} yasalmaydi: u tarmoq mijozi va
     * kalit talab qiladi. Bizga faqat bean MAVJUDLIGI kerak.
     */
    @Configuration
    static class Stubs {

        @Bean
        S3Presigner s3Presigner() {
            return mock(S3Presigner.class);
        }

        @Bean
        S3Properties s3Properties() {
            return new S3Properties();
        }

        @Bean
        CdnUrlService cdnUrlService() {
            return new CdnUrlService();
        }
    }

    /**
     * ⚠️ Boot'ning o'giruvchisi QO'LDA ulanadi.
     *
     * Yalang'och {@code ApplicationContextRunner} — bu oddiy Spring
     * konteksti, Boot emas. Unda {@code "4h"} kabi satrni
     * {@link java.time.Duration} ga o'giradigan xizmat yo'q, va
     * ikkala ta'minotchi ham aynan shunday sozlama oladi.
     *
     * Ulanmasa test «bean yaratilmadi» deb yiqilardi — sababi esa
     * tanlov mantig'ida emas, testning o'zida bo'lardi.
     */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getBeanFactory()
                    .setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(Stubs.class)
            .withBean(PresignedUrlProviderHolder.class)
            .withPropertyValues("app.storage.provider=s3");

    /**
     * Ikkala ta'minotchini ham ro'yxatga oladi — Spring'ning o'zi
     * ularni {@code @ConditionalOnProperty} bo'yicha filtrlaydi.
     */
    @Configuration
    @org.springframework.context.annotation.Import({
            PresignedUrlProvider.class, SecureTokenUrlProvider.class})
    static class PresignedUrlProviderHolder {
    }

    /**
     * Kalit YO'Q — eski holat. Secure token bean umuman yaratilmasin.
     *
     * ⚠️ Bu eng muhim holat: kod serverga chiqqanda kalit hali
     * qo'yilmagan bo'ladi. Agar yangi ta'minotchi o'sha zahoti
     * ishlab ketsa, CDN tomonida Secure token yoqilmagani uchun
     * BARCHA video 403 bo'lardi.
     */
    @Test
    @DisplayName("Kalitsiz — eski ta'minotchi ishlaydi")
    void withoutSecretPresignedWins() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(SecureTokenUrlProvider.class);
            assertThat(context).hasSingleBean(PresignedUrlProvider.class);
        });
    }

    /**
     * Kalit BOR — ikkala bean ham mavjud, lekin tanlov aniq.
     *
     * ⚠️ {@code hasNotFailed()} bu yerda testning yuragi: aynan shu
     * holatda kontekst ikkilanib yiqilishi mumkin edi.
     */
    @Test
    @DisplayName("Kalit bilan — secure token yutadi va kontekst yiqilmaydi")
    void withSecretSecureTokenWins() {
        runner.withPropertyValues("app.video.cdn.secure-token.secret=s3cr3t")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecureTokenUrlProvider.class);

                    // ⚠️ Aynan shu chaqiruv ishlatilyotgan joylarni
                    // takrorlaydi: `Optional<SignedUrlProvider>`.
                    Optional<SignedUrlProvider> chosen =
                            Optional.of(context.getBean(SignedUrlProvider.class));

                    assertThat(chosen).containsInstanceOf(SecureTokenUrlProvider.class);
                });
    }

    /**
     * ⚠️ Bo'sh kalit ham «yo'q» deb hisoblanadi.
     *
     * {@code application.properties} da qator qoldirilib qiymati
     * o'chirilishi odatiy hol. Bo'sh kalit bilan yasalgan token
     * ma'nosiz bo'lardi va CDN uni rad etardi.
     */
    @Test
    @DisplayName("Bo'sh kalit yoqilmaydi")
    void blankSecretDoesNotEnable() {
        runner.withPropertyValues("app.video.cdn.secure-token.secret=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(SecureTokenUrlProvider.class);
                });
    }
}
