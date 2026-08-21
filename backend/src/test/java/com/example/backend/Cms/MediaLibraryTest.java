package com.example.backend.Cms;

import com.example.backend.Admin.TestStaffFactory;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import com.example.backend.Cms.Service.AccessService;
import com.example.backend.Enums.Permission;
import com.example.backend.Enums.PlatformRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ТЗ §26 — Markazlashtirilgan media kutubxonasi.
 *
 * <h2>Nima tekshiriladi</h2>
 * Kutubxona ЯКDAN foydalanish uchun kerak: fayl bir marta yuklanadi va
 * ko'p joyda ishlatiladi. Buning uchun uni TOPISH mumkin bo'lishi kerak —
 * qidiruv va filtr. Eskirgan fayllar esa ro'yxatni to'ldirmasligi kerak —
 * arxivlash.
 *
 * <h2>Arxivlash — o'chirish EMAS</h2>
 * Fayl 12 xil joydan havola qilinadi. O'chirish sinib qolgan rasm degani.
 * Arxivlangan fayl kutubxonada ko'rinmaydi, lekin MAVJUD havolalar
 * ishlashda davom etadi — bu alohida tekshiriladi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestStaffFactory.class)
@Transactional
class MediaLibraryTest {

    private static final AtomicInteger SEQ = new AtomicInteger(0);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MediaAssetRepo mediaAssetRepo;
    @Autowired private AccessService accessService;
    @Autowired private TestStaffFactory staff;

    private String token;

    @BeforeEach
    void setUp() {
        token = staff.tokenForRole("+998900004001", PlatformRole.ADMIN,
                EnumSet.noneOf(Permission.class));
    }

