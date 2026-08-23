package com.example.backend.Cms;

import com.example.backend.Admin.Dto.ContentSaveRequest;
import com.example.backend.Admin.Dto.TranslationDto;
import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.*;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.support.Translations;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.ContentService;
import com.example.backend.Cms.Service.MediaUsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ishlatilayotgan faylni o'chirib bo'lmasligini qo'riqlaydi.
 *
 * Media 12 xil joydan havola qilinadi. Biror joyda ishlatilayotgan faylni
 * o'chirish sahifada sinib qolgan rasm yoki o'ynamaydigan video demakdir,
 * shuning uchun bu holat OLDINDAN aniqlanishi kerak — foreign key xatosi
 * bilan emas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MediaDeletionTest {

    @Autowired
    private MediaUsageService usageService;

    @Autowired
    private MediaAssetRepo mediaAssetRepo;

    @Autowired
    private ContentService contentService;

    private MediaAsset poster(String name) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/" + name + ".jpg")
                .originalFilename(name + ".jpg")
                .type(MediaType.IMAGE)
                .mimeType("image/jpeg")
                .sizeBytes(100L)
                .status(MediaStatus.READY)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("Hech qayerda ishlatilmagan fayl - bo'sh ro'yxat")
    void unusedMediaHasNoUsages() {
        MediaAsset free = poster("ishlatilmagan");
        assertThat(usageService.usages(free.getId())).isEmpty();
    }

    @Test
    @DisplayName("Kontent galereyasidagi fayl ishlatilayotgan deb topiladi")
    void mediaUsedByContentIsDetected() {
        MediaAsset used = poster("afisha-band");

        ContentSaveRequest c = new ContentSaveRequest();
        c.setContentType(ContentType.MOVIE);
        c.setStructureType(StructureType.SINGLE);
        c.setAccessPolicy(AccessPolicy.FREE);
        c.setStatus(PublicationStatus.PUBLISHED);
        c.setTranslations(Translations.all("Afishali film"));

        ContentSaveRequest.MediaLink link = new ContentSaveRequest.MediaLink();
        link.setRole(MediaRole.POSTER);
        link.setMediaId(used.getId());
        c.setMedia(List.of(link));

        Content content = contentService.create(null, c);
        assertThat(content.getId()).isNotNull();

        List<MediaUsageService.Usage> usages = usageService.usages(used.getId());
        assertThat(usages).isNotEmpty();
        assertThat(usages).anyMatch(u -> u.getWhere().equals("Kontent galereyasi"));
    }

    @Test
    @DisplayName("Mavjud bo'lmagan media uchun ham xato bo'lmaydi")
    void unknownMediaIsSafe() {
        // Barcha 12 ta so'rov ishlashi kerak - biror entity nomi noto'g'ri
        // yozilgan bo'lsa aynan shu yerda bilinadi.
        assertThat(usageService.usages(-1L)).isEmpty();
    }
}
