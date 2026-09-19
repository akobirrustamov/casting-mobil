package com.example.backend.Cms.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Mobil ilovadan casting anketasi ({@code POST /api/v1/app/casting/applications}).
 *
 * <h2>Nega eski {@code CastingUserDTO} ishlatilmaydi</h2>
 * U bot uchun: {@code telegramId}, {@code price}, {@code status}, {@code age}
 * ni KLIENTDAN oladi. Ilovada bularni odamning o'zi yozishi mumkin bo'lmasligi
 * kerak — narx va holatni faqat admin qo'yadi, yosh esa tug'ilgan kundan
 * hisoblanadi. Bu DTO'da ular umuman yo'q, ya'ni yuborilsa ham e'tiborga
 * olinmaydi.
 *
 * <h2>⚠️ Uzunlik chegaralari</h2>
 * {@code casting_user} dagi barcha matn ustunlari {@code varchar(255)} (V1).
 * Chegara bu yerda qo'yilmasa uzun matn bazaga urilib 500 qaytarardi —
 * klient esa qaysi maydon aybdorligini bilmasdi.
 */
@Data
public class CastingApplicationRequest {

    /** Eski bot bilan AYNAN bir xil qiymatlar — admin sayti shularni tarjima qiladi. */
    @NotBlank(message = "Casting turi tanlanmagan")
    @Pattern(regexp = "model|euromodel|bloger|actor|extra|influencer",
            message = "Casting turi noto'g'ri: model, euromodel, bloger, actor, extra yoki influencer")
    private String castingType;

    @NotBlank(message = "Jins tanlanmagan")
    @Pattern(regexp = "male|female", message = "Jins noto'g'ri: male yoki female")
    private String gender;

    @NotBlank(message = "Ism kiritilmagan")
    @Size(max = 255, message = "Ism juda uzun")
    private String name;

    @Size(max = 255, message = "Hudud juda uzun")
    private String region;

    @Size(max = 255, message = "Millat juda uzun")
    private String nationality;

    /**
     * {@code YYYY-MM-DD}. Sana-vaqt ham qabul qilinadi
     * ({@code 2000-05-17T00:00:00}, {@code 2000-05-17T00:00:00.000Z}) —
     * faqat sana qismi olinadi.
     *
     * ⚠️ Satr sifatida olinadi, {@code LocalDate} emas: Jackson noto'g'ri
     * sanani o'qiy olmasa butun tana «o'qib bo'lmadi» (400) bo'lardi va
     * javobda qaysi maydon ekani aytilmasdi.
     */
    @NotBlank(message = "Tug'ilgan sana kiritilmagan")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}([T ].*)?$",
            message = "Tug'ilgan sana YYYY-MM-DD ko'rinishida bo'lishi kerak")
    private String birthday;

    @Min(value = 50, message = "Bo'y santimetrda: kamida 50")
    @Max(value = 250, message = "Bo'y santimetrda: ko'pi bilan 250")
    private Integer height;

    @Size(max = 255, message = "Soch rangi juda uzun")
    private String hairColor;

    @Size(max = 255, message = "Ko'z rangi juda uzun")
    private String eyeColor;

    @Size(max = 255, message = "Kiyim o'lchami juda uzun")
    private String clothSize;

    @Size(max = 255, message = "Oyoq kiyim o'lchami juda uzun")
    private String shoeSize;

    /** Ko'krak. */
    @Size(max = 255, message = "Ko'krak o'lchami juda uzun")
    private String bust;

    /** Bel. */
    @Size(max = 255, message = "Bel o'lchami juda uzun")
    private String waist;

    /** Son (bot anketasidagi nom saqlangan). */
    @Size(max = 255, message = "Son o'lchami juda uzun")
    private String son;

    @Email(message = "Email noto'g'ri")
    @Size(max = 255, message = "Email juda uzun")
    private String email;

    @NotBlank(message = "Telefon raqami kiritilmagan")
    @Size(max = 32, message = "Telefon raqami juda uzun")
    private String phone;

    @Size(max = 255, message = "Telegram juda uzun")
    private String telegram;

    @Size(max = 255, message = "Facebook juda uzun")
    private String facebook;

    @Size(max = 255, message = "Instagram juda uzun")
    private String instagram;

    /**
     * {@code POST /api/v1/file/upload} qaytargan id'lar.
     *
     * Kamida bitta: rasmsiz anketani admin baholay olmaydi. Ko'pi bilan 10 —
     * bot formasidagi chegara bilan bir xil.
     */
    @NotNull(message = "Kamida bitta rasm kerak")
    @Size(min = 1, max = 10, message = "Rasmlar soni 1 dan 10 gacha bo'lishi kerak")
    private List<@NotNull(message = "Rasm id bo'sh bo'lmasligi kerak") UUID> photos;
}
