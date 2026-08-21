package com.example.backend.Cms.Service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * «Bu foydalanuvchi buni ko'ra oladimi» degan savolga javob.
 *
 * Faqat ha/yo'q emas: klient nima qilish kerakligini ham bilishi kerak —
 * obuna sotib olish, qismni sotib olish yoki kirish.
 */
@Data
@Builder
public class AccessDecision {

    public enum Reason {
        /** Kontent bepul. */
        FREE,
        /** Faol Premium obuna. */
        PREMIUM,
        /** Aynan shu qism sotib olingan. */
        EPISODE_PURCHASE,
        /** Butun premyera sotib olingan. */
        PREMIERE_PURCHASE,

        // --- rad etish sabablari ---
        /** Kontent hali nashr qilinmagan. */
        NOT_PUBLISHED,
        /** Foydalanuvchi bloklangan. */
        USER_BLOCKED,
        /** Tizimga kirmagan — bepul bo'lmagan kontent uchun. */
        NOT_AUTHENTICATED,
        /** To'lov kerak. */
        PAYMENT_REQUIRED
    }

    /** Nima qilish kerak — klient shu asosda tugma ko'rsatadi. */
    public enum RequiredAction {
        NONE,
        SIGN_IN,
        BUY_EPISODE,
        BUY_PREMIERE,
        SUBSCRIBE,
        /** Qism ham, premyera ham, obuna ham mumkin. */
        BUY_OR_SUBSCRIBE
    }

    private boolean allowed;
    private Reason reason;
    private RequiredAction requiredAction;

    /** Qism narxi — sotib olish taklif qilinsa. */
    private BigDecimal episodePrice;

    /** Butun premyera narxi. */
    private BigDecimal premierePrice;

    public static AccessDecision allow(Reason reason) {
        return AccessDecision.builder()
                .allowed(true)
                .reason(reason)
                .requiredAction(RequiredAction.NONE)
                .build();
    }

    public static AccessDecision deny(Reason reason, RequiredAction action) {
        return AccessDecision.builder()
                .allowed(false)
                .reason(reason)
                .requiredAction(action)
                .build();
    }
}
