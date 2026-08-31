package com.example.backend;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Skanerlanadigan paketlar ATAYLAB sanab o'tilgan.
 *
 * Sukut bo'yicha Spring butun com.example.backend ni skanerlagan bo'lardi, lekin
 * bu yerda cheklov mavjud edi (faqat Entity va Repository). UZCASTING CMS moduli
 * alohida Cms.* paketida turadi, shuning uchun ular ham qo'shildi.
 *
 * Yangi modul qo'shilganda shu ro'yxatni yangilash ESDAN CHIQMASIN - aks holda
 * repozitoriylar topilmay ilova ko'tarilmaydi.
 */
@SpringBootApplication
// Analitika agregatsiyasi fon vazifasi sifatida ishlaydi (AnalyticsService).
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.example.backend.Repository",
        "com.example.backend.Cms.Repository"
})
@EntityScan(basePackages = {
        "com.example.backend.Entity",
        "com.example.backend.Cms.Entity"
})
public class BackendApplication {

    public static void main(String[] args) {
        useConfigNextToJar();
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Jar YONIDAGI {@code application.properties} ni ham o'qiydigan
     * qiladi — joriy papka qayerda bo'lishidan qat'iy nazar.
     *
     * <h2>⚠️ Qanday nosozlikni bartaraf qiladi</h2>
     * Spring Boot sukut bo'yicha sozlamani JORIY PAPKADAN
     * ({@code file:./}) qidiradi, jar turgan papkadan emas. Serverda
     * esa tabiiy buyruq shunday yoziladi:
     *
     * <pre>
     *   root@server:~# java -jar /opt/uzcasting/backend.jar
     * </pre>
     *
     * Joriy papka bu yerda {@code /root}, sozlama esa
     * {@code /opt/uzcasting} da. Ikkisi uchrashmaydi va ilova
     * «kalit yo'q» deb to'xtaydi — garchi fayl serverda BOR bo'lsa
     * ham.
     *
     * Foydalanuvchi bunga TO'RT MARTA urildi. Har safar javob
     * «`cd` qiling» edi va bu javob to'g'ri, lekin muammoni
     * yechmaydi: buyruq tabiiy ko'rinadi va xato jimgina takrorlanadi.
     *
     * <h2>Nega aynan shu yechim</h2>
     * {@code additional-location} sukut joylarni ALMASHTIRMAYDI,
     * ustiga QO'SHADI. Ya'ni {@code ./application.properties} ham
     * ishlayveradi va eski odat buzilmaydi.
     *
     * ⚠️ {@code optional:} prefiksi SHART. Usiz jar yonida fayl
     * bo'lmagan holatda ilova «fayl topilmadi» deb yiqilardi — ya'ni
     * qulaylik uchun qo'shilgan narsa yangi nosozlik manbaiga
     * aylanardi.
     *
     * ⚠️ Buyruq qatoridagi {@code --spring.config.additional-location}
     * BUNDAN USTUN turadi: Spring'da buyruq argumenti tizim
     * xususiyatidan yuqori. Ya'ni aniq ko'rsatilgan yo'l g'olib
     * chiqadi.
     */
    private static void useConfigNextToJar() {
        // Aniq ko'rsatilganga TEGMAYMIZ.
        if (System.getProperty(CONFIG_LOCATION) != null) {
            return;
        }

        Path jarDir = jarDirectory();
        if (jarDir == null) {
            return;
        }

        System.setProperty(CONFIG_LOCATION,
                "optional:file:" + jarDir + File.separator);
    }

    private static final String CONFIG_LOCATION = "spring.config.additional-location";

    /**
     * Ishlayotgan jar turgan papka, yoki {@code null}.
     *
     * <h2>⚠️ Nega {@code getProtectionDomain()} EMAS</h2>
     * Birinchi variant kod manbasini shundan olardi va u ISHLAMADI.
     * Sababi: Spring Boot'ning «fat jar» ida asosiy sinf arxiv
     * ICHIDAN yuklanadi va manzil ichma-ich bo'ladi:
     *
     * <pre>
     *   jar:file:/opt/uzcasting/backend.jar!/BOOT-INF/classes!/
     * </pre>
     *
     * {@code Paths.get(...)} bunday URI'ni qabul qilmaydi va istisno
     * tashlaydi. Istisno esa {@code catch} da jimgina yutilardi —
     * ya'ni tuzatish o'zi jimgina ishlamasdi. Buni faqat haqiqiy
     * jar'ni ishga tushirib bilish mumkin edi.
     *
     * <h2>Nega {@code java.class.path}</h2>
     * {@code java -jar X.jar} da uning qiymati AYNAN {@code X.jar}
     * bo'ladi — bu JVM ning hujjatlashtirilgan xatti-harakati.
     *
     * ⚠️ {@code null} qaytishi ODATIY hol: testlarda va IDE'da
     * classpath ko'p elementli bo'ladi. O'shanda hech narsa
     * qo'shilmaydi va sukut xatti-harakat saqlanadi.
     */
    private static Path jarDirectory() {
        String classPath = System.getProperty("java.class.path");
        if (classPath == null || classPath.isBlank()) {
            return null;
        }

        // Bir nechta element - demak `java -jar` emas (IDE, test).
        if (classPath.contains(File.pathSeparator) || !classPath.endsWith(".jar")) {
            return null;
        }

        Path jar = Paths.get(classPath).toAbsolutePath();
        if (!Files.isRegularFile(jar)) {
            return null;
        }
        return jar.getParent();
    }

}
