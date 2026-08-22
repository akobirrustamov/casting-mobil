package com.example.backend.Admin;

import com.example.backend.Admin.Dto.CreatorSaveRequest;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Service.TaxonomyService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §54 — ijodkorni tanlash va joyida yaratish.
 *
 * <h2>Oqim</h2>
 * <ol>
 *   <li>mavjud ijodkorni qidirish;</li>
 *   <li>topilmasa — joyida yaratish;</li>
 *   <li>yaratilgach DARHOL kontentga biriktirish.</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreatorSelectionTest {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final Path PANEL = Path.of("../frontend/src/adminpanel");

    @Autowired private TaxonomyService taxonomyService;

    private CreatorSaveRequest request(boolean allLocales) {
        CreatorSaveRequest r = new CreatorSaveRequest();
        r.setActive(true);
        Map<Locale, CreatorSaveRequest.NameDto> tr = new LinkedHashMap<>();
        int n = SEQ.incrementAndGet();

        CreatorSaveRequest.NameDto uz = new CreatorSaveRequest.NameDto();
        uz.setDisplayName("Ijodkor " + n);
        tr.put(Locale.UZ, uz);

        if (allLocales) {
            CreatorSaveRequest.NameDto ru = new CreatorSaveRequest.NameDto();
            ru.setDisplayName("Автор " + n);
            tr.put(Locale.RU, ru);
            CreatorSaveRequest.NameDto en = new CreatorSaveRequest.NameDto();
            en.setDisplayName("Creator " + n);
            tr.put(Locale.EN, en);
        }
        r.setTranslations(tr);
        return r;
    }

    // ------------------------------------------------------------ backend

    @Nested
    @DisplayName("Yaratish qoidalari")
    class Creation {

        @Test
        @DisplayName("⚠️ FAOL ijodkor uchun uchala til majburiy")
        void activeCreatorNeedsAllThreeLanguages() {
            // Panel shu sababli uchala tilni so'raydi. Faqat o'zbekchasini
            // so'rab, ijodkorni NOFAOL yaratish ham mumkin edi — lekin u
            // holda kontentga biriktirilgan ijodkor hech qayerda
            // ko'rinmasdi va admin buning sababini tushunmasdi.
            assertThatThrownBy(() -> taxonomyService.saveCreator(null, null, request(false)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RU");
        }

        @Test
        @DisplayName("Uchala til to'liq bo'lsa yaratiladi")
        void fullyTranslatedCreatorIsCreated() {
            Creator created = taxonomyService.saveCreator(null, null, request(true));

            assertThat(created.getId()).isNotNull();
            assertThat(created.getActive()).isTrue();
            assertThat(created.getTranslations()).hasSize(3);
        }

        @Test
        @DisplayName("Ism va familiya ham yetarli — ko'rinadigan ism shart emas")
        void firstAndLastNameAreEnough() {
            CreatorSaveRequest r = new CreatorSaveRequest();
            r.setActive(true);
            Map<Locale, CreatorSaveRequest.NameDto> tr = new LinkedHashMap<>();
            for (Locale l : List.of(Locale.UZ, Locale.RU, Locale.EN)) {
                CreatorSaveRequest.NameDto n = new CreatorSaveRequest.NameDto();
                n.setFirstName("Ism" + SEQ.incrementAndGet());
                n.setLastName("Familiya");
                tr.put(l, n);
            }
            r.setTranslations(tr);

            assertThat(taxonomyService.saveCreator(null, null, r).getId()).isNotNull();
        }
    }

    // -------------------------------------------------------------- panel

    @Nested
    @DisplayName("Panel oqimi (ТЗ §54)")
    class PanelFlow {

        private String credits() throws IOException {
            return Files.readString(PANEL.resolve("pages/editor/CreditsTab.jsx"));
        }

        private String quick() throws IOException {
            return Files.readString(PANEL.resolve("components/CreatorQuickCreate.jsx"));
        }

        @Test
        @DisplayName("1. Mavjud ijodkorni qidirish")
        void searchExists() throws IOException {
            assertThat(credits()).contains("creatorQuery");
        }

        @Test
        @DisplayName("⚠️ 2. Topilmasa JOYIDA yaratish taklif qilinadi")
        void offersInlineCreationWhenNothingFound() throws IOException {
            String src = credits();

            // Ilgari admin muharrirni tark etib, «Ijodkorlar» bo'limiga
            // o'tib, yaratib, qaytishi kerak edi — va saqlanmagan
            // o'zgarishlarini yo'qotardi.
            assertThat(src).contains("CreatorQuickCreate");
            assertThat(src)
                    .as("Taklif faqat natija bo'lmaganda chiqishi kerak")
                    .contains("suggestions.length === 0");
        }

        @Test
        @DisplayName("3. Yaratilgach DARHOL biriktiriladi")
        void createdCreatorIsAttachedImmediately() throws IOException {
            String src = credits();

            // `onCreated` ichida biriktirish bo'lishi shart: aks holda
            // admin yangi yaratgan odamini ro'yxatdan qayta qidirardi.
            int onCreated = src.indexOf("onCreated=");
            assertThat(onCreated).isGreaterThan(0);
            assertThat(src.substring(onCreated, Math.min(onCreated + 400, src.length())))
                    .contains("attach(created.id)");
        }

        @Test
        @DisplayName("ТЗ dagi oltala maydon ham bor")
        void quickFormHasEveryRequestedField() throws IOException {
            String src = quick();

            // image · first name · last name · middle name · display name
            assertThat(src).as("rasm").contains("MediaField");
            assertThat(src).as("ism").contains("firstName");
            assertThat(src).as("familiya").contains("lastName");
            assertThat(src).as("otasining ismi").contains("middleName");
            assertThat(src).as("ko'rinadigan ism").contains("displayName");
        }

        @Test
        @DisplayName("Kasb BOG'LANISHDA saqlanadi, ijodkorda emas")
        void professionBelongsToTheCredit() throws IOException {
            // ТЗ §24: bitta odam bir kinoda aktyor, boshqasida rejissyor
            // bo'lishi mumkin. Kasb ijodkorda saqlansa, uni har kino
            // uchun o'zgartirib bo'lmasdi.
            assertThat(credits()).contains("profession");
            assertThat(quick())
                    .as("Tez yaratish formasi kasbni ijodkorga yozmasligi kerak")
                    .doesNotContain("profession:");
        }

        @Test
        @DisplayName("⚠️ Uch til kerakligi BOSISHDAN OLDIN aytiladi")
        void missingLanguagesAreShownBeforeSubmit() throws IOException {
            String src = quick();

            // Aks holda admin formani to'ldirib, saqlashga bosgandan
            // keyin xato ko'rardi va qaysi tabga qaytishni o'zi topishi
            // kerak bo'lardi.
            assertThat(src).contains("missing");
            assertThat(src).contains("disabled={saving || missing.length > 0}");
        }

        @Test
        @DisplayName("Biriktirilgan ijodkor taklif ro'yxatida takrorlanmaydi")
        void attachedCreatorsAreNotSuggestedAgain() throws IOException {
            assertThat(credits()).contains("attachedIds");
        }
    }
}
