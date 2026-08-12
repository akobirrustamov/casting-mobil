package com.example.backend.Repository;

import com.example.backend.Entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsRepo extends JpaRepository<News, Integer> {
    List<News> findAllByOrderByCreatedAtDesc();
}
