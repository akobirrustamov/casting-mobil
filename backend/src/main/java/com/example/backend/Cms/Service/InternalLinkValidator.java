package com.example.backend.Cms.Service;

import com.example.backend.Admin.Dto.InternalLinkDto;
import com.example.backend.Cms.Repository.CategoryRepo;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.CreatorRepo;
import com.example.backend.Cms.Repository.EpisodeRepo;
import com.example.backend.Cms.Repository.PremiereRepo;
import com.example.backend.Repository.CastingUserRepo;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;

/**
 * Havolani tekshirish — reklama, premyera va bildirishnoma uchun YAGONA joy (§28).
 *
 * <h2>Nima uchun kerak</h2>
 * Ilgari tekshiruv faqat «nishon turi va ID bo'sh emasmi» degan savolga
 * javob berardi. Ya'ni <b>CONTENT #999999</b> ga havola qiluvchi banner
 * bemalol saqlanardi va mobil ilovada bosilganda hech qayerga olib
 * bormasdi. Xato faqat foydalanuvchi bosgandan keyin bilinardi — admin
 * esa hech qanday ogohlantirish ko'rmagan edi.
 *
 * Endi nishon bazada bor-yo'qligi saqlash paytida tekshiriladi.
 *
 * <h2>Nima uchun alohida klass</h2>
 * Bir xil mexanizm uch modulda ishlatiladi. Tekshiruv HomepageService
 * ichida qolsa, bildirishnoma moduli uni takrorlashi kerak bo'lardi —
 * va takror nusxa vaqt o'tib asl nusxadan chetga chiqardi.
 */
@Service
@RequiredArgsConstructor
public class InternalLinkValidator {

    private final ContentRepo contentRepo;
    private final EpisodeRepo episodeRepo;
    private final CategoryRepo categoryRepo;
    private final CreatorRepo creatorRepo;
    private final PremiereRepo premiereRepo;
    private final CastingUserRepo castingUserRepo;

    /**
     * Havola to'g'ri to'ldirilgan va nishoni mavjudligini tekshiradi.
     *
     * @throws BusinessException maydonlar yetishmasa yoki nishon topilmasa
     */
    public void validate(InternalLinkDto link) {
        if (link == null || link.getLinkType() == null) {
            return;
        }
        switch (link.getLinkType()) {
            case EXTERNAL -> validateExternal(link.getLinkUrl());
            case INTERNAL -> validateInternal(link);
            default -> {
                // NONE — tekshiradigan narsa yo'q
            }
        }
    }

    private void validateExternal(String url) {
        if (url == null || url.isBlank()) {
            throw BusinessException.validation("Tashqi havola uchun URL kiritilmagan");
        }
        // Faqat http/https. Aks holda banner orqali `javascript:` yoki
        // `intent:` kabi sxemalarni klientga uzatish mumkin bo'lardi —
        // ya'ni admin panelidagi matn maydoni hujum vektoriga aylanardi.
        String scheme;
        try {
            scheme = URI.create(url.trim()).getScheme();
        } catch (IllegalArgumentException e) {
            throw BusinessException.validation("Tashqi havola noto'g'ri: " + url);
        }
        if (scheme == null) {
            throw BusinessException.validation(
                    "Tashqi havola to'liq bo'lishi kerak, masalan: https://example.uz");
        }
        String lower = scheme.toLowerCase(Locale.ROOT);
        if (!lower.equals("http") && !lower.equals("https")) {
            throw BusinessException.validation(
                    "Tashqi havola faqat http yoki https bo'lishi mumkin, kiritilgani: " + scheme);
        }
    }

    private void validateInternal(InternalLinkDto link) {
        if (link.getInternalTargetType() == null || link.getInternalTargetId() == null) {
            throw BusinessException.validation(
                    "Ichki havola uchun nishon turi va ID kiritilmagan");
        }
        Long id = link.getInternalTargetId();
        boolean exists = switch (link.getInternalTargetType()) {
            case CONTENT -> contentRepo.existsById(id);
            case EPISODE -> episodeRepo.existsById(id);
            case CATEGORY -> categoryRepo.existsById(id);
            case CREATOR -> creatorRepo.existsById(id);
            case PREMIERE -> premiereRepo.existsById(id);
            // Eski casting moduli — ID turi Integer.
            case CASTING -> id <= Integer.MAX_VALUE
                    && castingUserRepo.existsById(id.intValue());
            // OTHER — klient o'zi hal qiladigan nishon, bazada tekshirib
            // bo'lmaydi. Shu sababli bu turdan foydalanish tavsiya etilmaydi.
            case OTHER -> true;
        };
        if (!exists) {
            throw BusinessException.validation(
                    "Havola nishoni topilmadi: " + link.getInternalTargetType() + " #" + id);
        }
    }
}
