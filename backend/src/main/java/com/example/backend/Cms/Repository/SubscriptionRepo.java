package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByUserIdOrderByEndAtDesc(UUID userId);
}