    private MediaAsset asset(String filename, MediaType type, MediaStatus status) {
        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey("/test/" + SEQ.incrementAndGet() + "-" + filename)
                .originalFilename(filename)
                .type(type)
                .mimeType(type == MediaType.VIDEO ? "video/mp4" : "image/jpeg")
                .sizeBytes(1024L)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private JsonNode listing(String query) throws Exception {
        String body = mockMvc.perform(get("/api/v1/app/admin/media" + query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("items");
    }

    private boolean contains(JsonNode items, Long id) {
        for (JsonNode item : items) {
            if (item.get("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ turlar

    @Nested
    @DisplayName("Turlar va filtr")
    class TypesAndFilter {

        @Test
        @DisplayName("Uchala tur ham qo'llab-quvvatlanadi")
        void allThreeTypesSupported() {
            assertThat(MediaType.values()).containsExactlyInAnyOrder(
                    MediaType.IMAGE, MediaType.VIDEO, MediaType.DOCUMENT);
        }

        @Test
        @DisplayName("Tur bo'yicha filtr")
        void filtersByType() throws Exception {
            MediaAsset image = asset("rasm.jpg", MediaType.IMAGE, MediaStatus.READY);
            MediaAsset video = asset("kino.mp4", MediaType.VIDEO, MediaStatus.READY);

            JsonNode videos = listing("?type=VIDEO&size=100");
            assertThat(contains(videos, video.getId())).isTrue();
            assertThat(contains(videos, image.getId())).isFalse();
        }
    }

    // ----------------------------------------------------------- qidiruv

    @Nested
    @DisplayName("Qidiruv")
    class Search {

        @Test
        @DisplayName("Asl fayl nomi bo'yicha topiladi")
        void findsByOriginalFilename() throws Exception {
            MediaAsset target = asset("qalbim-egasi-afisha.jpg",
                    MediaType.IMAGE, MediaStatus.READY);
            MediaAsset other = asset("boshqa-fayl.jpg", MediaType.IMAGE, MediaStatus.READY);

            JsonNode found = listing("?q=qalbim&size=100");

            // Admin faylni YUKLAGAN nomi bilan eslaydi; storageKey esa UUID,
            // undan qidirishning ma'nosi yo'q.
            assertThat(contains(found, target.getId())).isTrue();
            assertThat(contains(found, other.getId())).isFalse();
        }

        @Test
        @DisplayName("Katta-kichik harf farq qilmaydi")
        void searchIsCaseInsensitive() throws Exception {
            MediaAsset target = asset("Sevgi-Qissasi.JPG", MediaType.IMAGE, MediaStatus.READY);

            assertThat(contains(listing("?q=sevgi&size=100"), target.getId())).isTrue();
            assertThat(contains(listing("?q=QISSASI&size=100"), target.getId())).isTrue();
        }

        @Test
        @DisplayName("Qidiruv va tur filtri birga ishlaydi")
        void searchCombinesWithTypeFilter() throws Exception {
            MediaAsset video = asset("sinov-kino.mp4", MediaType.VIDEO, MediaStatus.READY);
            MediaAsset image = asset("sinov-rasm.jpg", MediaType.IMAGE, MediaStatus.READY);

            JsonNode found = listing("?q=sinov&type=VIDEO&size=100");
            assertThat(contains(found, video.getId())).isTrue();
            assertThat(contains(found, image.getId())).isFalse();
        }
    }

    // ---------------------------------------------------------- arxivlash

    @Nested
    @DisplayName("Arxivlash")
    class Archiving {

        @Test
        @DisplayName("Arxivlangan fayl kutubxonada KO'RINMAYDI")
        void archivedIsHiddenFromLibrary() throws Exception {
            MediaAsset old = asset("eskirgan.jpg", MediaType.IMAGE, MediaStatus.READY);

            mockMvc.perform(post("/api/v1/app/admin/media/" + old.getId() + "/archive")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARCHIVED"));

            // Kutubxonaning vazifasi - yangi kontentga fayl tanlash.
            // Eskirgan fayl ro'yxatda tursa, admin uni bilmasdan qayta
            // ishlatib yuborardi.
            assertThat(contains(listing("?size=100"), old.getId())).isFalse();
        }

        @Test
        @DisplayName("Arxivlanganlarni ataylab so'rash mumkin")
        void archivedCanBeListedExplicitly() throws Exception {
            MediaAsset old = asset("arxiv.jpg", MediaType.IMAGE, MediaStatus.ARCHIVED);

            assertThat(contains(listing("?status=ARCHIVED&size=100"), old.getId())).isTrue();
        }

        @Test
        @DisplayName("Arxivlangan faylning MAVJUD havolalari ishlaydi")
        void archivedFileIsStillServed() {
            MediaAsset image = asset("hali-ishlatilyapti.jpg",
                    MediaType.IMAGE, MediaStatus.ARCHIVED);

            // ⚠️ Eng muhim tekshiruv: arxivlash faqat kutubxonadan yashiradi.
            // Agar u faylni ham yopib qo'yganda, arxivlash o'chirish bilan
            // bir xil xavfli bo'lardi - sahifalarda rasm sinardi.
            assertThat(accessService.canReadMedia(null, image)).isTrue();
        }

        @Test
        @DisplayName("Tiklash — fayl kutubxonaga qaytadi")
        void restoreBringsItBack() throws Exception {
            MediaAsset old = asset("tiklanadi.jpg", MediaType.IMAGE, MediaStatus.ARCHIVED);

            mockMvc.perform(post("/api/v1/app/admin/media/" + old.getId() + "/restore")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("READY"));

            assertThat(contains(listing("?size=100"), old.getId())).isTrue();
        }

        @Test
        @DisplayName("Ruxsatsiz xodim arxivlay olmaydi")
        void archiveRequiresPermission() throws Exception {
            String viewer = staff.tokenForRole("+998900004002", PlatformRole.WORKER,
                    EnumSet.of(Permission.MEDIA_VIEW));
            MediaAsset image = asset("himoyalangan.jpg", MediaType.IMAGE, MediaStatus.READY);

            mockMvc.perform(post("/api/v1/app/admin/media/" + image.getId() + "/archive")
                            .header("Authorization", "Bearer " + viewer))
                    .andExpect(status().isForbidden());
        }
    }
}
