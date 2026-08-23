package com.example.backend.Cms.Service;

import com.example.backend.Cms.Enums.Locale;
import com.example.backend.exceptions.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Tarjima to'liqligi qoidalari — YAGONA joy.
 *
 * <h2>Buyurtmachi talabi</h2>
 * «Hamma ma'lumot 3 ta tilda qo'shilishi kerak.» Ya'ni foydalanuvchi
 * ko'radigan har bir sarlavha UZ, RU va EN da bo'lishi shart.
 *
 * <h2>Nega tekshiruv SAQLASHDA emas, NASHRDA</h2>
 * Ikkalasida ham majburiy qilsak, admin qoralamani ham saqlay olmasdi:
 * kontent odatda bitta tilda yoziladi, keyin tarjima qilinadi. Har bir
 * saqlashda uchala tilni talab qilish ishni to'xtatib qo'yardi va odamlar
 * «keyin to'ldiraman» deb bo'sh joyga nuqta yozib ketardi — ya'ni qoida
 * amalda buzilardi.
 *
 * Shuning uchun chegara aniq:
 * <ul>
 *   <li><b>Qoralama</b> ({@code DRAFT}, {@code IN_REVIEW}) — o'zbekchasi
 *       yetarli, qolganini keyin to'ldirish mumkin;</li>
 *   <li><b>Foydalanuvchiga ko'rinadigan holat</b> ({@code PUBLISHED},
 *       {@code SCHEDULED}, faol kategoriya/janr) — <b>uchala til ham
 *       majburiy</b>.</li>
 * </ul>
 *
 * Natijada: bazada tarjimasiz nashr qilingan kontent BO'LMAYDI, lekin
 * ish jarayoni ham to'xtamaydi.
 *
 * <h2>Nega faqat sarlavha</h2>
 * Sarlavha ro'yxatlarda va menyularda chiqadi — u yo'q bo'lsa interfeys
 * bo'sh katak ko'rsatadi. Tavsif esa kontent sahifasida va uning yo'qligi
 * sahifani buzmaydi. Tavsifni ham majburiy qilish kerak bo'lsa —
 * {@link #requireAll} chaqiruviga yana bitta maydon qo'shiladi.
 */
public final class TranslationRules {

    /** Foydalanuvchiga ko'rinadigan tillar. */
    private static final Locale[] REQUIRED = {Locale.UZ, Locale.RU, Locale.EN};

    private TranslationRules() {
    }

    /**
     * Asosiy til — qoralama uchun ham majburiy.
     *
     * @param what xato xabarida ko'rinadigan nom («Sarlavha», «Nom»…)
     */
    public static <T> void requireBase(Map<Locale, T> translations,
                                       Function<T, String> field, String what) {
        T base = translations == null ? null : translations.get(Locale.UZ);
        if (base == null || isBlank(field.apply(base))) {
            throw BusinessException.validation(
                    what + " o'zbekchada majburiy — u asosiy til");
        }
    }

    /**
     * Uchala tilda ham to'ldirilganini tekshiradi.
     *
     * Xato xabarida AYNAN qaysi til yetishmayotgani sanaladi — admin
     * «nimadir yetishmayapti» degan xabar bilan qolib ketmasin.
     */
    public static <T> void requireAll(Map<Locale, T> translations,
                                      Function<T, String> field, String what) {
        List<String> missing = new ArrayList<>();
        for (Locale locale : REQUIRED) {
            T value = translations == null ? null : translations.get(locale);
            if (value == null || isBlank(field.apply(value))) {
                missing.add(locale.name());
            }
        }
        if (!missing.isEmpty()) {
            throw BusinessException.validation(
                    what + " quyidagi tillarda to'ldirilmagan: " + String.join(", ", missing)
                            + ". Foydalanuvchiga ko'rinadigan kontent uchun uchala til ham majburiy");
        }
    }

    /**
     * Holatga qarab tanlaydi: qoralamada faqat asosiy til, nashr qilinganda
     * uchalasi ham.
     *
     * @param userVisible kontent foydalanuvchiga ko'rinadimi
     */
    public static <T> void require(Map<Locale, T> translations,
                                   Function<T, String> field,
                                   String what,
                                   boolean userVisible) {
        requireBase(translations, field, what);
        if (userVisible) {
            requireAll(translations, field, what);
        }
    }

    /**
     * Ixtiyoriy maydon uchun: <b>bittasida bo'lsa — uchalasida ham</b> bo'lsin.
     *
     * <h3>Nima uchun alohida qoida</h3>
     * Ba'zi maydonlar umuman majburiy emas. Reklama banneri faqat rasmdan
     * iborat bo'lishi mumkin — unda matn yo'q va bu normal. Tugma matni ham
     * shunday: tugma o'chirilgan bo'lsa, matn ham kerak emas.
     *
     * Lekin admin matnni FAQAT o'zbekchada yozib nashr qilsa, rus tilidagi
     * foydalanuvchi bo'sh tugmani yoki o'zi tushunmaydigan sarlavhani
     * ko'rardi. Ya'ni xato admin uchun ko'rinmas, foydalanuvchi uchun esa
     * aniq bo'lardi.
     *
     * Shuning uchun: maydon bo'sh bo'lsa — hech narsa talab qilinmaydi;
     * to'ldirila boshlagan bo'lsa — uchala tilda ham to'ldirilsin.
     *
     * @param userVisible kontent foydalanuvchiga ko'rinadimi (qoralamada tekshirilmaydi)
     */
    public static <T> void requireAllIfAny(Map<Locale, T> translations,
                                           Function<T, String> field,
                                           String what,
                                           boolean userVisible) {
        if (!userVisible || translations == null) {
            return;
        }
        boolean anyFilled = false;
        for (Locale locale : REQUIRED) {
            T value = translations.get(locale);
            if (value != null && !isBlank(field.apply(value))) {
                anyFilled = true;
                break;
            }
        }
        if (anyFilled) {
            requireAll(translations, field, what);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
