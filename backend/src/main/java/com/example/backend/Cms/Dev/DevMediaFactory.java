package com.example.backend.Cms.Dev;

import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Enums.MediaStatus;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Repository.MediaAssetRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Dev uchun o'rin egallovchi rasmlar generatori.
 *
 * Haqiqiy fayllarni yuklashning hojati bo'lmasin uchun rasmlar kod bilan
 * chiziladi: gradient fon, sarlavha matni va "DEV MA'LUMOTI" yozuvi.
 * Fayl nomi deterministik, shuning uchun qayta ishga tushirishda ular
 * qaytadan chizilmaydi.
 *
 * Faqat dev profilida ishlaydi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevMediaFactory {

    private static final String FILES_ROOT = "backend/files";
    private static final String PREFIX = "/cms-dev";

    /** Admin panel to'q ko'k, shuning uchun rasmlar ham shu gammada. */
    private static final Color[][] GRADIENTS = {
            {new Color(0x0B1B3A), new Color(0x1E3A8A)},
            {new Color(0x111C44), new Color(0x2563EB)},
            {new Color(0x0A1730), new Color(0x3B82F6)},
            {new Color(0x1A1035), new Color(0x6D28D9)},
            {new Color(0x0F172A), new Color(0x0EA5E9)},
    };

    private final MediaAssetRepo mediaAssetRepo;

    /** Yonlama poster (16:9). */
    public MediaAsset landscape(String label, int variant) {
        return image(label, variant, 1280, 720, "landscape");
    }

    /** Tik poster (9:16) - Reels uslubidagi kontent uchun. */
    public MediaAsset vertical(String label, int variant) {
        return image(label, variant, 720, 1280, "vertical");
    }

    /** Kvadrat avatar - ijodkorlar uchun. */
    public MediaAsset avatar(String label, int variant) {
        return image(label, variant, 512, 512, "avatar");
    }

    private MediaAsset image(String label, int variant, int w, int h, String kind) {
        String fileName = "dev-" + kind + "-" + slug(label) + "-" + variant + ".jpg";
        File file = new File(FILES_ROOT + PREFIX + "/" + fileName);
        try {
            if (!file.isFile()) {
                file.getParentFile().mkdirs();
                ImageIO.write(render(label, variant, w, h), "jpg", file);
            }
        } catch (IOException e) {
            log.warn("Dev rasm yaratilmadi: {}", fileName, e);
        }

        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey(PREFIX + "/" + fileName)
                .originalFilename(fileName)
                .type(MediaType.IMAGE)
                .mimeType("image/jpeg")
                .sizeBytes(file.isFile() ? file.length() : 0L)
                .width(w)
                .height(h)
                .status(MediaStatus.READY)
                .build());
    }

    /**
     * Dev uchun video yozuvi.
     *
     * <h2>⚠️ Bu HAQIQIY video EMAS</h2>
     * Fayl ichida tasodifiy baytlar - pleyerda o'ynamaydi. Haqiqiy video
     * yaratish uchun kodlagich (ffmpeg) kerak, u esa loyihaning talabi emas.
     *
     * <h2>Unda nega umuman fayl yoziladi</h2>
     * Ilgari faqat metadata yozilardi va fayl diskda yo'q edi. Natijada video
     * yetkazish yo'lini tekshirib bo'lmasdi: har qanday so'rov 404 qaytarardi
     * va "ruxsat yo'q" bilan "fayl yo'q" bir-biridan farq qilmasdi.
     *
     * Endi fayl bor, ya'ni lokal muhitda quyidagilarni haqiqiy tekshirish
     * mumkin: entitlement (pullik qism anonimga berilmasligi), {@code Range}
     * so'rovi va 206 javob, kesh sarlavhalari.
     */
    public MediaAsset video(String label, int seconds) {
        String fileName = "dev-video-" + slug(label) + ".mp4";
        File file = new File(FILES_ROOT + PREFIX + "/" + fileName);
        try {
            if (!file.isFile()) {
                file.getParentFile().mkdirs();
                // Bo'laklab so'rash ma'noli bo'lishi uchun yetarli hajm.
                byte[] filler = new byte[256 * 1024];
                new java.util.Random(seconds).nextBytes(filler);
                java.nio.file.Files.write(file.toPath(), filler);
            }
        } catch (IOException e) {
            log.warn("Dev video to'ldirgichi yaratilmadi: {}", fileName, e);
        }

        return mediaAssetRepo.save(MediaAsset.builder()
                .storageKey(PREFIX + "/" + fileName)
                .originalFilename(fileName)
                .type(MediaType.VIDEO)
                .mimeType("video/mp4")
                .sizeBytes(file.isFile() ? file.length() : 0L)
                .durationSeconds(seconds)
                .width(1920)
                .height(1080)
                .status(MediaStatus.READY)
                .build());
    }

    private BufferedImage render(String label, int variant, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color[] pair = GRADIENTS[Math.floorMod(variant, GRADIENTS.length)];
        g.setPaint(new GradientPaint(0, 0, pair[0], w, h, pair[1]));
        g.fillRect(0, 0, w, h);

        // Yengil to'r - tekis fon quruq ko'rinmasligi uchun
        g.setColor(new Color(255, 255, 255, 14));
        for (int x = 0; x < w; x += 48) {
            g.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 48) {
            g.drawLine(0, y, w, y);
        }

        int titleSize = Math.max(22, w / 18);
        g.setFont(new Font("SansSerif", Font.BOLD, titleSize));
        g.setColor(Color.WHITE);
        drawWrapped(g, label, w, h, titleSize);

        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(12, w / 46)));
        g.setColor(new Color(255, 255, 255, 150));
        String note = "DEV MA'LUMOTI";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(note, (w - fm.stringWidth(note)) / 2, h - Math.max(18, h / 22));

        g.dispose();
        return img;
    }

    /** Uzun sarlavhani bir necha qatorga bo'lib, markazga chizadi. */
    private void drawWrapped(Graphics2D g, String text, int w, int h, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        int maxWidth = (int) (w * 0.82);
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        int totalHeight = lines.size() * (lineHeight + 6);
        int y = (h - totalHeight) / 2 + lineHeight;
        for (String l : lines) {
            g.drawString(l, (w - fm.stringWidth(l)) / 2, y);
            y += lineHeight + 6;
        }
    }

    private String slug(String s) {
        String out = s.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return out.isEmpty() ? "x" : out;
    }
}
