package com.example.backend.Cms;

import com.example.backend.Admin.Dto.InternalLinkDto;
import com.example.backend.Cms.Entity.Advertisement;
import com.example.backend.Cms.Entity.InternalLink;
import com.example.backend.Cms.Entity.Notification;
import com.example.backend.Cms.Entity.Premiere;
import com.example.backend.Cms.Enums.InternalTargetType;
import com.example.backend.Cms.Enums.LinkType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ТЗ §28 — havola mexanizmi UCHALA modulda ham bir xil.
 *
 * <h2>Talab</h2>
 * «Bir xil internal-link mexanizmini advertisement, premiere va
 * notification uchun reuse qilishga harakat qil.»
 *
 * <h2>Nega bu test kerak</h2>
 * Qayta ishlatish JIMGINA buziladi. Odatiy yo'l: kimdir bildirishnomaga
 * «tez yechim» sifatida alohida {@code linkUrl} maydonini qo'shadi, chunki
 * shu daqiqada osonroq. Natijada uchta modulda uch xil havola mantiqi
 * paydo bo'ladi va mobil ilova har biri uchun alohida kod yozishga majbur
 * bo'ladi.
 *
 * Bu test aynan shu ajralishni ushlaydi: uchala entity ham AYNAN bir xil
 * {@link InternalLink} turidan foydalanishi tekshiriladi.
 *
 * ⚠️ Refleksiya ishlatiladi, chunki savol xatti-harakat haqida emas —
 * TUZILISH haqida: maydon turi bir xilmi.
 */
class InternalLinkReuseTest {

    /** Havolaga ega bo'lishi SHART bo'lgan modullar. */
    private static final Class<?>[] LINKED_ENTITIES = {
            Advertisement.class, Premiere.class, Notification.class};

    // ------------------------------------------------------------ enumlar

    @Nested
    @DisplayName("Turlar ro'yxati")
    class Types {

        @Test
        @DisplayName("Havola turi: NONE, EXTERNAL, INTERNAL")
        void linkTypesMatchSpec() {
            assertThat(LinkType.values()).containsExactlyInAnyOrder(
                    LinkType.NONE, LinkType.EXTERNAL, LinkType.INTERNAL);
        }

        @Test
        @DisplayName("Ichki manzil turlari — ТЗ §28 dagi yettitasi")
        void internalTargetsMatchSpec() {
            assertThat(InternalTargetType.values()).containsExactlyInAnyOrder(
                    InternalTargetType.CONTENT, InternalTargetType.EPISODE,
                    InternalTargetType.CATEGORY, InternalTargetType.CREATOR,
                    InternalTargetType.CASTING, InternalTargetType.PREMIERE,
                    InternalTargetType.OTHER);
        }
    }

    // ------------------------------------------------------ qayta ishlatish

    @Nested
    @DisplayName("Uchala modul bir xil mexanizmni ishlatadi")
    class Reuse {

        @Test
        @DisplayName("Advertisement, Premiere va Notification — bitta InternalLink turi")
        void allThreeShareTheSameType() {
            List<String> missing = new ArrayList<>();

            for (Class<?> entity : LINKED_ENTITIES) {
                boolean found = false;
                for (Field f : entity.getDeclaredFields()) {
                    if (f.getType() == InternalLink.class) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missing.add(entity.getSimpleName());
                }
            }

            assertThat(missing)
                    .as("Bu modullar InternalLink dan foydalanmayapti: %s. "
                            + "Har biri o'z havola mantiqini yozsa, mobil ilova "
                            + "uchtasini alohida qayta ishlashga majbur bo'ladi "
                            + "(ТЗ §28).", missing)
                    .isEmpty();
        }

        /** Havola maydoni nomlari — {@link InternalLink} ichidagilar. */
        private boolean looksLikeLinkField(Field f) {
            String name = f.getName().toLowerCase();
            return name.equals("linkurl")
                    || name.equals("linktype")
                    || name.equals("internaltargettype")
                    || name.equals("internaltargetid");
        }

        private List<String> linkFieldsOf(Class<?> type) {
            List<String> found = new ArrayList<>();
            for (Field f : type.getDeclaredFields()) {
                if (looksLikeLinkField(f)) {
                    found.add(type.getSimpleName() + "." + f.getName());
                }
            }
            return found;
        }

