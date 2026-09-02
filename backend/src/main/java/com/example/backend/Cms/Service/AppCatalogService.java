package com.example.backend.Cms.Service;

import com.example.backend.Cms.Dto.CatalogCategoryDto;
import com.example.backend.Cms.Dto.HomeFeedDto;
import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.CategoryTranslation;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Enums.ContentVisibility;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.PublicationStatus;
import com.example.backend.Cms.Repository.CategoryRepo;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Katalog kategoriyalari mobil ilova uchun (ТЗ §16, §31).
 *
 * <h2>Nima uchun kerak</h2>
 * Bosh sahifada kategoriya faqat PLITKA edi: nomi bor, ichi yo'q va bosib
 * ham bo'lmasdi. Buyurtmachi esa har kategoriya o'z qatori bo'lishini
 * so'radi — «Drama», ostida kartochkalar, xuddi «Podkastlar» va
 * «Mini seriallar» kabi.
 *
 * <h2>Nima uchun bosh sahifa feediga qo'shilmadi</h2>
 * Feed BITTA javob va u admin yiqqan bo'limlar tartibida keladi. Kategoriya
 * soni esa cheklanmagan: o'ntasini feedga solish har bir bosh sahifa
 * so'roviga o'nta qo'shimcha qator qo'shardi — hatto ularni ko'rmaydigan
 * odamga ham. Shuning uchun kategoriyalar ALOHIDA olinadi: avval ro'yxat
 * (nomlar va sonlar), keyin klient ko'rinadiganlarini bittalab so'raydi.
 *
 * <h2>Ko'rinish qoidasi bitta joyda</h2>
 * Qaysi kontent katalogda chiqadi — {@link HomeFeedService#isVisible}.
 * Bu yerda takrorlanmaydi: qoida ikkiga bo'linsa, bosh sahifada ko'ringan
 * kontent kategoriyada yo'qolishi mumkin edi.
 */
@Service
@RequiredArgsConstructor
public class AppCatalogService {

    /** Sahifa hajmi, klient so'ramasa. */
    private static final int DEFAULT_SIZE = 20;

    /**
     * Bitta so'rovda maksimum. Chegara bor, chunki {@code size} klientdan
     * keladi: usiz bitta so'rov butun katalogni tortib chiqarardi.
     */
    private static final int MAX_SIZE = 100;

    private final CategoryRepo categoryRepo;
    private final ContentRepo contentRepo;
    private final HomeFeedService homeFeedService;

    /**
     * Faol kategoriyalar — admin bergan tartibda, kartochkalarsiz.
     *
     * Soni nol bo'lgan kategoriya ham qaytadi: klient uni ko'rsatmasligi
     * mumkin, lekin bu QAROR, yashirilgan ma'lumot emas. Panelda
     * kategoriya bor, ilovada esa u umuman yo'qday ko'rinishi
     * muharrirni chalg'itardi.
     */
    @Transactional(readOnly = true)
    public List<CatalogCategoryDto> categories(User user, Locale locale) {
        Locale lang = homeFeedService.resolveLanguage(user, locale);
        Map<Long, Integer> counts = visibleCounts();

        List<CatalogCategoryDto> result = new ArrayList<>();
        for (Category c : categoryRepo.findAllByActiveTrueOrderBySortOrderAsc()) {
            result.add(head(c, lang, counts.getOrDefault(c.getId(), 0)).build());
        }
        return result;
    }

    /**
     * Bitta kategoriyaning bitta SAHIFASI.
     *
     * <h2>Nima uchun «bittalab»</h2>
     * Klient ro'yxatni olib, har biriga alohida so'rov yuboradi. Shunda
     * ekranning yuqorisidagi qator birinchi bo'lib to'ladi va pastdagilarni
     * kutib turmaydi; bitta kategoriya xato bersa ham qolganlari chiqadi.
     *
     * <h2>Nima uchun sahifalash</h2>
     * Bosh sahifadagi qator birinchi 10 tani so'raydi (ekranda 3 tasi
     * ko'rinadi, qolgani surilganda chiqadi), «Barchasi» ekrani esa 20
     * tadan qo'shib boradi. Bittasiga butun kategoriyani berish katta
     * kategoriyada bir necha yuz kartochkani hech kim ko'rmasdan
     * yuklashni anglatardi.
     *
     * ⚠️ Nofaol kategoriya 404 qaytaradi. «Bor, lekin o'chirilgan» deyish
     * uni klientda bo'sh qator qilib chizishga olib kelardi.
     */
    @Transactional(readOnly = true)
    public CatalogCategoryDto category(User user, Locale locale, Long categoryId,
                                       Integer page, Integer size) {
        Locale lang = homeFeedService.resolveLanguage(user, locale);

        Category category = categoryRepo.findById(categoryId)
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .orElseThrow(() -> BusinessException.notFound("Category", categoryId));

        List<Content> visible = contentRepo
                .findAllByDeletedAtIsNullAndCategoryIdAndStatus(
                        categoryId, PublicationStatus.PUBLISHED)
                .stream()
                .filter(c -> homeFeedService.isVisible(c, user))
                // Bosh sahifa qatorlari bilan bir xil tartib: yangi kontent
                // tepada. Aks holda bitta film ikki qatorda ikki xil joyda
                // turardi.
                //
                // ⚠️ Tartib SAHIFALASH uchun ham muhim: u barqaror
                // bo'lmasa, ikkinchi sahifada birinchisidagi kontent
                // qaytib chiqishi mumkin edi.
                .sorted(Comparator.comparing(Content::getPublicationDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        // Nashr sanasi bir xil bo'lganda tartibni id hal
                        // qiladi — aks holda u so'rovdan so'rovga o'zgarardi.
                        .thenComparing(Content::getId, Comparator.reverseOrder()))
                .toList();

        int pageSize = clamp(size);
        int pageIndex = page == null || page < 0 ? 0 : page;
        int from = Math.min(pageIndex * pageSize, visible.size());
        int to = Math.min(from + pageSize, visible.size());

        List<HomeFeedDto.ContentCard> items = visible.subList(from, to).stream()
                .map(c -> homeFeedService.contentCard(c, lang))
                .toList();

        // «12 qism» yozuvi bosh sahifadagi bilan bitta qoidadan chiqsin.
        homeFeedService.fillEpisodeCounts(items);

        // total — sahifadan OLDINGI son: «Barchasi ›» ni ko'rsatish
        // kerakmi degan savolga faqat u javob beradi.
        return head(category, lang, visible.size())
                .page(pageIndex)
                .size(pageSize)
                .hasMore(to < visible.size())
                .items(items)
                .build();
    }

    // ------------------------------------------------------------ yordamchi

    private CatalogCategoryDto.CatalogCategoryDtoBuilder head(Category c, Locale lang, int total) {
        return CatalogCategoryDto.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .name(name(c, lang))
                .iconMediaId(c.getIcon() == null ? null : c.getIcon().getId())
                .total(total);
    }

    /** Kategoriya id → ko'rinadigan kontent soni. Bitta guruhlangan so'rov. */
    private Map<Long, Integer> visibleCounts() {
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : contentRepo.countVisibleByCategory(
                PublicationStatus.PUBLISHED, ContentVisibility.PUBLIC)) {
            counts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    /**
     * So'ralgan til, bo'lmasa o'zbekchasi.
     *
     * Bo'sh sarlavha bilan qator chizishdan ko'ra boshqa tildagi nom
     * yaxshiroq: kategoriya baribir bor.
     */
    private String name(Category c, Locale lang) {
        CategoryTranslation base = null;
        for (CategoryTranslation t : c.getTranslations()) {
            if (t.getLocale() == lang) {
                return t.getName();
            }
            if (t.getLocale() == Locale.UZ) {
                base = t;
            }
        }
        return base == null ? null : base.getName();
    }

    private int clamp(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
