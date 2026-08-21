package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Skanerlanadigan paketlar ATAYLAB sanab o'tilgan.
 *
 * Sukut bo'yicha Spring butun com.example.backend ni skanerlagan bo'lardi, lekin
 * bu yerda cheklov mavjud edi (faqat Entity va Repository). UZCASTING CMS moduli
 * alohida Cms.* paketida turadi, shuning uchun ular ham qo'shildi.
 *
 * Yangi modul qo'shilganda shu ro'yxatni yangilash ESDAN CHIQMASIN - aks holda
 * repozitoriylar topilmay ilova ko'tarilmaydi.
 */
@SpringBootApplication
// Analitika agregatsiyasi fon vazifasi sifatida ishlaydi (AnalyticsService).
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.example.backend.Repository",
        "com.example.backend.Cms.Repository"
})
@EntityScan(basePackages = {
        "com.example.backend.Entity",
        "com.example.backend.Cms.Entity"
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
