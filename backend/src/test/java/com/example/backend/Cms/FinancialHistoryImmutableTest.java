package com.example.backend.Cms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §42 — «Transaction uchun immutable history saqla. Financial
 * historyni hard delete qilma.»
 *
 * <h2>Nima uchun arxitektura testi</h2>
 * Bu talabni oddiy test qo'riqlay olmaydi. Bugun o'chirish kodi yo'q —
 * lekin {@code DonationRepo} {@code JpaRepository} dan meros oladi, ya'ni
 * {@code delete}, {@code deleteById} va {@code deleteAll} har qanday
 * chaqiruvchiga OCHIQ turadi.
 *
 * Ertaga kimdir «bu sinov donatlarini tozalab tashlay» deb bir qator
 * yozib qo'yishi mumkin. Kod ishlaydi, testlar yashil — moliyaviy tarix
 * esa qaytarib bo'lmaydigan tarzda yo'qoladi.
 *
 * Shuning uchun test KOD MATNINI o'qiydi.
 */
class FinancialHistoryImmutableTest {

    private static final Path MAIN = Path.of("src/main/java/com/example/backend");

    /**
     * Moliyaviy tarixni saqlaydigan repozitoriylar.
     *
     * Bularda o'chirish umuman bo'lmasligi kerak.
     */
    private static final List<String> FINANCIAL_REPOS = List.of(
            "donationRepo",
            "purchaseRepo",
            "subscriptionRepo");

    private static final Pattern DELETE_CALL = Pattern.compile(
            "\\b(" + String.join("|", FINANCIAL_REPOS) + ")\\s*\\.\\s*"
                    + "(delete|deleteById|deleteAll|deleteAllById|removeAll)\\s*\\(");

    @Nested
    @DisplayName("Hard delete yo'q")
    class NoHardDelete {

        @Test
        @DisplayName("⚠️ Moliyaviy yozuvni o'chiradigan kod YO'Q")
        void financialRecordsAreNeverDeleted() throws IOException {
            List<String> offenders = new ArrayList<>();

            try (Stream<Path> files = Files.walk(MAIN)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    Matcher m = DELETE_CALL.matcher(source);
                    while (m.find()) {
                        offenders.add(MAIN.relativize(file) + " → " + m.group());
                    }
                }
            }

            assertThat(offenders)
                    .as("Moliyaviy tarixni o'chiradigan kod topildi. ТЗ §42: "
                            + "«Financial historyni hard delete qilma». Xarid, obuna "
                            + "va donat yozuvlari — pul harakati dalili. Ular "
                            + "o'chirilsa, «bu odam nima uchun premium?» degan "
                            + "savolga javob topib bo'lmaydi va hisob-kitob "
                            + "tiklanmaydi.")
                    .isEmpty();
        }

        @Test
        @DisplayName("Ijobiy nazorat: detektor haqiqatan qidiryapti")
        void detectorActuallyMatches() {
            // ⚠️ Yuqoridagi test BO'SH ro'yxat kutadi. Detektor buzilsa
            // (naqsh xato, yo'l noto'g'ri) u ham bo'sh qaytaradi va test
            // abadiy yashil turadi — hech narsani tekshirmasdan.
            String sample = "        donationRepo.deleteById(id);";

            assertThat(DELETE_CALL.matcher(sample).find())
                    .as("Naqsh o'chirish chaqiruvini topa olmadi — "
                            + "demak asosiy test ham hech narsani tekshirmaydi")
                    .isTrue();
        }

        @Test
        @DisplayName("Naqsh boshqa repozitoriylarga tegmaydi")
        void detectorDoesNotOverreach() {
            // Media va yuklash sessiyalari o'chirilishi MUMKIN — ular
            // moliyaviy yozuv emas.
            String allowed = "        mediaAssetRepo.deleteById(id);";

            assertThat(DELETE_CALL.matcher(allowed).find()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tahrirlash ham yo'q")
    class NoUpdate {

        @Test
        @DisplayName("Donat xizmatida faqat yaratish bor")
        void donationServiceOnlyCreates() throws IOException {
            String source = Files.readString(
                    MAIN.resolve("Cms/Service/DonationService.java"));

            // Yozuv yaratilgandan keyin o'zgarmaydi: miqdorni yoki
            // nishonni keyin tuzatish hisobotni yolg'on qilardi.
            assertThat(source)
                    .doesNotContain("donationRepo.delete")
                    .doesNotContain("setAmount(")
                    .doesNotContain("setKind(");
        }

        @Test
        @DisplayName("Admin kontrollerida donat o'chirish endpointi yo'q")
        void adminCannotDeleteDonations() throws IOException {
            String source = Files.readString(
                    MAIN.resolve("Admin/Controller/MonetizationController.java"));

            assertThat(source)
                    .as("Donat uchun DELETE endpointi bo'lmasligi kerak")
                    .doesNotContain("@DeleteMapping(\"/donations")
                    .doesNotContain("deleteDonation");
        }
    }
}
