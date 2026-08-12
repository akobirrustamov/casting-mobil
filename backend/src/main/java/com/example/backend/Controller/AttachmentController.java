package com.example.backend.Controller;

import com.example.backend.Entity.Attachment;
import com.example.backend.Repository.AttachmentRepo;
import com.example.backend.Services.AttachmentService.AttachmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
@CrossOrigin
@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentRepo attachmentRepo;
    private final AttachmentService attachmentService;
    @PostMapping("/upload")
    public HttpEntity<?> uploadFile(@RequestParam MultipartFile photo, @RequestParam String prefix) throws IOException {
        System.out.println(photo);
        System.out.println(prefix);
        return attachmentService.uploadFile(photo, prefix);
    }
    @GetMapping("/getFile/{id}")
    public void getFile(HttpServletResponse response, @PathVariable UUID id) throws IOException {
        attachmentService.getFile(response, id);
    }

    @PutMapping("/{attachmentId}")
    public ResponseEntity<?> updateAttachment(@PathVariable UUID attachmentId) {
        Optional<Attachment> byId = attachmentRepo.findById(attachmentId);
        if (byId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Attachment attachment = byId.get();

        // null bo‘lsa false deb olamiz
        boolean current = attachment.getIsWebShow() != null && attachment.getIsWebShow();
        attachment.setIsWebShow(!current);
        attachmentRepo.save(attachment);
        return ResponseEntity.ok(attachment);
    }


}
