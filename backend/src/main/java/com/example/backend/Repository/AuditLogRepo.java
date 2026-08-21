package com.example.backend.Repository;

import com.example.backend.Entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    Page<AuditLog> findAllByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findAllByActionOrderByCreatedAtDesc(String action, Pageable pageable);
}
