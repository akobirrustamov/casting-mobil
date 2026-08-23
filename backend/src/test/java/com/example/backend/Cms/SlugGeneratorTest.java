package com.example.backend.Cms;

import com.example.backend.Cms.Service.SlugGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SlugGeneratorTest {

    @Test
    @DisplayName("O'zbek lotini: apostrof tashlanadi, chiziqchaga aylanmaydi")
    void uzbekApostrophesAreDropped() {
        assertThat(SlugGenerator.slugify("Qo'shiq va musiqa")).isEqualTo("qoshiq-va-musiqa");
        assertThat(SlugGenerator.slugify("G'alaba")).isEqualTo("galaba");
        // Turli tirnoq belgilari ham
        assertThat(SlugGenerator.slugify("Qo’shiq")).isEqualTo("qoshiq");
    }

    @Test
    @DisplayName("Kirill lotinga o'giriladi")
    void cyrillicIsTransliterated() {
        assertThat(SlugGenerator.slugify("Хозяин моего сердца")).isEqualTo("hozyain-moego-serdtsa");
        assertThat(SlugGenerator.slugify("Ночь звёзд")).isEqualTo("noch-zvyozd");
    }

    @Test
    @DisplayName("Ortiqcha belgilar va chiziqchalar tozalanadi")
    void punctuationIsCleaned() {
        assertThat(SlugGenerator.slugify("  Salom,   dunyo!!!  ")).isEqualTo("salom-dunyo");
        assertThat(SlugGenerator.slugify("---test---")).isEqualTo("test");
    }

    @Test
    @DisplayName("Bo'sh kirish - bo'sh natija")
    void emptyInput() {
        assertThat(SlugGenerator.slugify(null)).isEmpty();
        assertThat(SlugGenerator.slugify("   ")).isEmpty();
    }

    @Test
    @DisplayName("Band slug bo'lsa raqam qo'shiladi")
    void uniqueAppendsSuffix() {
        Set<String> taken = Set.of("film", "film-2");
        assertThat(SlugGenerator.unique("Film", "x", taken::contains)).isEqualTo("film-3");
    }

    @Test
    @DisplayName("Slug yasab bo'lmasa fallback, u ham bo'lmasa 'item'")
    void fallbackIsUsed() {
        assertThat(SlugGenerator.unique("!!!", "Zaxira", s -> false)).isEqualTo("zaxira");
        assertThat(SlugGenerator.unique("!!!", "???", s -> false)).isEqualTo("item");
    }
}
