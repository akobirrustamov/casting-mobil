package com.example.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sozlama jar YONIDAN ham o'qiladi.
 *
 * <h2>⚠️ Qanday nosozlikni qo'riqlaydi</h2>
 * Spring Boot sozlamani JORIY PAPKADAN qidiradi, jar turgan
 * papkadan emas. Serverdagi tabiiy buyruq esa shunday yoziladi:
 *
 * <pre>
 *   root@server:~# java -jar /opt/uzcasting/backend.jar
 * </pre>
 *
 * Joriy papka bu yerda {@code /root}, sozlama esa
 * {@code /opt/uzcasting} da — ular uchrashmaydi va ilova «kalit
 * yo'q» deb to'xtaydi, garchi fayl serverda BOR bo'lsa ham.
 *
 * Foydalanuvchi bunga TO'RT MARTA urildi. Har safar javob «`cd`
 * qiling» edi — javob to'g'ri, lekin muammoni yechmaydi: buyruq
 * tabiiy ko'rinadi va xato jimgina takrorlanaveradi.
 *
 * <h2>⚠️ Bu testning qamrovi CHEKLANGAN — buni ochiq aytish kerak</h2>
 * U faqat tizim xususiyati to'g'ri o'rnatilishini tekshiradi.
 * Testda {@code java.class.path} ko'p elementli bo'ladi, ya'ni
 * «haqiqiy jar boshqa papkadan» holati bu yerda TAKRORLANMAYDI.
 *
 * Va bu chegara aynan shu tuzatishda og'ridi: birinchi variant
 * {@code getProtectionDomain()} dan foydalanardi va fat jar ichida
 * ISHLAMASDI — istisno {@code catch} da jimgina yutilardi. Uchala
 * test o'shanda ham YASHIL edi.
 *
 * Nosozlik faqat haqiqiy jar'ni begona papkadan ishga tushirganda
 * ko'rindi. Shuning uchun bu yo'l har o'zgarishda QO'LDA sinaladi:
 *
 * <pre>
 *   cd /boshqa/papka &amp;&amp; java -jar /opt/uzcasting/backend.jar
 * </pre>
 */
class ConfigNextToJarTest {

    private static final String KEY = "spring.config.additional-location";

    @AfterEach
    void tearDown() {
        System.clearProperty(KEY);
    }

    /**
     * ⚠️ ENG MUHIM TEKSHIRUV — aniq ko'rsatilgan yo'lga TEGILMAYDI.
     *
     * Foydalanuvchi {@code --spring.config.additional-location=...}
     * bersa u g'olib chiqishi kerak. Aks holda qulaylik uchun
     * qo'shilgan kod aniq berilgan buyruqni bosib qo'yardi — va buni
     * tushunish juda qiyin bo'lardi.
     */
    @Test
    @DisplayName("Aniq ko'rsatilgan yo'l USTUN turadi")
    void explicitLocationWins() {
        System.setProperty(KEY, "file:/mening/yolim/");

        ReflectionTestUtils.invokeMethod(BackendApplication.class, "useConfigNextToJar");

        assertThat(System.getProperty(KEY))
                .as("aniq berilgan yo'l o'zgarmasin")
                .isEqualTo("file:/mening/yolim/");
    }

    /**
     * IDE va testlarda kod jar'dan yuklanmaydi — o'shanda hech narsa
     * qo'shilmasligi kerak, sukut xatti-harakat saqlansin.
     */
    @Test
    @DisplayName("Jar bo'lmasa — aralashmaydi")
    void doesNothingOutsideJar() {
        ReflectionTestUtils.invokeMethod(BackendApplication.class, "useConfigNextToJar");

        assertThat(System.getProperty(KEY))
                .as("classes papkasidan ishga tushganda sukut yo'l saqlansin")
                .isNull();
    }

    /**
     * ⚠️ {@code optional:} prefiksi SHART.
     *
     * Usiz jar yonida fayl bo'lmagan holatda Spring «config data
     * location does not exist» deb YIQILARDI — ya'ni qulaylik uchun
     * qo'shilgan narsa yangi nosozlik manbaiga aylanardi. Aynan shu
     * holat lokal sinovda (`--spring.profiles.active=dev`) yuzaga
     * keladi.
     *
     * Yuqoridagi ikki test buni ko'ra olmaydi, chunki ularda kod
     * jar'dan yuklanmaydi. Shuning uchun manba matnidan tekshiramiz.
     */
    @Test
    @DisplayName("optional: prefiksi ishlatiladi")
    void usesOptionalPrefix() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/example/backend/BackendApplication.java"));

        assertThat(source)
                .as("usiz jar yonida fayl bo'lmasa ilova yiqilardi")
                .contains("\"optional:file:\"");
    }
}
