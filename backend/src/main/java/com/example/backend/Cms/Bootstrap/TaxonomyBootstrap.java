package com.example.backend.Cms.Bootstrap;

import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.CategoryTranslation;
import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Entity.GenreTranslation;
import com.example.backend.Cms.Entity.PlatformSetting;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.CategoryRepo;
import com.example.backend.Cms.Repository.GenreRepo;
import com.example.backend.Cms.Repository.PlatformSettingRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kategoriya va janr katalogini bazaga ko'chiradi — ishga tushishda,
 * HAR QANDAY muhitda ({@code AutoRun} rollarni yaratgani kabi).
 *
 * <h2>Nega dev seeder emas</h2>
 * {@code DevDataSeeder} faqat {@code app.dev.seed=true} bo'lganda
 * ishlaydi va u soxta kontent yasaydi. Janrlar esa soxta emas: usiz
 * kontent qo'shayotgan admin bo'sh janr ro'yxatiga duch keladi va har
 * bir janrni qo'lda, uch tilda yozib chiqishi kerak bo'lardi.
 *
 * <h2>⚠️ Nega versiya belgisi bor</h2>
 * Runner har ishga tushishda ishlaydi. Agar u shunchaki "yetishmagan
 * slug'ni qo'sh" desa, admin O'CHIRGAN janr keyingi qayta ishga
 * tushirishda QAYTA PAYDO bo'lardi — o'chirish tugmasi ishlamayotgandek
 * ko'rinardi va sababini topish qiyin: nosozlik faqat serverni qayta
 * ishga tushirgandan keyin ko'rinadi.
 *
 * Shuning uchun katalog versiyasi sozlamalar jadvalida saqlanadi:
 * bir marta ko'chirilgan versiya qaytadan ko'chirilmaydi. Ro'yxatga
 * yangi janr qo'shilsa {@link TaxonomyCatalog#VERSION} oshiriladi va
 * o'shanda faqat YETISHMAYOTGAN slug'lar qo'shiladi.
 *
 * <h2>Mavjud satrlarga tegilmaydi</h2>
 * Slug bazada bor bo'lsa — o'tkazib yuboriladi. Admin nomni yoki
 * tartibni o'zgartirgan bo'lsa, o'zgarish saqlanib qoladi.
 */
@Slf4j
@Component
@Order(10) // AutoRun (0) dan keyin, seeder'lardan (20, 100) oldin
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.taxonomy.bootstrap", havingValue = "true", matchIfMissing = true)
public class TaxonomyBootstrap implements CommandLineRunner {

    /**
     * ⚠️ {@code internal.} prefiksi — bu satr admin sozlamalar sahifasida
     * KO'RINMASLIGI uchun ({@code SettingsService.all()} shu prefiksni
     * chiqarib tashlaydi). Bu texnik belgi, sozlanadigan qiymat emas:
     * admin uni o'zgartirsa katalog qaytadan ko'chirilib, o'chirilgan
     * janrlar tiklanardi.
     */
    public static final String VERSION_KEY = "internal.taxonomy.catalog.version";

    private final CategoryRepo categoryRepo;
    private final GenreRepo genreRepo;
    private final PlatformSettingRepo settingRepo;

    @Override
    @Transactional
    public void run(String... args) {
        int applied = appliedVersion();
        if (applied >= TaxonomyCatalog.VERSION) {
            return;
        }

        int categories = seedCategories();
        int genres = seedGenres();

        settingRepo.save(PlatformSetting.builder()
                .key(VERSION_KEY)
                .value(String.valueOf(TaxonomyCatalog.VERSION))
                .description("Kategoriya/janr katalogining ko'chirilgan versiyasi. Qo'lda o'zgartirilmaydi.")
                .build());

        log.info("Taksonomiya katalogi v{} qo'llandi: {} kategoriya, {} janr qo'shildi",
                TaxonomyCatalog.VERSION, categories, genres);
    }

    /**
     * Qaysi versiya allaqachon ko'chirilgan.
     *
     * Buzilgan qiymat (masalan qo'lda yozilgan matn) butun ishga
     * tushishni to'xtatmasin — 0 deb qaraladi, ya'ni katalog qaytadan
     * qo'llanadi. Mavjud satrlarga tegilmagani uchun bu xavfsiz.
     */
    private int appliedVersion() {
        return settingRepo.findById(VERSION_KEY)
                .map(PlatformSetting::getValue)
                .map(value -> {
                    try {
                        return Integer.parseInt(value.trim());
                    } catch (NumberFormatException e) {
                        log.warn("'{}' sozlamasi son emas: '{}' — katalog qaytadan tekshiriladi",
                                VERSION_KEY, value);
                        return 0;
                    }
                })
                .orElse(0);
    }

    private int seedCategories() {
        int added = 0;
        int order = nextOrder(categoryRepo.count());
        for (String[] row : TaxonomyCatalog.CATEGORIES) {
            if (categoryRepo.existsBySlug(row[0])) {
                continue;
            }
            Category category = Category.builder()
                    .slug(row[0])
                    .sortOrder(order++)
                    .active(true)
                    .build();
            category.addTranslation(CategoryTranslation.builder().locale(Locale.UZ).name(row[1]).build());
            category.addTranslation(CategoryTranslation.builder().locale(Locale.RU).name(row[2]).build());
            category.addTranslation(CategoryTranslation.builder().locale(Locale.EN).name(row[3]).build());
            categoryRepo.save(category);
            added++;
        }
        return added;
    }

    private int seedGenres() {
        int added = 0;
        int order = nextOrder(genreRepo.count());
        for (String[] row : TaxonomyCatalog.GENRES) {
            if (genreRepo.existsBySlug(row[0])) {
                continue;
            }
            Genre genre = Genre.builder()
                    .slug(row[0])
                    .sortOrder(order++)
                    .active(true)
                    .build();
            genre.addTranslation(GenreTranslation.builder().locale(Locale.UZ).name(row[1]).build());
            genre.addTranslation(GenreTranslation.builder().locale(Locale.RU).name(row[2]).build());
            genre.addTranslation(GenreTranslation.builder().locale(Locale.EN).name(row[3]).build());
            genreRepo.save(genre);
            added++;
        }
        return added;
    }

    /**
     * Yangi satrlar mavjudlaridan KEYIN tursin.
     *
     * Bo'sh bazada 0 dan boshlanadi va katalog tartibi saqlanadi. Bazada
     * allaqachon satr bo'lsa (masalan admin qo'lda qo'shgan), yangilari
     * ularning ustiga chiqib ketmaydi.
     */
    private int nextOrder(long existing) {
        return (int) existing;
    }
}
