package com.example.backend.Cms.Enums;

/**
 * Bir martalik xarid turi (ТЗ: monetizatsiyaning 1 va 2-darajasi).
 */
public enum PurchaseType {

    /** Bitta qism — default 3 000 so'm. Faqat shu qism ochiladi. */
    EPISODE,

    /**
     * Butun premyera — default 15 000 so'm.
     *
     * ⚠️ Buyurtmachi «mavjud qismlarini» degan. Kelajakdagi qismlarga
     * tarqaladimi — aytilmagan (roadmap.md §8, 3-savol). Hozircha xarid
     * paytida mavjud bo'lgan va keyin qo'shilgan qismlarga ham tarqaladi;
     * javob kelgach {@code coversFutureEpisodes} bilan cheklash mumkin.
     */
    PREMIERE
}
