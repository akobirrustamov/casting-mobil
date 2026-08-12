package com.example.backend.Controller;

import com.example.backend.DTO.NewsDTO;
import com.example.backend.Entity.Attachment;
import com.example.backend.Entity.News;
import com.example.backend.Repository.AttachmentRepo;
import com.example.backend.Repository.NewsRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@CrossOrigin
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsRepo newsRepo;
    private final AttachmentRepo attachmentRepo;

    @GetMapping
    public HttpEntity<?> getAllNews() {
        List<News> newsList = newsRepo.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(newsList);
    }
    @PostMapping
    public HttpEntity<?> addNews(@RequestBody NewsDTO newsDTO) {
        try {
            System.out.println(newsDTO);
            Attachment attachment = null;
            if (newsDTO.getMainPhoto() != null) {
                Optional<Attachment> byId = attachmentRepo.findById(newsDTO.getMainPhoto());
                if (byId.isPresent()) {
                    attachment = byId.get();
                } else {
                    return ResponseEntity.notFound().build();
                }
            }
            List<Attachment> attachments = new ArrayList<>();
            if (newsDTO.getPhotos() != null && !newsDTO.getPhotos().isEmpty()) {
                for (UUID photoId : newsDTO.getPhotos()) {
                    Optional<Attachment> byId = attachmentRepo.findById(photoId);
                    if (byId.isPresent()) {
                        attachments.add(byId.get());
                    } else {
                        return ResponseEntity.badRequest().body("Attachment with ID " + photoId + " not found");
                    }
                }
            }
            News newsEntity = new News(
                    newsDTO.getTitleUz(),
                    newsDTO.getTitleRu(),
                    newsDTO.getDescriptionUz(),
                    newsDTO.getDescriptionRu(),
                    newsDTO.getLink(),
                    attachment,
                    attachments
            );
            News savedNews = newsRepo.save(newsEntity);
            return ResponseEntity.ok(savedNews);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating news: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public HttpEntity<?> updateNews(@PathVariable Integer id, @RequestBody NewsDTO newsDTO) {
        try {
            Optional<News> existingNewsOpt = newsRepo.findById(id);
            if (!existingNewsOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            News existingNews = existingNewsOpt.get();

            // Update basic fields
            existingNews.setTitleUz(newsDTO.getTitleUz());
            existingNews.setTitleRu(newsDTO.getTitleRu());
            existingNews.setDescriptionUz(newsDTO.getDescriptionUz());
            existingNews.setDescriptionRu(newsDTO.getDescriptionRu());
            existingNews.setLink(newsDTO.getLink());

            // Handle main photo update
            if (newsDTO.getMainPhoto() != null) {
                Optional<Attachment> mainPhotoOpt = attachmentRepo.findById(newsDTO.getMainPhoto());
                if (mainPhotoOpt.isPresent()) {
                    existingNews.setMainPhoto(mainPhotoOpt.get());
                } else {
                    return ResponseEntity.badRequest().body("Main photo attachment not found");
                }
            } else {
                existingNews.setMainPhoto(null);
            }

            // Handle additional photos update
            List<Attachment> updatedAttachments = new ArrayList<>();
            if (newsDTO.getPhotos() != null && !newsDTO.getPhotos().isEmpty()) {
                for (UUID photoId : newsDTO.getPhotos()) {
                    Optional<Attachment> attachmentOpt = attachmentRepo.findById(photoId);
                    if (attachmentOpt.isPresent()) {
                        updatedAttachments.add(attachmentOpt.get());
                    } else {
                        return ResponseEntity.badRequest().body("Attachment with ID " + photoId + " not found");
                    }
                }
            }
            existingNews.setPhotos(updatedAttachments);

            News updatedNews = newsRepo.save(existingNews);
            return ResponseEntity.ok(updatedNews);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating news: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public HttpEntity<?> deleteNews(@PathVariable Integer id) {
        try {
            Optional<News> newsOpt = newsRepo.findById(id);
            if (!newsOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            newsRepo.deleteById(id);
            return ResponseEntity.ok("News deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting news: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public HttpEntity<?> getNewsById(@PathVariable Integer id) {
        try {
            Optional<News> newsEntity = newsRepo.findById(id);
            if (newsEntity.isPresent()) {
                return ResponseEntity.ok(newsEntity.get());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching news: " + e.getMessage());
        }
    }
}