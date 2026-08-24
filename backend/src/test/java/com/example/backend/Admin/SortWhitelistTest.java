package com.example.backend.Admin;

import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §95 — ro'yxatlarda saralash.
 *
 * <h2>Nega oq ro'yxat</h2>
 * Klientga istalgan maydon bo'yicha saralashga ruxsat berish uch xil
 * muammoni ochadi: indekssiz ustun butun jadvalni skanerlaydi (§66),
 * mavjud bo'lmagan maydon 500 beradi, va sinov yo'li bilan ichki
 * tuzilishni aniqlash mumkin bo'ladi.
 */
class SortWhitelistTest {

    private static final SortWhitelist SORT = SortWhitelist.of("createdAt")
            .add("name")
            .add("views", "viewCount");

    @Test
    @DisplayName("Ruxsat berilgan ustun qabul qilinadi")
    void allowedColumnWorks() {
        Sort sort = SORT.resolve("name", "asc");

        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Klient nomi entity maydoniga o'giriladi")
    void keyMapsToEntityField() {
        // Panel `views` deydi, bazada esa ustun `viewCount`.
        Sort sort = SORT.resolve("views", "desc");

        assertThat(sort.getOrderFor("viewCount")).isNotNull();
        assertThat(sort.getOrderFor("views")).isNull();
    }

    @Test
    @DisplayName("⚠️ Ro'yxatda yo'q ustun 422 beradi, 500 emas")
    void unknownColumnIsRejected() {
        // Bu klient xatosi: xabar aniq bo'lishi va qaysi ustunlar
        // mumkinligini aytishi kerak.
        assertThatThrownBy(() -> SORT.resolve("password", "asc"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNPROCESSABLE_ENTITY)
                .hasMessageContaining("createdAt");
    }

    @Test
    @DisplayName("Bo'sh so'rovda standart ustun ishlatiladi")
    void blankFallsBackToDefault() {
        assertThat(SORT.resolve(null, null).getOrderFor("createdAt")).isNotNull();
        assertThat(SORT.resolve("  ", null).getOrderFor("createdAt")).isNotNull();
    }

    @Test
    @DisplayName("Noma'lum yo'nalish kamayish tartibi")
    void unknownDirectionIsDescending() {
        // Ro'yxatlar odatda yangidan eskiga qaraladi.
        assertThat(SORT.resolve("name", "chalkash").getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(SORT.resolve("name", null).getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Standart ustun ham ro'yxatga kiradi")
    void defaultIsAllowedExplicitly() {
        assertThat(SORT.keys()).contains("createdAt", "name", "views");
    }
}
