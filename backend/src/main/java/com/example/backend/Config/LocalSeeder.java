package com.example.backend.Config;

import com.example.backend.Entity.Attachment;
import com.example.backend.Entity.CastingUser;
import com.example.backend.Repository.AttachmentRepo;
import com.example.backend.Repository.CastingUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lokal sinov ma'lumotlari: mobil ilova bo'sh ekranlar o'rniga to'ldirilgan
 * ro'yxatlarni ko'rsatishi uchun.
 * <p>
 * Faqat {@code app.local.seed=true} bo'lganda ishlaydi — ya'ni faqat
 * {@code local} profilida. Serverda bu xossa yo'q, demak hech qachon ishga tushmaydi.
 * <p>
 * Ma'lumotlar butunlay o'ylab topilgan: haqiqiy ismlar, telefon, email va
 * jismoniy o'lchovlar ishlatilmaydi. Jonli saytdagi anketalar shaxsiy
 * ma'lumot bo'lgani uchun ko'chirilmaydi (batafsil: mobile/docs/API.md).
 */
@Slf4j
@Component
@Order(20) // AutoRun rollarni yaratib bo'lgandan keyin
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.local.seed", havingValue = "true")
public class LocalSeeder implements CommandLineRunner {

    /** AttachmentServiceImpl fayllarni shu yerdan o'qiydi — bir xil yo'l. */
    private static final String FILES_ROOT = "backend/files";
    private static final String PREFIX = "/casting";

    private static final int PHOTO_W = 800;
    private static final int PHOTO_H = 1000;

    private final CastingUserRepo castingUserRepo;
    private final AttachmentRepo attachmentRepo;

    /**
     * Ism, casting turi, jins, viloyat, bo'y, tug'ilgan yil, foto soni.
     * <p>
     * Jins qiymatlari jonli API bilan bir xil bo'lishi SHART: "female" / "male".
     * O'zbekcha ("Ayol"/"Erkak") yozilgan edi — natijada ilovadagi filtr hech
     * kimni topmadi, garchi kod to'g'ri ishlagan bo'lsa ham. Ko'rinadigan matn
     * i18n'dan olinadi, bazada esa faqat texnik qiymat turadi.
     */
    private static final Object[][] PEOPLE = {
            {"Malika Yusupova",   "model",      "female",   "Toshkent",     176, 2001, 3},
            {"Aziz Rahimov",      "actor",      "male",  "Samarqand",    182, 1996, 2},
            {"Nilufar Qodirova",  "euromodel",  "female",   "Toshkent",     179, 2000, 3},
            {"Sardor Ergashev",   "bloger",     "male",  "Farg'ona",     178, 1999, 2},
            {"Kamola Tursunova",  "actor",      "female",   "Buxoro",       168, 1997, 2},
            {"Jasur Nazarov",     "model",      "male",  "Toshkent",     186, 1998, 3},
            {"Zilola Sobirova",   "influencer", "female",   "Andijon",      170, 2002, 2},
            {"Bekzod Umarov",     "extra",      "male",  "Namangan",     175, 1995, 1},
            {"Dilnoza Ahmedova",  "model",      "female",   "Toshkent",     174, 2003, 3},
            {"Temur Xolmatov",    "actor",      "male",  "Qashqadaryo",  180, 1993, 2},
            {"Sevara Islomova",   "bloger",     "female",   "Toshkent",     165, 2001, 2},
            {"Rustam Qosimov",    "influencer", "male",  "Xorazm",       177, 1994, 2},
    };

    /** Fotolar uchun ranglar — ilova palitrasidan (mobile/src/theme/tokens.ts). */
    private static final Color[][] GRADIENTS = {
            {new Color(0x7C3AED), new Color(0xEC4899)},
            {new Color(0x22D3EE), new Color(0x7C3AED)},
            {new Color(0xEC4899), new Color(0xF5C542)},
            {new Color(0x11111F), new Color(0x7C3AED)},
            {new Color(0x22D3EE), new Color(0x34D399)},
    };

    @Override
    public void run(String... args) {
        if (castingUserRepo.count() > 0) {
            log.info("LocalSeeder: baza bo'sh emas, seed o'tkazib yuborildi");
            return;
        }

        int photoCount = 0;
        for (int i = 0; i < PEOPLE.length; i++) {
            Object[] row = PEOPLE[i];
            String name = (String) row[0];
            int photos = (Integer) row[6];

            List<Attachment> attachments = new ArrayList<>();
            for (int p = 0; p < photos; p++) {
                Attachment attachment = createPhoto(name, i, p);
                if (attachment != null) {
                    attachments.add(attachment);
                    photoCount++;
                }
            }

            castingUserRepo.save(buildUser(row, attachments));
        }

        log.info("LocalSeeder: {} ta anketa va {} ta foto yaratildi",
                PEOPLE.length, photoCount);
        log.info("LocalSeeder: fotolar papkasi -> {}",
                new File(FILES_ROOT + PREFIX).getAbsolutePath());
    }

