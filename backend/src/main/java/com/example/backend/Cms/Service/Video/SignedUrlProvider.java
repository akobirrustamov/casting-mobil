package com.example.backend.Cms.Service.Video;

/**
 * Segment uchun qisqa muddatli imzolangan havola.
 *
 * <h2>⚠️ Nega interfeys</h2>
 * Ikkita yechim mumkin va tanlov Timeweb CDN imkoniyatiga bog'liq:
 *
 * <ul>
 *   <li><b>Presigned S3</b> — hozir amalga oshirilgan. Har qanday
 *       S3-mos ombor bilan ishlaydi, hech qanday tasdiq kutmaydi;</li>
 *   <li><b>CDN secure token</b> — Timeweb qo'llab-quvvatlasa. Yaxshiroq:
 *       segment CDN'dan keladi va keshlash to'liq ishlaydi.</li>
 * </ul>
 *
 * Ikkalasining ham vazifasi bir xil: kalitdan cheklangan muddatli
 * havola yasash. Shuning uchun qaror shu interfeys ortida turadi va
 * uni almashtirish playlist mantig'iga tegmaydi.
 */
public interface SignedUrlProvider {

    /**
     * Kalitdan imzolangan havola yasaydi.
     *
     * ⚠️ Bir xil kalit uchun QAYTA-QAYTA bir xil satr qaytishi
     * kerak — hech bo'lmaganda qisqa oyna ichida.
     *
     * Sabab: har foydalanuvchi boshqa havola olsa, CDN uni boshqa
     * manzil deb hisoblaydi va har biri uchun alohida kesh yozuvi
     * yasaydi. 3000 kishi bitta filmni ko'rsa, kesh umuman ishlamaydi
     * va butun trafik omborga tushadi.
     *
     * @param storageKey {@code /videos/7/hls/720p/segment_00001.m4s}
     * @return mutlaq havola
     */
    String sign(String storageKey);

    /** Sozlangan va ishlashga tayyormi. */
    boolean isAvailable();
}
