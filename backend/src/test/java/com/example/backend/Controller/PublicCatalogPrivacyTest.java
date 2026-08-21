package com.example.backend.Controller;

import com.example.backend.Entity.CastingUser;
import com.example.backend.Repository.CastingUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Ochiq katalogdan shaxsiy ma'lumot chiqmasligini qo'riqlaydi.
 *
 * Bu test ATAYLAB qattiq: agar kimdir kelajakda {@code /casting-user/web}
 * ni yana entity qaytaradigan qilib qo'ysa, test darhol yiqiladi.
 *
 * Sabab: bu endpoint tokensiz ochiq va bazada voyaga yetmagan anketalar bor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicCatalogPrivacyTest {

    /** Ochiq katalogda HECH QACHON ko'rinmasligi kerak bo'lgan maydonlar. */
    private static final String[] FORBIDDEN_FIELDS = {
            "phone", "email", "telegram", "facebook", "instagram", "telegramId",
            "bust", "waist", "son", "clothSize", "shoeSize", "price",
            "status", "firstChan", "secondChan"
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CastingUserRepo castingUserRepo;

    /** Aynan shu anketa bo'yicha qidiramiz - boshqa testlar yaratganlari bilan aralashmasin. */
    private static final String MARKER_NAME = "Maxfiylik sinovi anketasi";

    @BeforeEach
    void seedOneProfileWithEverythingFilled() {
        // ⚠️ `count() > 0` bilan tekshirish YARAMAYDI: boshqa testlar (masalan
        // SecurityRulesTest) ham anketa yaratadi va u holda bu yerdagi ma'lumot
        // umuman qo'shilmay qolardi.
        boolean alreadySeeded = castingUserRepo.findAll().stream()
                .anyMatch(u -> MARKER_NAME.equals(u.getName()));
        if (alreadySeeded) {
            return;
        }
        CastingUser u = new CastingUser();
        u.setName(MARKER_NAME);
        u.setCastingType("model");
        u.setGender("female");
        u.setRegion("Toshkent");
        u.setNationality("Ozbek");
        u.setAge(17);                       // ataylab voyaga yetmagan
        u.setBirthday(LocalDateTime.now().minusYears(17));
        u.setHeight(170);
        u.setHairColor("Qora");
        u.setEyeColor("Jigarrang");
        u.setIsWebShow(true);
        // Shaxsiy ma'lumotlar - javobda CHIQMASLIGI kerak
        u.setPhone("+998901234567");
        u.setEmail("sinov@example.com");
        u.setTelegram("@sinov");
        u.setFacebook("fb/sinov");
        u.setInstagram("ig/sinov");
        u.setTelegramId("123456789");
        u.setBust("80");
        u.setWaist("60");
        u.setSon("88");
        u.setClothSize("s");
        u.setShoeSize("38");
        u.setPrice(390.0);
        u.setStatus(1);
        u.setCreatedAt(LocalDateTime.now());
        castingUserRepo.save(u);
    }

    @Test
    @DisplayName("Ochiq katalogda shaxsiy ma'lumot YO'Q")
    void publicCatalogHasNoPersonalData() throws Exception {
        String body = mockMvc.perform(get("/api/v1/casting-user/web"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).as("anketa javobda bo'lishi kerak").contains(MARKER_NAME);

        for (String field : FORBIDDEN_FIELDS) {
            assertThat(body)
                    .as("Ochiq katalogda '%s' maydoni BO'LMASLIGI kerak - endpoint tokensiz ochiq", field)
                    .doesNotContain("\"" + field + "\"");
        }
        // Qiymatlarning o'zi ham chiqmasin (maydon nomi o'zgartirilgan bo'lsa ham)
        assertThat(body).doesNotContain("+998901234567");
        assertThat(body).doesNotContain("sinov@example.com");
    }

    @Test
    @DisplayName("Klientlarga kerak bo'lgan maydonlar joyida")
    void showcaseFieldsArePresent() throws Exception {
        String body = mockMvc.perform(get("/api/v1/casting-user/web"))
                .andReturn().getResponse().getContentAsString();

        // Sayt katalogi va mobil ilova aynan shularni ishlatadi
        for (String field : new String[]{
                "id", "name", "castingType", "gender", "region", "nationality",
                "age", "birthday", "height", "hairColor", "eyeColor", "photos"}) {
            assertThat(body)
                    .as("Klient '%s' maydonini ishlatadi - u qolishi kerak", field)
                    .contains("\"" + field + "\"");
        }
    }
}
