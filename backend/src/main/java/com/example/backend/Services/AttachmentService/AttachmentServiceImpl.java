package com.example.backend.Services.AttachmentService;

import com.example.backend.Entity.Attachment;
import com.example.backend.Repository.AttachmentRepo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLConnection;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private static final String ROOT = "backend/files";

    private final AttachmentRepo attachmentRepo;

    @Override
    public HttpEntity<?> uploadFile(MultipartFile photo, String prefix) throws IOException {
        if (photo == null || photo.isEmpty()) {
            return ResponseEntity.badRequest().body("Fayl bo'sh");
        }

        String safePrefix = safePrefix(prefix);
        UUID id = UUID.randomUUID();
        String fileName = id + "_" + safeFileName(photo.getOriginalFilename());

        // Saqlash yo'li avvalgidek: backend/files{prefix}/{fileName}
        File file = new File(ROOT + safePrefix + "/" + fileName);
        file.getParentFile().mkdirs();

        try (OutputStream outputStream = new FileOutputStream(file)) {
            FileCopyUtils.copy(photo.getInputStream(), outputStream);
        }
        Attachment attachment = new Attachment(id, safePrefix, fileName, true);
        attachmentRepo.save(attachment);
        return ResponseEntity.ok(id);
    }


    @Override
    public void getFile(HttpServletResponse response, UUID id) throws IOException {
        Optional<Attachment> attachmentOptional = attachmentRepo.findById(id);
        if (attachmentOptional.isEmpty()) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Fayl topilmadi");
            return;
        }

        Attachment attachment = attachmentOptional.get();
        File file = resolveFile(attachment);
        if (file == null) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Fayl topilmadi");
            return;
        }

        response.setContentType(detectContentType(attachment.getName()));
        response.setContentLengthLong(file.length());
        response.setHeader("Content-Disposition", "inline; filename=\"" + attachment.getName() + "\"");
        try (InputStream inputStream = new FileInputStream(file)) {
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
    }

    /**
     * Fayl ikki xil yo'l qoidasi bilan saqlangan bo'lishi mumkin:
     *  - yangi/upload qoidasi:  backend/files{prefix}/{name}
     *  - eski o'qish qoidasi:   backend/files/{prefix}/{name}
     * Ikkalasi ham tekshiriladi, shuning uchun eski fayllar ham ochilaveradi.
     */
    private File resolveFile(Attachment attachment) {
        String prefix = attachment.getPrefix() == null ? "" : attachment.getPrefix();
        String name = attachment.getName();

        File[] candidates = {
                new File(ROOT + prefix + "/" + name),
                new File(ROOT + "/" + prefix + "/" + name)
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    private String detectContentType(String fileName) {
        String type = URLConnection.guessContentTypeFromName(fileName == null ? "" : fileName);
        return type != null ? type : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /** Katalogdan chiqib ketuvchi ".." va boshqa xavfli qismlarni olib tashlaydi. */
    private String safePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        String cleaned = prefix.replace("\\", "/").replace("..", "");
        return cleaned.isEmpty() || cleaned.startsWith("/") ? cleaned : "/" + cleaned;
    }

    private String safeFileName(String originalName) {
        String cleaned = StringUtils.getFilename(originalName == null ? "" : originalName.replace("\\", "/"));
        if (cleaned == null || cleaned.isBlank()) {
            return "file";
        }
        return cleaned.replace("..", "");
    }
}
