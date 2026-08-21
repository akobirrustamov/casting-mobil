package com.example.backend.Admin.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Bosh sahifa bo'limlarining yangi tartibi (ТЗ §31).
 *
 * <h2>Nima uchun bitta so'rov</h2>
 * Ilgari tartib har bir bo'lim uchun alohida {@code PUT} bilan
 * o'rnatilardi. Admin bo'limni yuqoriga sudrasa, panel 8 ta bo'limni
 * qayta raqamlashi va 8 ta so'rov yuborishi kerak edi — oradagi vaqtda
 * bazada ikkita bo'lim bir xil raqamda turardi va {@code /app/home} ni
 * so'ragan foydalanuvchi ARALASHIB KETGAN bosh sahifani ko'rardi.
 *
 * Endi butun tartib bitta tranzaksiyada qo'llanadi.
 */
@Data
public class SectionOrderRequest {

    /**
     * Bo'lim ID'lari — KO'RINISH tartibida. Birinchi element eng yuqorida.
     *
     * Ro'yxatga kirmagan bo'limlar tartibi o'zgarmaydi va ular
     * ro'yxatdagilardan keyin turadi.
     */
    @NotNull(message = "Bo'limlar ro'yxati kiritilmagan")
    private List<Long> sectionIds = new ArrayList<>();
}
