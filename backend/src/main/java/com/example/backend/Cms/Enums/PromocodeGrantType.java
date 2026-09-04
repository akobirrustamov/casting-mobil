package com.example.backend.Cms.Enums;

/**
 * Promokod NIMA beradi — admin kod yaratayotganda tanlaydi.
 *
 * <h2>Nima uchun tur kerak bo'ldi</h2>
 * Buyurtmachi (04.09.2026): «promokodlar adminka tomonidan yaratiladi,
 * nima uchun yaratilsa o'shanga ulanib ketaveradigan qilish kerak».
 * Ya'ni kodning maqsadi yaratilish paytida belgilanadi va berilgan huquq
 * aynan o'shanga bog'lanadi — «bir xil kod, har xil talqin» bo'lmaydi.
 *
 * <h2>Nima uchun ikkitasi ham «kunlar»</h2>
 * Ikkalasi ham MUDDAT beradi, miqdor emas. Shuning uchun ikkalasida ham
 * bitta {@code grantDays} maydoni ishlaydi va ikkalasi ham mavjud muddat
 * USTIGA qo'shiladi. Kelajakda «Yulduz qo'shish» kabi boshqa turdagi
 * sovg'a kerak bo'lsa, u alohida maydon talab qiladi — o'shanda bu enum
 * kengayadi va shart shu yerda ochiq turadi.
 */
public enum PromocodeGrantType {

    /**
     * N kun Premium.
     *
     * ⚠️ Bu casting bo'limini HAM ochadi: {@code AccessService} da
     * casting huquqi «Premium yoki casting muddati» deb yozilgan.
     * Ya'ni Premium kodi casting kodini ham qamrab oladi.
     */
    PREMIUM_DAYS,

    /**
     * N kun FAQAT Casting bo'limiga kirish.
     *
     * Film va seriallar ochilmaydi. Buyurtmachi aynan shu farqni so'radi:
     * «casting bo'limiga bepul kirish kunlari».
     *
     * ⚠️ Bugun bu huquq hech qayerda TEKSHIRILMAYDI —
     * {@code canAccessCasting()} ni main kodda hech kim chaqirmaydi,
     * chunki casting e'lonlari moduli hali yozilmagan (rejadagi
     * 9-bosqich). Huquq beriladi va saqlanadi; u modul paydo bo'lishi
     * bilan kuchga kiradi.
     */
    CASTING_DAYS
}
