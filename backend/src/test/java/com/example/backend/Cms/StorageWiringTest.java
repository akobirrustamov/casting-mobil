package com.example.backend.Cms;

import com.example.backend.Cms.Service.LocalStorageService;
import com.example.backend.Cms.Service.StorageService;
import com.example.backend.Cms.Service.Storage.RoutingStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Saqlash binlari QAYSI rejimda qanday ulanishi.
 *
 * <h2>⚠️ Nega bu test yozildi</h2>
 * Ilgari {@code S3StorageService} va {@code RoutingStorageService} da
 * {@code @ConditionalOnBean} turardi. U faqat AVTOKONFIGURATSIYA
 * klasslari uchun ishonchli: oddiy {@code @Service} da shart komponent
 * SKANERLASH TARTIBIDA baholanadi.
 *
 * Natijada {@code RoutingStorageService} yaratildi, uning
 * {@code S3StorageService} bog'liqligi esa topilmadi — va butun
 * ilova konteksti ko'tarilmay qoldi. **213 ta test yiqildi.**
 *
 * Nosozlik kompilyatsiyada ko'rinmasdi va faqat kontekst ko'tarilganda
 * bilinardi. Shuning uchun ikkala rejim ham test bilan qo'riqlanadi.
 */
class StorageWiringTest {

    @Nested
    @DisplayName("Sukut rejimi — lokal disk")
    @SpringBootTest
    @ActiveProfiles("test")
    class LocalMode {

        @Autowired
        private StorageService storageService;

        @Test
        @DisplayName("`provider` berilmasa LOKAL disk ishlatiladi")
        void defaultsToLocalDisk() {
            // ⚠️ Sukut qiymat MUHIM: mavjud o'rnatishlarda hech qanday
            // sozlama yo'q va ular o'zgarishsiz ishlashda davom etishi
            // kerak (§33).
            assertThat(storageService).isInstanceOf(LocalStorageService.class);
        }
    }

    @Nested
    @DisplayName("S3 rejimi")
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "app.storage.provider=s3",
            // ⚠️ Soxta hisob ma'lumotlari. Bu test TARMOQQA CHIQMAYDI —
            // faqat binlar to'g'ri ulanishini tekshiradi.
            "app.storage.s3.endpoint=https://s3.example.invalid",
            "app.storage.s3.region=ru-1",
            "app.storage.s3.bucket=test-bucket",
            "app.storage.s3.access-key=test-key",
            "app.storage.s3.secret-key=test-secret",
    })
    class S3Mode {

        @Autowired
        private StorageService storageService;

        @Test
        @DisplayName("Yo'naltiruvchi ASOSIY bin bo'ladi va kontekst ko'tariladi")
        void routerBecomesPrimary() {
            // Kontekst ko'tarilishining o'zi ham tekshiruv: ilgari aynan
            // shu yerda yiqilardi.
            assertThat(storageService).isInstanceOf(RoutingStorageService.class);
        }
    }
}
