package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentListDto;
import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Category;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Repository.CategoryRepo;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.ContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.Cms.Entity.Genre;
import com.example.backend.Cms.Entity.Creator;
import com.example.backend.Cms.Enums.CreatorProfession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tahrirlash kontentni yo'qotmasligini qo'riqlaydi (B17).
 *
 * <h2>Nima buzilgan edi</h2>
 * Saqlashda backend media ro'yxatini BUTUNLAY almashtiradi
 * ({@code content.getMedia().clear()}), kategoriyani esa so'rovdagi qiymatga
 * qo'yadi. Bu o'z-o'zicha to'g'ri — muharrir to'liq holatni yuboradi.
 *
 * Lekin muharrirga qaytariladigan DTO'da {@code categoryId}, muqova va
 * galereya UMUMAN YO'Q edi. Ya'ni muharrir ularni yuklay olmasdi va
 * saqlashda bo'sh yuborardi. Natijada foydalanuvchi faqat sarlavhani
 * tuzatsa ham, kategoriya, muqova va butun galereya JIMGINA o'chib ketardi.
 *
 * <h2>Nima tekshiriladi</h2>
 * DTO kontentni QAYTA QURISH uchun yetarli ma'lumot beradimi. Agar kimdir
 * kelajakda bu maydonlarni olib tashlasa yoki yangi media roli qo'shib
 * DTO'ni yangilashni unutsa — test yiqiladi.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContentEditRoundTripTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private ContentService contentService;
    @Autowired private CategoryRepo categoryRepo;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private com.example.backend.Cms.Repository.GenreRepo genreRepo;
    @Autowired private com.example.backend.Cms.Repository.ContentRepo contentRepo;
    @Autowired private com.example.backend.Cms.Service.TaxonomyService taxonomyService;

    private MediaAsset image(String name) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/" + name + "-" + SEQ.incrementAndGet() + ".jpg")
                .originalFilename(name + ".jpg")
                .type(MediaType.IMAGE)
                .mimeType("image/jpeg")
                .sizeBytes(10L)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private ContentSaveRequest.MediaLink link(MediaRole role, Long mediaId, int order) {
        ContentSaveRequest.MediaLink l = new ContentSaveRequest.MediaLink();
        l.setRole(role);
        l.setMediaId(mediaId);
        l.setSortOrder(order);
        return l;
    }

    @Test
    @DisplayName("DTO kontentni qayta qurish uchun yetarli ma'lumot beradi")
    void dtoCarriesEverythingTheEditorNeeds() {
        Category category = categoryRepo.save(Category.builder()
                .slug("roundtrip-" + SEQ.incrementAndGet())
                .active(true)
                .sortOrder(0)
                .build());

        MediaAsset poster = image("afisha");
        MediaAsset posterRu = image("afisha-ru");
        MediaAsset cover = image("muqova");
        MediaAsset g1 = image("galereya-1");
        MediaAsset g2 = image("galereya-2");

        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.PUBLISHED);
        req.setCategoryId(category.getId());
        req.setTranslations(Translations.all("To'liq kontent"));

        List<ContentSaveRequest.MediaLink> media = new ArrayList<>();
        media.add(link(MediaRole.POSTER, poster.getId(), 0));
        ContentSaveRequest.MediaLink ru = link(MediaRole.POSTER, posterRu.getId(), 0);
        ru.setLocale(Locale.RU);
        media.add(ru);
        media.add(link(MediaRole.COVER, cover.getId(), 0));
        // Tartib ATAYLAB teskari beriladi - DTO uni sortOrder bo'yicha tiklashi kerak.
        media.add(link(MediaRole.GALLERY, g2.getId(), 1));
        media.add(link(MediaRole.GALLERY, g1.getId(), 0));
        req.setMedia(media);

        Content saved = contentService.create(null, req);
        ContentListDto dto = ContentListDto.from(saved);

        assertThat(dto.getCategoryId())
                .as("Kategoriya id bo'lmasa muharrir uni tanlab qo'ya olmaydi "
                        + "va saqlashda kategoriya o'chib ketadi")
                .isEqualTo(category.getId());

        assertThat(dto.getPosterMediaId()).isEqualTo(poster.getId());
        assertThat(dto.getLocalePosters()).containsEntry(Locale.RU, posterRu.getId());

        assertThat(dto.getCoverMediaId())
                .as("Muqova qaytmasa, tahrirlashda o'chib ketadi")
                .isEqualTo(cover.getId());

        assertThat(dto.getGallery())
                .as("Galereya sortOrder bo'yicha tartiblangan holda qaytishi kerak")
                .containsExactly(g1.getId(), g2.getId());
    }

    @Test
    @DisplayName("HAMMA media roli qaytadi — video va treyler yo'qolmaydi")
    void everyMediaRoleSurvivesTheRoundTrip() {
        MediaAsset poster = image("afisha");
        MediaAsset cover = image("muqova");
        MediaAsset thumb = image("eskiz");
        MediaAsset video = image("asosiy-video");
        MediaAsset trailer = image("treyler");
        MediaAsset teaser = image("tizer");
        MediaAsset gallery1 = image("galereya");

        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.PUBLISHED);
        req.setTranslations(Translations.all("Barcha rollar"));
        req.setMedia(List.of(
                link(MediaRole.POSTER, poster.getId(), 0),
                link(MediaRole.COVER, cover.getId(), 0),
                link(MediaRole.THUMBNAIL, thumb.getId(), 0),
                link(MediaRole.VIDEO, video.getId(), 0),
                link(MediaRole.TRAILER, trailer.getId(), 0),
                link(MediaRole.TEASER, teaser.getId(), 0),
                link(MediaRole.GALLERY, gallery1.getId(), 0)));

        ContentListDto dto = ContentListDto.from(contentService.create(null, req));

        // ⚠️ Qulaylik maydonlari faqat 3 ta rolni qamraydi. Saqlashda esa
        // media ro'yxati BUTUNLAY almashtiriladi — demak DTO qaytarmagan
        // har qanday rol tahrirlashda jimgina o'chib ketardi.
        assertThat(dto.getMedia())
                .as("DTO barcha media bog'lanishlarini qaytarishi kerak")
                .hasSize(7);

        assertThat(dto.getMedia()).extracting(ContentListDto.MediaLinkDto::getRole)
                .containsExactlyInAnyOrder(MediaRole.POSTER, MediaRole.COVER,
                        MediaRole.THUMBNAIL, MediaRole.VIDEO, MediaRole.TRAILER,
                        MediaRole.TEASER, MediaRole.GALLERY);

        // Asosiy video aynan o'sha fayl bo'lishi kerak.
        assertThat(dto.getMedia()).filteredOn(m -> m.getRole() == MediaRole.VIDEO)
                .singleElement()
                .extracting(ContentListDto.MediaLinkDto::getMediaId)
                .isEqualTo(video.getId());
    }

    @Test
    @DisplayName("Har bir MediaRole qiymati DTO'da qamrab olinadi")
    void noRoleIsSilentlyDropped() {
        // Kelajakda yangi rol qo'shilsa, u ham avtomatik qaytadi -
        // DTO'ni yangilash esdan chiqmaydi.
        MediaAsset asset = image("universal");

        List<ContentSaveRequest.MediaLink> links = new ArrayList<>();
        for (MediaRole role : MediaRole.values()) {
            links.add(link(role, asset.getId(), 0));
        }

        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.DRAFT);
        req.setTranslations(Translations.all("Har bir rol"));
        req.setMedia(links);

        ContentListDto dto = ContentListDto.from(contentService.create(null, req));

        assertThat(dto.getMedia())
                .as("MediaRole da %d ta qiymat bor, DTO %d ta qaytardi",
                        MediaRole.values().length, dto.getMedia().size())
                .hasSize(MediaRole.values().length);
    }

    @Test
    @DisplayName("Mediasiz kontentda ham DTO buzilmaydi")
    void emptyMediaIsSafe() {
        ContentSaveRequest req = new ContentSaveRequest();
        req.setContentType(ContentType.MOVIE);
        req.setStructureType(StructureType.SINGLE);
        req.setAccessPolicy(AccessPolicy.FREE);
        req.setStatus(PublicationStatus.DRAFT);
        req.setTranslations(Translations.all("Mediasiz"));

        ContentListDto dto = ContentListDto.from(contentService.create(null, req));

        assertThat(dto.getCategoryId()).isNull();
        assertThat(dto.getCoverMediaId()).isNull();
        // null emas, BO'SH ro'yxat - klientda .map() xatosi chiqmasin.
        assertThat(dto.getGallery()).isNotNull().isEmpty();
        assertThat(dto.getMedia()).isNotNull().isEmpty();
    }

    // ------------------------------------------------- janr va ijodkorlar

    /**
     * ⚠️ Bu ikkisi yuqoridagi media testida QAMRALMAGAN edi va aynan shu
     * sababli nuqson uzoq yashadi: {@code ContentListDto} da
     * {@code genreIds} ham, {@code credits} ham yo'q edi.
     *
     * {@code apply()} esa ikkala ro'yxatni ham SHARTSIZ almashtiradi.
     * Ya'ni panel ularni yuklolmagani uchun bo'sh ro'yxat qaytib kelardi
     * va sarlavhadagi bitta harfni tuzatgan admin kontentning barcha
     * janrlarini hamda §54 da biriktirilgan ijodkorlarini jimgina
     * yo'qotardi. Hech qanday xato chiqmasdi.
     */
    @Test
    @DisplayName("Janrlar DTO'da qaytadi va tahrirdan omon qoladi")
    void genresSurviveEdit() {
        Genre genre = genre();

        ContentSaveRequest req = base("Janrli kontent");
        req.setGenreIds(java.util.Set.of(genre.getId()));
        Content created = contentService.create(null, req);

        ContentListDto loaded = ContentListDto.from(
                contentRepo.findById(created.getId()).orElseThrow());
        assertThat(loaded.getGenreIds())
                .as("DTO janrlarni qaytarmasa, muharrir ularni bo'sh saqlab yuborardi")
                .containsExactly(genre.getId());

        // Panel aynan shunday ishlaydi: o'qiydi, bitta maydonni
        // o'zgartiradi, hammasini qaytarib yuboradi.
        ContentSaveRequest edit = base("Sarlavha o'zgardi");
        edit.setVersion(loaded.getVersion());
        edit.setGenreIds(new java.util.LinkedHashSet<>(loaded.getGenreIds()));
        contentService.update(null, created.getId(), edit);

        assertThat(contentRepo.findById(created.getId()).orElseThrow().getGenres())
                .as("faqat sarlavha o'zgardi - janrlar tegilmasligi kerak")
                .hasSize(1);
    }

    @Test
    @DisplayName("Ijodkorlar DTO'da qaytadi va tahrirdan omon qoladi")
    void creditsSurviveEdit() {
        Creator creator = creator();

        ContentSaveRequest req = base("Ijodkorli kontent");
        req.setCredits(List.of(credit(creator.getId())));
        Content created = contentService.create(null, req);

        ContentListDto loaded = ContentListDto.from(
                contentRepo.findById(created.getId()).orElseThrow());
        assertThat(loaded.getCredits()).hasSize(1);
        assertThat(loaded.getCredits().get(0).getCreatorId()).isEqualTo(creator.getId());
        assertThat(loaded.getCredits().get(0).getProfession())
                .isEqualTo(CreatorProfession.ACTOR);

        ContentSaveRequest edit = base("Sarlavha o'zgardi");
        edit.setVersion(loaded.getVersion());
        edit.setCredits(loaded.getCredits().stream().map(cr -> {
            ContentSaveRequest.CreditLink l = new ContentSaveRequest.CreditLink();
            l.setCreatorId(cr.getCreatorId());
            l.setProfession(cr.getProfession());
            l.setCharacterName(cr.getCharacterName());
            l.setSortOrder(cr.getSortOrder());
            return l;
        }).toList());
        contentService.update(null, created.getId(), edit);

        assertThat(contentRepo.findById(created.getId()).orElseThrow().getCredits())
                .hasSize(1);
    }

    /**
     * Backend to'g'ri qaytarsa ham, panel formaga ko'chirmasa ma'lumot
     * baribir yo'qoladi. JSX ni reflection bilan ko'rib bo'lmaydi —
     * shuning uchun manba matni tekshiriladi.
     */
    @Test
    @DisplayName("Panel muharriri ikkala ro'yxatni ham yuklaydi")
    void editorLoadsBothLists() throws java.io.IOException {
        String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                "../frontend/src/adminpanel/pages/ContentEditor.jsx"));

        assertThat(src).contains("genreIds: Array.isArray(c.genreIds)");
        assertThat(src).contains("credits: Array.isArray(c.credits)");
    }

    private ContentSaveRequest base(String title) {
        ContentSaveRequest r = new ContentSaveRequest();
        r.setContentType(ContentType.MOVIE);
        r.setStructureType(StructureType.SINGLE);
        r.setAccessPolicy(AccessPolicy.FREE);
        r.setStatus(PublicationStatus.DRAFT);
        r.setTranslations(Translations.all(title + " " + SEQ.incrementAndGet()));
        return r;
    }

    private ContentSaveRequest.CreditLink credit(Long creatorId) {
        ContentSaveRequest.CreditLink l = new ContentSaveRequest.CreditLink();
        l.setCreatorId(creatorId);
        l.setProfession(CreatorProfession.ACTOR);
        l.setSortOrder(0);
        return l;
    }

    private Genre genre() {
        int n = SEQ.incrementAndGet();
        Genre g = new Genre();
        g.setSlug("janr-" + n);
        g.setActive(true);
        g.setSortOrder(n);
        return genreRepo.save(g);
    }

    private Creator creator() {
        var r = new com.example.backend.Admin.Dto.CreatorSaveRequest();
        r.setActive(true);
        int n = SEQ.incrementAndGet();
        var tr = new java.util.LinkedHashMap<Locale,
                com.example.backend.Admin.Dto.CreatorSaveRequest.NameDto>();
        for (Locale loc : Locale.values()) {
            var name = new com.example.backend.Admin.Dto.CreatorSaveRequest.NameDto();
            name.setDisplayName("Ijodkor " + loc + " " + n);
            tr.put(loc, name);
        }
        r.setTranslations(tr);
        return taxonomyService.saveCreator(null, null, r);
    }

    // ------------------------------------------------- ijodkor kartochkasi

    /**
     * Panel ijodkorlar ro'yxatini alohida yuklaydi va u <b>200 ta</b>
     * bilan cheklangan. Ya'ni bog'lanishning o'zida ism va surat
     * bo'lmasa, 200 dan keyingi ijodkor biriktirilgan kontentda
     * kartochka o'rniga quruq {@code #42} chiqardi va admin kimni
     * biriktirganini bilmasdi.
     */
    @Test
    @DisplayName("Bog'lanish ijodkor ismi va suratini o'zi bilan olib keladi")
    void creditCarriesNameAndPhoto() {
        MediaAsset photo = image("ijodkor-surati");
        Creator creator = creatorWithPhoto(photo);

        ContentSaveRequest req = base("Kartochkali kontent");
        req.setCredits(List.of(credit(creator.getId())));
        Content created = contentService.create(null, req);

        var credits = ContentListDto.from(
                contentRepo.findById(created.getId()).orElseThrow()).getCredits();

        assertThat(credits).hasSize(1);
        assertThat(credits.get(0).getCreatorName())
                .as("ro'yxatdan topilmasa ham ism ko'rinsin")
                .isNotBlank();
        assertThat(credits.get(0).getPhotoMediaId())
                .as("kartochkada surat ko'rsatiladi")
                .isEqualTo(photo.getId());
    }

    /**
     * ⚠️ Tartib raqami ilgari biriktirish paytida {@code credits.length}
     * dan olinardi. O'rtadagi bittasi o'chirilib yangisi qo'shilsa raqam
     * TAKRORLANARDI va kim oldin turishi bazaga bog'liq bo'lib qolardi.
     */
    @Test
    @DisplayName("Tartib raqami takrorlanmaydi")
    void sortOrderHasNoDuplicates() {
        Creator a = creator();
        Creator b = creator();
        Creator c = creator();

        ContentSaveRequest req = base("Tartibli kontent");
        req.setCredits(List.of(credit(a.getId()), credit(b.getId()), credit(c.getId())));
        // Panel saqlashda tartibni ro'yxatdagi joydan qayta hisoblaydi.
        List.of(0, 1, 2).forEach(i -> req.getCredits().get(i).setSortOrder(i));
        Content created = contentService.create(null, req);

        var credits = ContentListDto.from(
                contentRepo.findById(created.getId()).orElseThrow()).getCredits();

        assertThat(credits.stream().map(x -> x.getSortOrder()).toList())
                .as("har bir bog'lanishda o'z raqami bo'lsin")
                .doesNotHaveDuplicates()
                .containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("Panel kartochkani ism va surat bilan chizadi")
    void editorRendersCard() throws java.io.IOException {
        String src = java.nio.file.Files.readString(java.nio.file.Path.of(
                "../frontend/src/adminpanel/pages/editor/CreditsTab.jsx"));

        // Aniq ifoda tekshiriladi: shunchaki `cr.creatorName` matni
        // faylning boshqa joyida ham uchrashi mumkin va u holda test
        // hech narsani isbotlamasdi.
        assertThat(src)
                .as("ro'yxatda yo'q ijodkor uchun bog'lanishdagi ism ishlatilsin")
                .contains("(cr.creatorName || `#${cr.creatorId}`)");
        assertThat(src)
                .as("surat ko'rsatilsin")
                .contains("mediaUrl(info.photoId)");

        String editor = java.nio.file.Files.readString(java.nio.file.Path.of(
                "../frontend/src/adminpanel/pages/ContentEditor.jsx"));
        // ⚠️ Shunchaki `sortOrder: i` yetarli emas: u galereya uchun ham
        // ishlatiladi va mutatsiya buni ko'rsatdi — kreditlar qatorini
        // butunlay olib tashlasam ham test o'tib ketdi.
        assertThat(editor)
                .as("tartib saqlashda qayta hisoblansin")
                .contains("credits: form.credits.map((c, i) => ({ ...c, sortOrder: i }))");
    }

    private Creator creatorWithPhoto(MediaAsset photo) {
        var r = new com.example.backend.Admin.Dto.CreatorSaveRequest();
        r.setActive(true);
        r.setPhotoMediaId(photo.getId());
        int n = SEQ.incrementAndGet();
        var tr = new java.util.LinkedHashMap<Locale,
                com.example.backend.Admin.Dto.CreatorSaveRequest.NameDto>();
        for (Locale loc : Locale.values()) {
            var name = new com.example.backend.Admin.Dto.CreatorSaveRequest.NameDto();
            name.setDisplayName("Suratli ijodkor " + loc + " " + n);
            tr.put(loc, name);
        }
        r.setTranslations(tr);
        return taxonomyService.saveCreator(null, null, r);
    }
}
