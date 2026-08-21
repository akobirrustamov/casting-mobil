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

    @NotNull(message = "Kontent ro'yxati kiritilmagan")
    private List<Long> contentIds = new ArrayList<>();
}
