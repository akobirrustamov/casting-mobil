package com.example.backend.Cms.Service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Fayl saqlash abstraksiyasi.
 *
 * Provayder nomi biznes logikaga QOTIRILMAYDI: bugun lokal disk, ertaga Timeweb
 * yoki S3 bo'lishi mumkin. Almashtirish uchun faqat shu interfeysning yangi
 * implementatsiyasi yoziladi.
 */
public interface StorageService {

    /**
     * Faylni saqlaydi va storage kalitini qaytaradi.
     *
     * Fayl nomi SERVER TOMONIDA generatsiya qilinadi - foydalanuvchi yuborgan
     * nom yo'l sifatida ishlatilmaydi (path traversal himoyasi).
     */
    String store(MultipartFile file, String folder);

    /**
     * Oqimdan saqlash - bo'laklab yuklangan faylni yig'ish uchun.
     *
     * Multipart varianti ham shu metodga tayanadi, ya'ni kengaytma tekshiruvi
     * va yo'l himoyasi BITTA joyda qoladi.
     *
     * @param in               metod o'zi yopadi
     * @param originalFilename faqat kengaytma olinadi, yo'l sifatida ishlatilmaydi
     */
    String store(InputStream in, String originalFilename, String folder);

    /**
     * Bu nomdagi fayl umuman qabul qilinadimi (kengaytma bo'yicha).
     *
     * Bo'laklab yuklashda BOSHIDA kerak: aks holda foydalanuvchi gigabaytlab
     * ma'lumot yuborib bo'lgach, yig'ish paytida rad javobini olardi.
     */
    boolean accepts(String originalFilename);

    /** Kalit bo'yicha faylni o'qish uchun ochadi. */
    Resource load(String storageKey);

    /** Fayl mavjudligini tekshiradi. */
    boolean exists(String storageKey);

    /**
     * Faylni diskdan o'chiradi.
     *
     * Fayl topilmasa xato EMAS: baza yozuvi allaqachon o'chirilgan bo'lishi
     * mumkin va bu holat tuzatishni talab qilmaydi.
     */
    void delete(String storageKey);
}
