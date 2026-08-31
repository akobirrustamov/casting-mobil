package com.example.backend.Cms.Enums;

/**
 * Nima sevimliga qo'shilgan.
 *
 * <h2>⚠️ Nega tur bor — hozir bittasi ishlatilsa ham</h2>
 * Mobil ilova bugun faqat casting ijodkorlarini saqlaydi. Lekin bu
 * VIDEO platformasi: «saqlangan filmlar» ertami-kechmi kerak bo'ladi.
 *
 * Tursiz jadval yasalsa, o'sha kuni migratsiya, entity va endpointni
 * qaytadan yozish kerak bo'lardi — va eski qatorlarga qaysi tur
 * berishni taxmin qilishga to'g'ri kelardi.
 *
 * {@code Purchase} da ham aynan shu naqsh: {@code type} + {@code targetId}.
 */
public enum FavoriteType {

    /** Casting ijodkori — {@code CastingUser.id} (eski modul). */
    CREATOR,

    /** Video kontent — {@code Content.id}. Hali klientda ishlatilmaydi. */
    CONTENT
}
