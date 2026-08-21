package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UploadSessionRepo extends JpaRepository<UploadSession, String> {

    /** Tashlab ketilgan sessiyalarni tozalash uchun. */
    List<UploadSession> findAllByStatusAndCreatedAtBefore(String status, LocalDateTime before);
}
