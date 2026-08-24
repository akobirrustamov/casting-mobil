package com.example.backend;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Har bir test yurishida bazani NOLDAN quradi.
 *
 * <h2>Nima uchun kerak</h2>
 * Testlar qat'iy telefon raqamlari va nomlari bilan ma'lumot yaratadi
 * ({@code +998 90 111 11 11} kabi). Baza oldingi yurishdan qolgan
 * yozuvlar bilan boshlansa, ular {@code 409 DUPLICATE_PHONE} oladi va
 * o'nlab test yiqiladi — kod butunlay to'g'ri bo'lsa ham.
 *
 * Bu «testlar beqaror» degan eng yomon turdagi holat: natija kodga
 * emas, bazaning oldingi holatiga bog'liq bo'ladi. Bir marta o'tadi,
 * ikkinchi marta yiqiladi va nima o'zgargani tushunarsiz.
 *
 * <h2>Nima uchun aynan `clean`</h2>
 * Har bir testni tranzaksiyaga o'rash mumkin emas: ular {@code MockMvc}
 * orqali HTTP so'rov yuboradi va server o'z tranzaksiyasida ishlaydi.
 * Bazani tozalash — yagona ishonchli chegara.
 *
 * Yon foyda: migratsiyalarning O'ZI ham har yurishda noldan
 * qo'llanadi. Buzuq yoki tartibi noto'g'ri migratsiya darhol
 * ko'rinadi, ishlab chiqarishga chiqqanda emas (ТЗ §91).
 *
 * <h2>⚠️ Bu FAQAT `test` profilida</h2>
 * {@code @Profile("test")} va `application-test.properties` dagi
 * alohida baza ikkalasi ham shart. Ishlab chiqarish konfiguratsiyasida
 * {@code spring.flyway.clean-disabled=true} qoladi va uni
 * {@code DatabaseRulesTest} qo'riqlaydi — ya'ni bu bean u yerga
 * hech qachon yetib bormaydi.
 */
/*
 * ⚠️ `@TestConfiguration` EMAS, `@Configuration`.
 *
 * `@TestConfiguration` komponent skanerlashdan ATAYLAB chiqarilgan va
 * uni har bir test klassida qo'lda `@Import` qilish kerak bo'lardi —
 * bitta joyda unutilsa, o'sha test yana eski ma'lumot ustida ishlardi.
 * Bu klass faqat `src/test/java` da, ya'ni ishlab chiqarish
 * classpath'iga umuman tushmaydi.
 */
@Configuration
@Profile("test")
public class TestDatabaseReset {

    @Bean
    public FlywayMigrationStrategy cleanBeforeMigrate() {
        return flyway -> {
            assertTestDatabase(flyway);
            flyway.clean();
            flyway.migrate();
        };
    }

    /**
     * Xavfsizlik to'sig'i: noto'g'ri konfiguratsiya bilan ishlab
     * chiquvchining haqiqiy bazasini tozalab yubormaslik uchun.
     *
     * ⚠️ Bu ortiqcha ehtiyot emas. `spring.datasource.url` ni
     * environment orqali almashtirish oson, va xato qilingan taqdirda
     * bu bean butun bazani so'roqsiz o'chirardi. Nom tekshiruvi arzon,
     * yo'qotilgan ma'lumot esa qaytmaydi.
     */
    private void assertTestDatabase(Flyway flyway) {
        // ⚠️ `getConfiguration().getUrl()` bu yerda DOIM `null`.
        //
        // Spring Boot Flyway'ni manzil bilan emas, tayyor `DataSource`
        // bean bilan sozlaydi — ya'ni URL Flyway konfiguratsiyasiga
        // umuman tushmaydi. Uni ulanish metama'lumotidan olamiz.
        String url;
        try (java.sql.Connection connection =
                     flyway.getConfiguration().getDataSource().getConnection()) {
            url = connection.getMetaData().getURL();
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(
                    "Test bazasiga ulanib bo'lmadi, tozalash to'xtatildi", e);
        }

        if (url == null || !url.contains("test")) {
            throw new IllegalStateException(
                    "Test bazasi nomida 'test' bo'lishi SHART. Kelgan manzil: " + url
                            + ". Tozalash to'xtatildi — noto'g'ri bazani o'chirib "
                            + "yubormaslik uchun.");
        }
    }
}
