package com.example.backend.Cms;

import com.example.backend.Cms.Service.SettingKeys;
import com.example.backend.Cms.Service.SettingsService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sozlama qiymatlari YOZISHDA tekshiriladi (ТЗ §40, §41).
 *
 * <h2>Nima uchun bu muhim</h2>
 * Ilgari qiymat hech qanday tekshiruvsiz saqlanardi. Admin narx maydoniga
 * xato yozsa — u saqlanardi, panelda ko'rinardi va admin ishni bajardim
 * deb o'ylardi. Aslida esa o'qishda {@code getMoney} uni tushuna olmay
 * <b>0</b> qaytarardi.
 *
 * Oqibati kalitga qarab turlicha:
 * <ul>
 *   <li>{@code currency.*.rate} — paketlar jimgina sotib olinmaydigan
 *       bo'lib qolardi;</li>
 *   <li>{@code pricing.episode.default} — <b>qism narxi 0 ga aylanardi,
 *       ya'ni pullik kontent bepul bo'lib qolardi.</b></li>
 * </ul>
 *
 * Ikkala holatda ham xato YOZILGAN paytda emas, kimdir oqibatiga duch
 * kelganda bilinardi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettingValidationTest {

    @Autowired private SettingsService settingsService;

    @Nested
    @DisplayName("Pul qiymatlari")
    class Money {

        @Test
        @DisplayName("⚠️ Xato yozilgan NARX rad etiladi")
        void malformedPriceIsRejected() {
            // Bu eng xavfli holat: o'qishda 0 ga aylanib, pullik kontent
            // bepul bo'lib qolardi.
            assertThatThrownBy(() -> settingsService.update(
                    null, SettingKeys.EPISODE_PRICE, "uch ming"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pul qiymati");
        }

        @Test
        @DisplayName("Xato yozilgan KURS ham rad etiladi")
        void malformedRateIsRejected() {
            assertThatThrownBy(() -> settingsService.update(
                    null, SettingKeys.COIN_RATE, "abc"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Manfiy narx rad etiladi")
        void negativePriceIsRejected() {
            assertThatThrownBy(() -> settingsService.update(
                    null, SettingKeys.PREMIERE_PRICE, "-100"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("manfiy");
        }

        @Test
        @DisplayName("To'g'ri narx saqlanadi")
        void validPriceIsAccepted() {
            assertThat(settingsService.update(null, SettingKeys.EPISODE_PRICE, "5000")
                    .getValue()).isEqualTo("5000");
        }

        @Test
        @DisplayName("Kasr qiymat ham to'g'ri — tiyinlar bor")
        void fractionalValueIsAccepted() {
            assertThat(settingsService.update(null, SettingKeys.STAR_RATE, "1250.50")
                    .getValue()).isEqualTo("1250.50");
        }

        @Test
        @DisplayName("Nol mumkin — «belgilanmagan» degani")
        void zeroIsAllowed() {
            // Kurs 0 = hali belgilanmagan. Bu haqiqiy holat, xato emas.
            assertThat(settingsService.update(null, SettingKeys.STAR_RATE, "0")
                    .getValue()).isEqualTo("0");
        }
    }

    @Nested
    @DisplayName("Butun sonlar")
    class Integers {

        @Test
        @DisplayName("Qurilma chegarasi butun son bo'lishi kerak")
        void deviceLimitMustBeInteger() {
            assertThatThrownBy(() -> settingsService.update(
                    null, SettingKeys.DEVICE_LIMIT, "ikkita"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("butun son");
        }

        @Test
        @DisplayName("Kasr qiymat qurilma chegarasi bo'la olmaydi")
        void fractionalDeviceLimitIsRejected() {
            // 2.5 ta qurilma bo'lmaydi.
            assertThatThrownBy(() -> settingsService.update(
                    null, SettingKeys.DEVICE_LIMIT, "2.5"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("To'g'ri qiymat saqlanadi")
        void validIntegerIsAccepted() {
            assertThat(settingsService.update(null, SettingKeys.DEVICE_LIMIT, "3")
                    .getValue()).isEqualTo("3");
        }
    }

    @Nested
    @DisplayName("Ro'yxatdan tanlanadigan qiymatlar")
    class Enums {

        @Test
        @DisplayName("Ruxsat etilmagan qiymat rad etiladi")
        void unknownEnumValueIsRejected() {
            assertThatThrownBy(() -> settingsService.update(
                    null, SettingKeys.CREATOR_RANKING, "TASODIFIY"))
                    .isInstanceOf(BusinessException.class)
                    // Xabar nima kutilayotganini AYTADI — «noto'g'ri qiymat»
                    // degan umumiy xato admin nimani tuzatishni bilmasligiga
                    // olib kelardi.
                    .hasMessageContaining("MANUAL")
                    .hasMessageContaining("STARS");
        }

        @Test
        @DisplayName("Ikkala to'g'ri qiymat ham qabul qilinadi")
        void bothValidValuesAreAccepted() {
            assertThat(settingsService.update(null, SettingKeys.CREATOR_RANKING, "STARS")
                    .getValue()).isEqualTo("STARS");
            assertThat(settingsService.update(null, SettingKeys.CREATOR_RANKING, "MANUAL")
                    .getValue()).isEqualTo("MANUAL");
        }
    }

    @Nested
    @DisplayName("Noma'lum kalit")
    class UnknownKey {

        @Test
        @DisplayName("Kodda e'lon qilinmagan kalit yaratilmaydi")
        void undeclaredKeyIsRejected() {
            // Admin xato yozib, hech kim o'qimaydigan satr yaratib
            // qo'ymasin.
            assertThatThrownBy(() -> settingsService.update(
                    null, "allaqanday.sozlama", "qiymat"))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
