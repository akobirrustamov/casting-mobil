package com.example.backend.Cms.Service;

import com.example.backend.exceptions.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Odam ismi — tozalash va tekshirish qoidasi.
 *
 * <h2>Nima uchun alohida sinf</h2>
 * Qoida {@code AppAccountService} ichida yopiq metod edi va u yerda
 * to'g'ri ishlardi: kirish oqimida ism bir marta so'raladi. Profilni
 * tahrirlash paydo bo'lganda aynan shu qoida ikkinchi marta kerak
 * bo'ldi.
 *
 * Nusxalash arzon ko'rinadi, lekin ikki nusxa bir kun ajralib ketardi:
 * masalan kirishda 60 belgi, profilda 100 — va odam profilda qo'ygan
 * ismini keyingi kirishda «juda uzun» degan xato bilan ko'rardi.
 *
 * <h2>Format qoidasi yo'q — ataylab</h2>
 * Faqat uzunlik tekshiriladi. Dunyoda ism qanday yozilishi haqidagi har
 * qanday qoida kimningdir haqiqiy ismini rad etadi: apostrof, chiziqcha,
 * bitta so'z, to'rtta so'z, lotin bo'lmagan yozuv — hammasi haqiqiy.
 */
public final class PersonName {

    /**
     * Pastki chegara bitta harfli «ism» dan himoya qiladi, yuqorigisi —
     * profil va izohlar ostida sig'maydigan matndan.
     */
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 60;

    private PersonName() {
    }

    /**
     * @return tozalangan ism — chetdagi bo'shliqlarsiz, ichkarida bitta
     *         bo'shliq bilan
     * @throws BusinessException {@code NAME_INVALID} (422)
     */
    public static String validate(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");

        if (name.length() < MIN_LENGTH || name.length() > MAX_LENGTH) {
            throw new BusinessException("NAME_INVALID",
                    "Ismni to'liq kiriting (" + MIN_LENGTH + "-" + MAX_LENGTH + " ta belgi)",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return name;
    }
}
