package com.example.backend.Cms.Enums;

/**
 * Nima ko'rilyapti — qism yoki yaxlit kontent.
 *
 * <h2>Nega {@link PurchaseType} qayta ishlatilmadi</h2>
 * U yerda ham EPISODE bor, lekin ikkinchi qiymati PREMIERE — bu
 * XARID birligi, ko'rish birligi emas. Premyera sotib olinadi, lekin
 * ko'rilmaydi: ko'riladigani uning ichidagi qismlar.
 *
 * Bitta enumni ikki maqsadda ishlatish CURRENCY_PACKAGE ni ham
 * ko'rish maqsadiga ochib qo'yardi — «tanga paketining 5565-soniyasi»
 * degan ma'nosiz holat.
 */
public enum WatchTargetType {

    /** Serial yoki ko'p qismli kontentning bitta qismi. */
    EPISODE,

    /**
     * Yaxlit kontent — film, qisqa metraj, klip, shou.
     *
     * ⚠️ Faqat {@code StructureType.SINGLE} uchun. Ko'p qismli
     * kontentda ko'rish qism darajasida boradi va uni kontent
     * darajasida saqlash «qaysi qismning 5565-soniyasi?» degan
     * javobsiz savolni qoldirardi.
     */
    CONTENT
}
