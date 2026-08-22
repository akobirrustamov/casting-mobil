package com.example.backend.Cms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §37 — premium huquqlari MARKAZLASHTIRILGAN bo'lsin.
 *
 * <h2>Nima uchun bu arxitektura testi</h2>
 * Buyurtmachi aniq yozgan: «Frontend/mobile ichida scattered condition
 * yozib tashlama.» Ya'ni talab — bitta funksiya ishlashi emas, balki
 * QOIDANING BIR JOYDA turishi.
 *
 * Bunday talabni oddiy test ushlab tura olmaydi: bugun hamma qaror
 * {@code AccessService} da bo'lsa ham, ertaga kimdir shoshib bir joyda
 * {@code account.hasActivePremium()} yozib qo'yishi mumkin. Kod ishlaydi,
 * testlar yashil — lekin qoida ikkiga bo'linadi. Uch oydan keyin ular
 * bir-biridan chetga chiqadi va «nega mobil ilovada ko'rinadi, saytda
 * yo'q» degan xatolar boshlanadi.
 *
 * Shuning uchun test KOD MATNINI o'qiydi: refleksiya metod ichini
 * ko'rmaydi.
 */
class PremiumRightsTest {

    private static final Path SRC =
            Path.of("src/main/java/com/example/backend");

    /** Qaror qabul qiladigan yagona joy. */
    private static final String OWNER = "Cms/Service/AccessService.java";

    /**
     * Bu chaqiruvlar entitlement QARORI hisoblanadi.
     *
     * Ular {@code AccessService} dan tashqarida uchrasa — qoida
     * ko'chirilgan degani.
     */
    private static final List<String> DECISION_CALLS = List.of(
            "hasActivePremium()");

    /**
     * Ataylab ruxsat berilgan joylar.
     *
     * ⚠️ Bu ro'yxatga qo'shishdan oldin o'ylang: har bir yangi qator —
     * qoidaning yana bitta nusxasi.
     */
    private static final List<String> ALLOWED = List.of(
            // Faqat KO'RSATADI: admin paneldagi holat ustuni. Hech qanday
            // kirish qarori qabul qilmaydi.
            "Admin/Dto/AppUserDto.java",
            // Premiumni UZAYTIRADI (yozish). «Mavjud obuna ustiga qo'shilsin»
            // degan savol — kirish qarori emas.
            "Cms/Service/UserAdminService.java",
            // Dev ma'lumotlari; ishlab chiqarishda umuman yuklanmaydi.
            "Cms/Dev/DevDataSeeder.java");

    @Nested
    @DisplayName("Qoida bir joyda")
    class Centralised {

        @Test
        @DisplayName("⚠️ Entitlement qarori AccessService dan tashqarida yo'q")
        void decisionsLiveInOnePlace() throws IOException {
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(SRC)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String relative = SRC.relativize(file).toString().replace('\\', '/');
                    if (relative.equals(OWNER)
                            || ALLOWED.contains(relative)
                            || relative.equals("Cms/Entity/UserAccount.java")) {
                        continue;
                    }
                    String source = Files.readString(file);
                    for (String call : DECISION_CALLS) {
                        if (source.contains(call)) {
                            offenders.add(relative + " → " + call);
                        }
                    }
                }
            }

            assertThat(offenders)
                    .as("Premium qarori AccessService dan tashqariga chiqib ketdi. "
                            + "Buyurtmachi talabi: bu mantiq MARKAZLASHTIRILGAN "
                            + "bo'lsin (ТЗ §37). Nusxa vaqt o'tib asl nusxadan "
                            + "chetga chiqadi va «nega ilovada ko'rinadi, saytda "
                            + "yo'q» degan xatolar boshlanadi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Ijobiy nazorat: detektor haqiqatan qidiryapti")
        void detectorActuallyFindsThings() throws IOException {
            // ⚠️ Yuqoridagi test BO'SH ro'yxat kutadi. Agar detektor
            // buzilgan bo'lsa (masalan yo'l noto'g'ri), u ham bo'sh
            // qaytaradi va test abadiy yashil turadi — hech narsani
            // tekshirmasdan.
            //
            // Shuning uchun: egasining o'zida chaqiruv BOR ekanligini
            // tasdiqlaymiz.
            String owner = Files.readString(SRC.resolve(OWNER));

            assertThat(owner)
                    .as("AccessService da entitlement chaqiruvi topilmadi — "
                            + "demak detektor noto'g'ri joyga qarayapti")
                    .contains("hasActivePremium()");
        }
    }

    @Nested
    @DisplayName("ТЗ §37 dagi huquqlar qamrovi")
    class Coverage {

        @Test
        @DisplayName("Yettala huquq ham AccessService orqali hal bo'ladi")
        void allSevenRightsAreCovered() throws IOException {
            String source = Files.readString(SRC.resolve(OWNER));

            // barcha Premium kontent · seriallar · filmlar · premyeralar
            assertThat(source)
                    .as("Kontent va qism uchun tomosha qarori")
                    .contains("canWatch(User user, Episode episode)")
                    .contains("canWatch(User user, Content content)");

            // reklamasiz ko'rish
            assertThat(source)
                    .as("Reklama ko'rsatiladimi — bu ham premium huquqi")
                    .contains("shouldShowAds");

            // Casting loyihasiga kirish
            assertThat(source)
                    .as("Casting loyihasiga kirish premium huquqi (ТЗ §37)")
                    .contains("canAccessCasting");

            // premyera xaridi
            assertThat(source)
                    .as("Butun premyerani sotib olish")
                    .contains("PREMIERE");
        }

        @Test
        @DisplayName("Kirish siyosatining to'rt holati ham hisobga olingan")
        void allFourPoliciesAreHandled() throws IOException {
            String source = Files.readString(SRC.resolve(OWNER));

            for (String policy : List.of("FREE", "PREMIUM_ONLY",
                    "PURCHASE_ONLY", "PREMIUM_OR_PURCHASE")) {
                assertThat(source)
                        .as("Kirish siyosati hisobga olinmagan: " + policy)
                        .contains(policy);
            }
        }
    }
}
