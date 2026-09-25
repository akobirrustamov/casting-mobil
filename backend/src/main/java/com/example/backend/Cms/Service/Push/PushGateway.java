package com.example.backend.Cms.Service.Push;

import java.util.List;
import java.util.Map;

/**
 * Push provayderi (hozir — Expo Push Service).
 *
 * Interfeys testlar uchun: haqiqiy {@code exp.host} ga so'rov testda
 * ketmasligi kerak.
 */
public interface PushGateway {

    /** Yuborish yoqilganmi ({@code app.push.enabled}). */
    boolean isEnabled();

    /**
     * Xabarlarni yuboradi.
     *
     * @return har xabar uchun natija — AYNAN shu tartibda, shu sonda
     */
    List<PushResult> send(List<PushMessage> messages);

    /**
     * @param image rasm manzili (ochiq, to'liq URL) yoki {@code null}.
     *              Android'da bildirishnomaning o'ng tomonida kichik rasm
     *              bo'lib chiqadi, ochilganda — katta.
     */
    record PushMessage(String token, String title, String body, Map<String, Object> data,
                       String image) {
    }

    /**
     * @param ok               provayder qabul qildimi
     * @param deviceGone       token endi yaroqsiz (ilova o'chirilgan) —
     *                         bazadan tozalash kerak
     * @param error            xato matni; {@code ok} bo'lsa {@code null}
     */
    record PushResult(boolean ok, boolean deviceGone, String error) {

        public static PushResult accepted() {
            return new PushResult(true, false, null);
        }

        public static PushResult failed(String error) {
            return new PushResult(false, false, error);
        }
    }
}