    private CastingUser buildUser(Object[] row, List<Attachment> attachments) {
        String name = (String) row[0];
        int birthYear = (Integer) row[5];
        int age = LocalDateTime.now().getYear() - birthYear;

        CastingUser user = new CastingUser();
        user.setName(name);
        user.setCastingType((String) row[1]);
        user.setGender((String) row[2]);
        user.setRegion((String) row[3]);
        user.setHeight((Integer) row[4]);
        user.setBirthday(LocalDateTime.of(birthYear, 1 + (age % 12), 1 + (age % 27), 0, 0));
        user.setAge(age);
        user.setNationality("O'zbek");
        user.setHairColor(age % 2 == 0 ? "Qora" : "Jigarrang");
        user.setEyeColor(age % 3 == 0 ? "Ko'k" : "Qora");
        user.setClothSize(String.valueOf(42 + (age % 6)));
        user.setShoeSize(String.valueOf(37 + (age % 7)));
        user.setPrice(500000.0 + (age % 5) * 250000);
        user.setCreatedAt(LocalDateTime.now().minusDays(age % 30));

        // status=1 — "Castingdan o'tdi", saytdagi ma'no bilan bir xil
        user.setStatus(1);
        user.setIsWebShow(Boolean.TRUE);
        user.setFirstChan(0);
        user.setSecondChan(0);

        // Telefon, email, telegram va tana o'lchovlari ataylab bo'sh:
        // bular shaxsiy ma'lumot, sinov uchun kerak emas.
        user.setPhotos(attachments);
        return user;
    }

    /**
     * Fotoni generatsiya qiladi va Attachment sifatida saqlaydi.
     * UUID ism asosida — qayta ishga tushirilganda o'sha fayl qayta ishlatiladi.
     */
    private Attachment createPhoto(String personName, int personIndex, int photoIndex) {
        UUID id = UUID.nameUUIDFromBytes(
                ("uzcasting-seed:" + personName + ":" + photoIndex).getBytes(StandardCharsets.UTF_8));
        String fileName = id + "_photo.jpg";
        File file = new File(FILES_ROOT + PREFIX + "/" + fileName);

        try {
            if (!file.isFile()) {
                file.getParentFile().mkdirs();
                ImageIO.write(render(personName, personIndex + photoIndex), "jpg", file);
            }
        } catch (IOException e) {
            log.warn("LocalSeeder: {} fotoni yozib bo'lmadi: {}", fileName, e.getMessage());
            return null;
        }

        return attachmentRepo.save(new Attachment(id, PREFIX, fileName, Boolean.TRUE));
    }

    /** Gradient + bosh harflar. Haqiqiy suratlar o'rniga o'rin egallovchi rasm. */
    private BufferedImage render(String personName, int variant) {
        BufferedImage image = new BufferedImage(PHOTO_W, PHOTO_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color[] pair = GRADIENTS[Math.floorMod(variant, GRADIENTS.length)];
        g.setPaint(new GradientPaint(0, 0, pair[0], PHOTO_W, PHOTO_H, pair[1]));
        g.fillRect(0, 0, PHOTO_W, PHOTO_H);

        // Pastki qismini biroz qoraytiramiz — ilovada ism ustiga yozilganda o'qilsin
        g.setPaint(new GradientPaint(0, PHOTO_H * 0.55f, new Color(0, 0, 0, 0),
                0, PHOTO_H, new Color(0, 0, 0, 190)));
        g.fillRect(0, 0, PHOTO_W, PHOTO_H);

        g.setColor(new Color(255, 255, 255, 235));
        g.setFont(new Font("Segoe UI", Font.BOLD, 240));
        String initials = initialsOf(personName);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(initials,
                (PHOTO_W - fm.stringWidth(initials)) / 2f,
                PHOTO_H / 2f + fm.getAscent() / 2f - 60);

        g.setFont(new Font("Segoe UI", Font.PLAIN, 34));
        String note = "TEST MA'LUMOTI";
        FontMetrics fm2 = g.getFontMetrics();
        g.setColor(new Color(255, 255, 255, 150));
        g.drawString(note, (PHOTO_W - fm2.stringWidth(note)) / 2f, PHOTO_H - 70f);

        g.dispose();
        return image;
    }

    private String initialsOf(String name) {
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return sb.toString();
    }
}
