package com.example.backend.Security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google ID token'ni tekshiradi: imzo, muddat, issuer va audience.
 *
 * Audience — bizning OAuth client ID'larimiz. Mobil ilova qaysi platformadan
 * kirishiga qarab har xil client ID bilan token oladi, shuning uchun
 * hammasi ro'yxatga qo'shiladi (web, android, ios).
 *
 * Sozlash (application.properties yoki environment):
 *   app.google.client-ids=xxx.apps.googleusercontent.com,yyy.apps.googleusercontent.com
 *   yoki APP_GOOGLE_CLIENT_IDS
 */
@Component
public class GoogleTokenVerifier {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    @Value("${app.google.client-ids:}")
    private String clientIdsRaw;

    private GoogleIdTokenVerifier verifier;
    private List<String> clientIds;

    @PostConstruct
    void init() {
        clientIds = Arrays.stream(clientIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (clientIds.isEmpty()) {
            // Sozlanmagan bo'lsa verifier yaratilmaydi - login aniq xato qaytaradi,
            // jim turib har qanday token'ni qabul qilib yubormaydi.
            return;
        }

        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(clientIds)
                .setIssuers(List.of(GOOGLE_ISSUER, "accounts.google.com"))
                .build();
    }

    public boolean isConfigured() {
        return verifier != null;
    }

    /**
     * @return tekshirilgan token payload'i
     * @throws IllegalStateException sozlanmagan bo'lsa
     * @throws IllegalArgumentException token yaroqsiz bo'lsa
     */
    public GoogleIdToken.Payload verify(String idToken) {
        if (verifier == null) {
            throw new IllegalStateException(
                    "app.google.client-ids sozlanmagan - Google login ishlamaydi");
        }
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("idToken bo'sh");
        }

        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("Google token tekshirilmadi: " + e.getMessage(), e);
        }

        if (token == null) {
            throw new IllegalArgumentException("Google token yaroqsiz yoki muddati o'tgan");
        }

        GoogleIdToken.Payload payload = token.getPayload();

        // Tasdiqlanmagan pochta bilan akkaunt ochib bo'lmaydi:
        // aks holda birovning emailini o'zlashtirish mumkin bo'lardi.
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new IllegalArgumentException("Google pochtasi tasdiqlanmagan");
        }

        return payload;
    }
}
