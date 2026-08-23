package com.example.backend.Security.Bootstrap;

import java.util.Set;

/**
 * Ishga tushirishda yaratiladigan master hisoblar uchun parol qoidasi.
 *
 * <h2>Nima uchun alohida klass</h2>
 * Bu qoida {@code AutoRun} ichida yozilgan bo'lsa, uni test qilib bo'lmasdi:
 * u faqat ilova ko'tarilganda ishlaydi. Alohida chiqarilgani uchun
 * {@code BootstrapPasswordPolicyTest} har bir chekka holatni tekshiradi.
 *
 * <h2>Qanday muammoni yopadi</h2>
 * Ilgari master hisoblar KODDAGI standart parol bilan yaratilardi:
 * {@code gipersuperadmin / 00000000} — platformadagi eng yuqori rol.
 * Parol manba kodda turgani uchun uni har kim bilardi va HYPER_ADMIN
 * sifatida kirib, istalgan hisob yaratishi mumkin edi.
 *
 * Endi bunday parol RAD ETILADI va hisob umuman yaratilmaydi.
 */
public final class BootstrapPasswordPolicy {

    /** Eng kam uzunlik — ТЗ R15 bilan bir xil. */
    private static final int MIN_LENGTH = 8;

    /**
     * Ochiq-oydin zaif parollar.
     *
     * Uzunlik va tarkib tekshiruvidan o'tib ketadigan, lekin amalda
     * hammaga ma'lum bo'lgan variantlar.
     */
    private static final Set<String> KNOWN_WEAK = Set.of(
            "00000000", "11111111", "12345678", "123456789",
            "password", "password1", "parol123", "qwerty123",
            "admin123", "adminadmin");

    private BootstrapPasswordPolicy() {
    }

    /**
     * Shu parol bilan master hisob yaratish mumkinmi.
     *
     * Talab: kamida {@value #MIN_LENGTH} belgi, kamida bitta harf va bitta
     * raqam, va ma'lum zaif parollar ro'yxatida bo'lmasligi.
     */
    public static boolean isAcceptable(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        if (KNOWN_WEAK.contains(password.toLowerCase())) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    /** Log uchun sabab — parolning O'ZI hech qachon yozilmaydi. */
    public static String rejectionReason(String password) {
        if (password == null || password.isBlank()) {
            return "parol berilmagan";
        }
        if (password.length() < MIN_LENGTH) {
            return "parol " + MIN_LENGTH + " belgidan qisqa";
        }
        if (KNOWN_WEAK.contains(password.toLowerCase())) {
            return "parol ma'lum zaif parollar ro'yxatida";
        }
        return "parolda harf va raqam bo'lishi kerak";
    }
}
