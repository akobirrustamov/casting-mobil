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
}
