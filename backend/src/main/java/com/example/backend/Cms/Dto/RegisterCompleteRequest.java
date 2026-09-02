package com.example.backend.Cms.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Ro'yxatdan o'tishning 3-qadami: ism, parol va uning takrori.
 *
 * <h2>Nega takror serverda ham tekshiriladi</h2>
 * Ikkita maydonni ilova solishtiradi, lekin klientdagi xato odamga
 * O'ZI BILMAYDIGAN parol qo'yib qo'yardi — va «parolni unutdim» hali
 * ishlamaydi. Tekshiruv arzon, xatosi esa qaytmas.
 *
 * {@code passwordConfirm} ixtiyoriy: eski klient uni yubormasa,
 * solishtiruv o'tkazib yuboriladi.
 */
@Data
public class RegisterCompleteRequest {

    @NotBlank(message = "Telefon raqami kiritilmagan")
    private String phone;

    /**
     * Ism-familiya.
     *
     * <h2>Nega aynan shu qadamda</h2>
     * Ilgari ism umuman so'ralmasdi va SMS orqali ochilgan hisob
     * NOMSIZ qolardi: profilda ham, izohlar ostida ham bo'shliq
     * turardi. Buyurtmachi (01.09.2026) uni ro'yxatdan o'tishga
     * qo'shishni so'radi.
     *
     * Telefon va kod qadamlariga qo'shilmadi: u yerda har qo'shimcha
     * maydon SMS'gacha bo'lgan yo'lni uzaytiradi. Bu qadamda esa odam
     * baribir ikkita parol maydonini to'ldiryapti.
     */
    @NotBlank(message = "Ism kiritilmagan")
    private String name;

    @NotBlank(message = "Parol kiritilmagan")
    private String password;

    private String passwordConfirm;
}
