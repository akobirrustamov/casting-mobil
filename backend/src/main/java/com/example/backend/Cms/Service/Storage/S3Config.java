package com.example.backend.Cms.Service.Storage;

import com.example.backend.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 mijozi — faqat {@code app.storage.provider=s3} bo'lganda.
 *
 * <h2>Nega aniq kalit, avtomatik aniqlash emas</h2>
 * Avval bu yerda «sozlamalar to'liq bo'lsa yoqiladi» mantig'i bor edi.
 * U jimgina xatoga olib borardi: bitta environment o'zgaruvchisi
 * yozilmay qolsa, tizim hech nima demasdan lokal diskka qaytardi va
 * buni faqat prod'da, fayllar noto'g'ri joyda ekanini ko'rib bilinardi.
 *
 * Endi tanlov OSHKORA. {@code s3} so'ralsa-yu sozlama to'liq bo'lmasa —
 * ilova ishga tushmaydi va nima yetishmayotganini aytadi.
 *
 * <h2>Sukut qiymat — {@code local}</h2>
 * Mavjud o'rnatishlar va testlar hech qanday o'zgarishsiz ishlashda
 * davom etadi.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties properties) {
        require(properties);
        log.info("S3 saqlash yoqildi: bucket={} region={}",
                properties.getBucket(), properties.getRegion());
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials(properties))
                // ⚠️ Path-style MAJBURIY. Timeweb virtual-host uslubidagi
                // manzilni (bucket.s3.twcstorage.ru) qo'llab-quvvatlamaydi,
                // AWS SDK esa sukut bo'yicha aynan shu uslubni tanlaydi.
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * Presigned havola yasovchi.
     *
     * S3Client'dan alohida bin: imzolash uchun tarmoq ulanishi kerak
     * emas, lekin bir xil hisob ma'lumotlari va manzil kerak.
     */
    @Bean
    public S3Presigner s3Presigner(S3Properties properties) {
        require(properties);
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * ⚠️ Xato xabarida faqat MAYDON NOMLARI bo'ladi, qiymatlar emas —
     * aks holda yarim yozilgan kalit logga tushardi.
     */
    private void require(S3Properties properties) {
        if (properties.isConfigured()) {
            return;
        }
        throw new BusinessException("S3_NOT_CONFIGURED",
                "app.storage.provider=s3, lekin sozlama to'liq emas. Yetishmayapti: "
                        + String.join(", ", properties.missingFields()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private StaticCredentialsProvider credentials(S3Properties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    }
}
