package com.example.backend.Admin.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Bosh sahifa qatoriga qaysi kontent kirishi (ТЗ §31 — «Custom content rows»).
 *
 * Tartib ro'yxatning O'ZI: birinchi element birinchi ko'rinadi. Alohida
 * {@code sortOrder} maydoni so'ralmaydi — admin panelida elementlar
 * sudrab ko'chiriladi va ro'yxat tartibi tabiiy natija bo'ladi.
 */
@Data
public class SectionItemsRequest {

    /**
     * ⚠️ Bu yerda {@code @NotNull} ATAYLAB YO'Q.
     *
     * Bo'sh ro'yxat MA'NOLI: u «qatorni butunlay tozalash» degani.
     * Maydonda standart qiymat bor, ya'ni u hech qachon null
     * bo'lmaydi — annotatsiya himoya qilayotgandek ko'rinib,
     * aslida hech narsa qilmasdi. Ishlamaydigan annotatsiya
     * yo'qidan yomonroq: u tekshiruv bor degan taassurot
     * qoldiradi.
     */
    private List<Long> contentIds = new ArrayList<>();
}
