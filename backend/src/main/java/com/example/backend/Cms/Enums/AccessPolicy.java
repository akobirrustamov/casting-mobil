package com.example.backend.Cms.Enums;

/**
 * Kontentga kirish siyosati.
 *
 * Entitlement to'rt manbadan kelishi mumkin: bitta qism xaridi, butun premyera
 * xaridi, faol Premium obuna, bepul kontent. Tekshiruv bitta joyda - AccessService.
 */
public enum AccessPolicy {

    /** Hamma ko'ra oladi. */
    FREE,

    /** Faqat Premium obunachilar. */
    PREMIUM_ONLY,

    /** Faqat sotib olganlar (qism yoki premyera). */
    PURCHASE_ONLY,

    /** Premium YOKI sotib olish - ikkalasi ham ochadi. */
    PREMIUM_OR_PURCHASE;

    public boolean requiresPayment() {
        return this != FREE;
    }
}