        /**
         * MUSBAT NAZORAT — aniqlagich haqiqatan ishlashini isbotlaydi.
         *
         * ⚠️ Busiz keyingi test bekorga o'tishi mumkin edi: agar
         * {@code looksLikeLinkField} hech qachon {@code true} qaytarmasa,
         * «takrorlanish yo'q» degan xulosa har doim chiqardi va u
         * haqiqatni emas, buzuq aniqlagichni aks ettirardi.
         *
         * {@link InternalLink} ning o'zida bu maydonlar BOR — demak
         * aniqlagich ularni topishi shart.
         */
        @Test
        @DisplayName("Aniqlagich ishlaydi — InternalLink ichida 4 ta maydon topadi")
        void detectorActuallyFindsLinkFields() {
            assertThat(linkFieldsOf(InternalLink.class))
                    .as("Aniqlagich InternalLink ichidagi havola maydonlarini "
                            + "topa olmasa, keyingi test bekorga o'tadi")
                    .hasSize(4);
        }

        @Test
        @DisplayName("Havola maydonlari entity darajasida TAKRORLANMAGAN")
        void noDuplicateLinkFields() {
            List<String> duplicates = new ArrayList<>();
            for (Class<?> entity : LINKED_ENTITIES) {
                // InternalLink ichidagi maydonlar entity darajasida
                // takrorlanmasligi kerak - aks holda ikkita haqiqat manbai
                // paydo bo'ladi va ular bir-biriga zid bo'lib qoladi.
                duplicates.addAll(linkFieldsOf(entity));
            }

            assertThat(duplicates)
                    .as("Bu maydonlar InternalLink ichida bo'lishi kerak, "
                            + "entity darajasida takrorlanmasin: %s", duplicates)
                    .isEmpty();
        }
    }

    // ------------------------------------------------------ xatti-harakat

    @Nested
    @DisplayName("Havola mantiqi")
    class Behaviour {

        @Test
        @DisplayName("NONE — hech qayerga olib bormaydi")
        void noneIsNotActionable() {
            assertThat(new InternalLink().isActionable()).isFalse();
            assertThat(InternalLink.builder().linkType(LinkType.NONE).build()
                    .isActionable()).isFalse();
        }

        @Test
        @DisplayName("EXTERNAL — URL bo'lmasa ishlamaydi")
        void externalNeedsUrl() {
            assertThat(InternalLink.builder().linkType(LinkType.EXTERNAL).build()
                    .isActionable())
                    .as("URL siz tashqi havola tugmasi hech qayerga olib bormaydi")
                    .isFalse();

            assertThat(InternalLink.builder().linkType(LinkType.EXTERNAL)
                    .linkUrl("https://uzcasting.uz").build().isActionable()).isTrue();
        }

        @Test
        @DisplayName("INTERNAL — tur ham, id ham kerak")
        void internalNeedsBothParts() {
            assertThat(InternalLink.builder().linkType(LinkType.INTERNAL)
                    .internalTargetType(InternalTargetType.CONTENT).build()
                    .isActionable())
                    .as("id siz 'qaysi kontent' noma'lum")
                    .isFalse();

            assertThat(InternalLink.builder().linkType(LinkType.INTERNAL)
                    .internalTargetId(7L).build().isActionable())
                    .as("tursiz id 'nimaning 7-si' degan savol qoldiradi")
                    .isFalse();

            assertThat(InternalLink.builder().linkType(LinkType.INTERNAL)
                    .internalTargetType(InternalTargetType.EPISODE)
                    .internalTargetId(7L).build().isActionable()).isTrue();
        }

        @Test
        @DisplayName("DTO ↔ entity aylanishida ma'lumot yo'qolmaydi")
        void dtoRoundTripKeepsEverything() {
            InternalLink original = InternalLink.builder()
                    .linkType(LinkType.INTERNAL)
                    .internalTargetType(InternalTargetType.CREATOR)
                    .internalTargetId(15L)
                    .linkUrl("https://saqlanadi.uz")
                    .build();

            InternalLink back = InternalLinkDto.from(original).toEntity();

            // Bitta DTO uchala modulga xizmat qiladi - u yerda maydon
            // yo'qolsa, uchalasida ham yo'qoladi.
            assertThat(back.getLinkType()).isEqualTo(LinkType.INTERNAL);
            assertThat(back.getInternalTargetType()).isEqualTo(InternalTargetType.CREATOR);
            assertThat(back.getInternalTargetId()).isEqualTo(15L);
            assertThat(back.getLinkUrl()).isEqualTo("https://saqlanadi.uz");
        }
    }
}
