package com.example.backend.Cms;

import com.example.backend.Admin.Dto.CreatorSaveRequest;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Entity.HomepageSection;
import com.example.backend.Cms.Entity.HomepageSectionTranslation;
import com.example.backend.Cms.Enums.HomepageSectionType;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.CreatorRepo;
import com.example.backend.Cms.Service.HomepageService;
import com.example.backend.Cms.Service.SettingKeys;
import com.example.backend.Cms.Service.SettingsService;
import com.example.backend.Cms.Service.TaxonomyService;
import com.example.backend.exceptions.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ТЗ §25 — «Mashhur ijodkorlar» bo'limi.
 *
 * <h2>Qanday bo'shliq yopildi</h2>
 * Bo'lim {@code HomepageSectionType.POPULAR_CREATORS} sifatida sozlamada
 * bor edi, {@code CreatorRepo} da featured so'rovi ham bor edi — lekin
 * ularni BOG'LAYDIGAN kod yo'q edi. Ya'ni bo'lim mavjud, mazmuni esa
 * hech qachon hisoblanmasdi va so'rov o'lik kod bo'lib turardi.
 *
 * <h2>Analitikaga tayyorlik</h2>
 * ТЗ: «Hozir manual featured/sort yetarli, ammo arxitektura analytics
 * rankingga mos bo'lsin.» Tartib sozlamadan o'qiladi — kod o'zgartirmasdan
 * MANUAL dan STARS ga o'tish mumkin.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FeaturedCreatorsTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private HomepageService homepageService;
    @Autowired private TaxonomyService taxonomyService;
    @Autowired private CreatorRepo creatorRepo;
    @Autowired private SettingsService settingsService;
    @Autowired private com.example.backend.Cms.Repository.PlatformSettingRepo settingRepo;

    private Creator creator(String name, boolean featured, int sortOrder,
                            boolean active, long stars) {
        CreatorSaveRequest req = new CreatorSaveRequest();
        req.setActive(active);
        req.setFeatured(featured);
        req.setSortOrder(sortOrder);
        int n = SEQ.incrementAndGet();
        req.setTranslations(Map.of(
                Locale.UZ, nameDto(name + n),
                Locale.RU, nameDto(name + n),
                Locale.EN, nameDto(name + n)));
        Creator saved = taxonomyService.saveCreator(null, null, req);

        // Stars donat oqimi orqali to'planadi; u hali ulanmagan, shuning
        // uchun testda to'g'ridan-to'g'ri qo'yamiz.
        saved.setStarsReceived(stars);
        return creatorRepo.save(saved);
    }

    private CreatorSaveRequest.NameDto nameDto(String first) {
        CreatorSaveRequest.NameDto n = new CreatorSaveRequest.NameDto();
        n.setFirstName(first);
        n.setLastName("Ijodkor");
        return n;
    }

    // ------------------------------------------------------------ manual

    @Nested
    @DisplayName("Qo'lda tartib (standart)")
    class ManualRanking {

        @Test
        @DisplayName("Faqat featured va faol ijodkorlar, sortOrder bo'yicha")
        void onlyFeaturedActiveInOrder() {
            settingsService.update(null, SettingKeys.CREATOR_RANKING, "MANUAL");

            Creator second = creator("Ikkinchi", true, 2, true, 0);
            Creator first = creator("Birinchi", true, 1, true, 0);
            Creator notFeatured = creator("Belgilanmagan", false, 0, true, 0);
            Creator inactive = creator("Nofaol", true, 0, false, 0);

            List<Creator> result = homepageService.featuredCreators(null);
            List<Long> ids = result.stream().map(Creator::getId).toList();

            assertThat(ids).containsSubsequence(first.getId(), second.getId());
            assertThat(ids)
                    .as("featured belgilanmagan ijodkor bo'limda chiqmasligi kerak")
                    .doesNotContain(notFeatured.getId());
            assertThat(ids)
                    .as("faolsizlantirilgan ijodkor bo'limda chiqmasligi kerak")
                    .doesNotContain(inactive.getId());
        }

        @Test
        @DisplayName("limit hurmat qilinadi")
        void limitIsApplied() {
            settingsService.update(null, SettingKeys.CREATOR_RANKING, "MANUAL");
            for (int i = 0; i < 5; i++) {
                creator("Ko'p", true, i, true, 0);
            }

            assertThat(homepageService.featuredCreators(3)).hasSize(3);
        }
    }

    // ------------------------------------------------------------- stars

    @Nested
    @DisplayName("Analitika tartibi")
    class StarsRanking {

        @Test
        @DisplayName("STARS tanlansa — Stars bo'yicha tartiblanadi")
        void ordersByStars() {
            settingsService.update(null, SettingKeys.CREATOR_RANKING, "STARS");

            Creator low = creator("Kam", false, 0, true, 10);
            Creator high = creator("Ko'p", false, 0, true, 900);
            Creator mid = creator("O'rta", false, 0, true, 100);

            List<Long> ids = homepageService.featuredCreators(null)
                    .stream().map(Creator::getId).toList();

            // ⚠️ featured bayrog'i bu tartibda TEKSHIRILMAYDI: avtomatik
            // reyting "kim mashhur" degan savolga o'zi javob beradi.
            assertThat(ids).containsSubsequence(high.getId(), mid.getId(), low.getId());
        }

        @Test
        @DisplayName("Sozlama o'zgarsa tartib ham DARHOL o'zgaradi")
        void strategyIsSwitchableWithoutCodeChange() {
            Creator manualFirst = creator("Qo'lda birinchi", true, 0, true, 1);
            Creator starsFirst = creator("Stars bo'yicha birinchi", false, 99, true, 5000);

            settingsService.update(null, SettingKeys.CREATOR_RANKING, "MANUAL");
            assertThat(homepageService.featuredCreators(1))
                    .extracting(Creator::getId)
                    .containsExactly(manualFirst.getId());

            // Kod o'zgarmadi, deploy bo'lmadi - faqat sozlama.
            settingsService.update(null, SettingKeys.CREATOR_RANKING, "STARS");
            assertThat(homepageService.featuredCreators(1))
                    .extracting(Creator::getId)
                    .containsExactly(starsFirst.getId());
        }

        @Test
        @DisplayName("Buzuq sozlama bo'lsa MANUAL ga qaytadi, xato tashlamaydi")
        void brokenSettingFallsBackToManual() {
            Creator featured = creator("Qo'lda", true, 0, true, 0);

            // ⚠️ Qiymat REPO orqali yoziladi, servis orqali emas.
            //
            // Servis endi bunday qiymatni rad etadi (yozish tekshiruvi) —
            // bu to'g'ri. Lekin buzuq qiymat baribir bazaga tushishi
            // mumkin: to'g'ridan-to'g'ri SQL bilan, ma'lumot ko'chirishda
            // yoki tekshiruv qo'shilishidan oldin yozilgan bo'lsa.
            //
            // Bu test aynan O'QISH yo'lining chidamliligini tekshiradi:
            // bosh sahifa buzuq sozlama tufayli bo'sh qolmasligi kerak.
            settingRepo.save(com.example.backend.Cms.Entity.PlatformSetting.builder()
                    .key(SettingKeys.CREATOR_RANKING)
                    .value("ALLAQANDAY")
                    .description("sinov")
                    .build());

            assertThat(homepageService.featuredCreators(null))
                    .extracting(Creator::getId)
                    .contains(featured.getId());
        }

        @Test
        @DisplayName("Servis buzuq qiymatni YOZISHGA ruxsat bermaydi")
        void serviceRejectsBrokenValue() {
            // Ikkinchi qavat: xato yozilgan paytda ko'rinadi, keyinroq
            // jimgina «MANUAL ga qaytdi» bo'lib qolmaydi.
            assertThatThrownBy(() -> settingsService.update(
                    null, SettingKeys.CREATOR_RANKING, "ALLAQANDAY"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("MANUAL");
        }
    }

    // ---------------------------------------------------------- uch til

    @Nested
    @DisplayName("Bo'lim sarlavhasi uch tilda")
    class SectionTitles {

        @Test
        @DisplayName("Avtomatik yaratilgan bo'limlarda UZ, RU va EN bor")
        void sectionsAreTrilingual() {
            List<HomepageSection> sections = homepageService.sections();

            assertThat(sections).isNotEmpty();
            for (HomepageSection s : sections) {
                assertThat(s.getTranslations())
                        .as("«%s» bo'limi uchala tilda bo'lishi kerak — u mobil "
                                + "ilova bosh sahifasida chiqadi", s.getType())
                        .extracting(HomepageSectionTranslation::getLocale)
                        .contains(Locale.UZ, Locale.RU, Locale.EN);
            }
        }

        @Test
        @DisplayName("«Mashhur ijodkorlar» eng pastda (buyurtmachi talabi R3)")
        void popularCreatorsGoLast() {
            HomepageSection creators = homepageService.sections().stream()
                    .filter(s -> s.getType() == HomepageSectionType.POPULAR_CREATORS)
                    .findFirst().orElseThrow();

            assertThat(creators.getSortOrder()).isEqualTo(999);
        }
    }
}
