package com.example.backend.Cms.Service;

import com.example.backend.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Lokal diskda saqlash. Mavjud casting moduli bilan bir xil ildiz
 * ({@code backend/files}) ishlatiladi, shuning uchun ilova repozitoriy
 * ildizidan ishga tushirilishi kerak.
 */
@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private static final Path ROOT = Paths.get("backend", "files");

    /** Ruxsat etilgan kengaytmalar. Boshqasi qabul qilinmaydi. */
    private static final Set<String> ALLOWED = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "svg",
            "mp4", "mov", "webm", "m4v",
            "pdf");

    @Override
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.validation("Fayl bo'sh");
        }
        try {
            return store(file.getInputStream(), file.getOriginalFilename(), folder);
        } catch (IOException e) {
            log.error("Yuklangan faylni o'qib bo'lmadi", e);
            throw new BusinessException("STORAGE_ERROR", "Fayl saqlanmadi",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String store(InputStream in, String originalFilename, String folder) {
        String extension = extensionOf(originalFilename);
        if (!ALLOWED.contains(extension)) {
            throw BusinessException.validation("Bu turdagi fayl qabul qilinmaydi: " + extension);
        }

        // Nom butunlay server tomonida yasaladi - foydalanuvchi nomidan faqat
        // kengaytma olinadi. Shu sababli "../../etc/passwd" kabi yo'l bo'lishi mumkin emas.
        String safeFolder = folder == null ? "misc" : folder.replaceAll("[^a-zA-Z0-9_-]", "");
        String fileName = UUID.randomUUID() + "." + extension;
        String key = "/" + safeFolder + "/" + fileName;

        Path target = ROOT.resolve(safeFolder).resolve(fileName).normalize();
        if (!target.startsWith(ROOT)) {
            throw BusinessException.validation("Yo'l noto'g'ri");
        }

        try (InputStream stream = in) {
            Files.createDirectories(target.getParent());
            // Oqim orqali ko'chiriladi - katta video RAM'ga to'liq yuklanmaydi.
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Fayl saqlanmadi: {}", key, e);
            throw new BusinessException("STORAGE_ERROR", "Fayl saqlanmadi",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return key;
    }

    @Override
    public boolean accepts(String originalFilename) {
        return ALLOWED.contains(extensionOf(originalFilename));
    }

    @Override
    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw BusinessException.notFound("Media", storageKey);
        }
        return new FileSystemResource(path);
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.isRegularFile(resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        Path path = resolve(storageKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // O'chirilmagan fayl — bu egasiz bayt, ma'lumot yo'qolishi emas.
            log.warn("Fayl o'chirilmadi: {}", storageKey, e);
        }
    }

    private Path resolve(String storageKey) {
        String key = storageKey == null ? "" : storageKey;
        Path path = ROOT.resolve(key.startsWith("/") ? key.substring(1) : key).normalize();
        // Kalit bazadan kelsa ham tekshiriladi - ildizdan tashqariga chiqib bo'lmaydi.
        if (!path.startsWith(ROOT)) {
            throw BusinessException.validation("Yo'l noto'g'ri");
        }
        return path;
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
