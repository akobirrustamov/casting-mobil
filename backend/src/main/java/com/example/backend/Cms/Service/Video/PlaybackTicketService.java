package com.example.backend.Cms.Service.Video;

import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * HLS playlist so'rovi uchun chipta.
 *
 * <h2>⚠️ Nega sarlavha emas, MANZIL ichida</h2>
 * Pleyer playlistni o'zi so'raydi va segmentlarni ham o'zi tortadi.
 * ExoPlayer va AVPlayer'da sarlavhalar butun oqim uchun BIR MARTA
 * beriladi — ya'ni ular segment so'roviga ham qo'shiladi.
 *
 * Segmentlar esa imzolangan havola bilan to'g'ridan-to'g'ri ombordan
 * keladi. S3 ikkita avtorizatsiyani birga qabul qilmaydi: so'rovda
 * ham {@code Authorization}, ham {@code X-Amz-Signature} bo'lsa u
 * 400 qaytaradi. Ya'ni {@code Authorization} yuborilsa video
 * OCHILMASDI.
 *
 * Shuning uchun HLS oqimida sarlavha UMUMAN ishlatilmaydi: chipta
 * manzil ichida keladi.
 *
 * <h2>⚠️ Chipta HUQUQ bermaydi</h2>
 * U faqat «bu so'rov kimniki» degan savolga javob beradi. Ko'rish
 * huquqi har so'rovda {@link com.example.backend.Cms.Service.AccessService}
 * dan qayta so'raladi.
 *
 * Bu ataylab: obuna tugasa yoki xarid qaytarilsa, kirish O'SHA ZAHOTI
 * yopiladi. Chipta ichiga «ruxsat berilgan» deb yozilganda esa u
 * muddati tugagunicha ishlayverardi — pulini qaytarib olgan odam
 * filmni ko'rishda davom etardi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaybackTicketService {

    /** Imzo kaliti shu yerdan — alohida sozlama qo'shilmaydi. */
    private final JwtService jwtService;

    private final UserRepo userRepo;

    /**
     * Chipta qancha yashaydi.
     *
     * ⚠️ Tomosha seansidan uzun bo'lishi kerak: eng uzun film ham
     * to'rt soatdan oshmaydi. Qisqa bo'lsa, odam filmning o'rtasida
     * «playlist ochilmadi» degan xatoga tushardi.
     *
     * Uzun bo'lishi ham xavfli emas — huquq baribir har so'rovda
     * qaytadan tekshiriladi.
     */
    @Value("${app.video.ticket-ttl:6h}")
    private Duration ttl;

    /** Chipta ekanini bildiruvchi belgi — kirish tokeni bilan almashmaydi. */
    private static final String TYPE_PLAYBACK = "playback";

    /** Qaysi media uchun. */
    private static final String CLAIM_MEDIA = "mid";

    /** Anonim tomoshabin — bepul kontent uchun. */
    private static final String ANONYMOUS = "anon";

    /**
     * Shu media uchun chipta yasaydi.
     *
     * @param user  tomoshabin yoki {@code null} — anonim
     * @param mediaId qaysi media
     */
    public String issue(User user, Long mediaId) {
        return Jwts.builder()
                .setSubject(user == null ? ANONYMOUS : user.getId().toString())
                .claim(CLAIM_MEDIA, mediaId)
                .claim(JwtService.CLAIM_TYPE, TYPE_PLAYBACK)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .signWith(jwtService.getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Chiptani o'qib, egasini qaytaradi.
     *
     * @return chipta egasi; anonim bo'lsa {@code null}
     * @throws IllegalArgumentException chipta yaroqsiz, muddati o'tgan
     *         yoki BOSHQA media uchun
     */
    public User holderOf(String ticket, Long mediaId) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(jwtService.getSigningKey())
                    .build()
                    .parseClaimsJws(ticket)
                    .getBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("Chipta yaroqsiz", e);
        }

        // ⚠️ Kirish tokeni chipta o'rniga ishlatilmasin. Ikkalasi ham
        // bir xil kalit bilan imzolangan, ya'ni imzo tekshiruvidan
        // ikkalasi ham o'tadi — farqni FAQAT shu belgi qiladi.
        //
        // Busiz o'g'irlangan chipta oddiy kirish tokeni sifatida
        // ishlardi: butun hisobga kirish huquqi.
        if (!TYPE_PLAYBACK.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
            throw new IllegalArgumentException("Chipta yaroqsiz");
        }

        // ⚠️ Chipta AYNAN shu media uchun. Tekshirilmasa, bepul
        // klipning chiptasi bilan pullik filmni ochish mumkin bo'lardi.
        Number media = claims.get(CLAIM_MEDIA, Number.class);
        if (media == null || !mediaId.equals(media.longValue())) {
            throw new IllegalArgumentException("Chipta boshqa media uchun");
        }

        String subject = claims.getSubject();
        if (subject == null || ANONYMOUS.equals(subject)) {
            return null;
        }
        return userRepo.findById(UUID.fromString(subject)).orElse(null);
    }
}
