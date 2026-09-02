package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.WatchProgress;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Enums.WatchTargetType;
import com.example.backend.Cms.Repository.ContentRepo;
import com.example.backend.Cms.Repository.EpisodeRepo;
import com.example.backend.Cms.Repository.WatchProgressRepo;
import com.example.backend.Entity.User;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * «Ko'rishda davom eting» — qayerda to'xtaganini eslab qolish.
 *
 * <h2>Nega bu serverda, faqat telefonda emas</h2>
 * Telefondagi xotira ilova o'chirilganda yo'qoladi, ikkinchi
 * qurilmada bo'sh bo'ladi va web'da umuman ko'rinmaydi. Sevimlilar
 * bilan aynan shu bo'lgan edi (V29).
 *
 * Klient ikkalasini ham yozadi: telefondagi nusxa DARHOL o'qiladi
 * (video ochilishi kutmaydi), server esa haqiqat manbai bo'lib
 * qoladi.
 */
@Service
@RequiredArgsConstructor
public class WatchProgressService {

    /**
     * Shu ulushdan keyin video «ko'rilgan» hisoblanadi.
     *
     * ⚠️ 100% kutib bo'lmaydi: odam titrlarni oxirigacha ko'rmaydi,
     * pleyer esa oxirgi soniyalarni ko'pincha umuman bermaydi.
     * Kutilsa, tugatilgan film «davom eting» ro'yxatida ABADIY
     * osilib qolardi.
     */
    private static final double COMPLETED_RATIO = 0.95;

    /**
     * Shundan kam ko'rilgan video ro'yxatga TUSHMAYDI.
     *
     * Odam videoni ochib darhol yopishi mumkin — noto'g'ri bosdi,
     * tavsifni o'qidi, fikridan qaytdi. Bunday yozuvlar ro'yxatni
     * tasodifan ochilgan videolar bilan to'ldirardi.
     */
    private static final int MIN_CONTINUE_SECONDS = 15;

    /** Ro'yxat uzunligi — bosh sahifadagi bitta gorizontal lenta. */
    private static final int CONTINUE_LIMIT = 20;

    /**
     * Qabul qilinadigan sifat tanlovlari.
     *
     * ⚠️ Ro'yxat YOPIQ. Klient nima yuborsa o'shani saqlash bazaga
     * cheklanmagan matn kiritish imkonini berardi, keyin esa
     * «{@code 2160p}» kabi mavjud bo'lmagan sifat qaytib kelib
     * pleyerni chalg'itardi. Transkodlash profillari aynan shu
     * uchtasi.
     */
    private static final Set<String> ALLOWED_QUALITY =
            Set.of("auto", "1080p", "720p", "480p");

    private final WatchProgressRepo repo;
    private final EpisodeRepo episodeRepo;
    private final ContentRepo contentRepo;
    private final HomeFeedService homeFeedService;

    /**
     * Joriy holatni saqlaydi — bor bo'lsa ustiga yozadi.
     *
     * <h2>⚠️ Orqaga surish TAQIQLANMAGAN</h2>
     * Yangi pozitsiya eskisidan kichik bo'lishi mumkin va bu normal:
     * odam orqaga qaytardi. «Faqat oldinga» qoidasi qo'yilsa,
     * boshiga qaytib ko'rish saqlanmasdi.
     *
     * @param durationSeconds pleyerdagi HAQIQIY davomiylik; qismdagi
     *                        ma'lumot admin qo'lda kiritgani va u
     *                        noto'g'ri bo'lishi mumkin
     * @param quality         odam QO'LDA tanlagani yoki {@code auto}
     */
    @Transactional
    public WatchProgress save(User user,
                              WatchTargetType type,
                              Long targetId,
                              Integer positionSeconds,
                              Integer durationSeconds,
                              String quality) {

        if (type == null) {
            throw BusinessException.validation("Ko'rish turi ko'rsatilmagan");
        }
        if (targetId == null) {
            throw BusinessException.validation("Video identifikatori ko'rsatilmagan");
        }
        if (positionSeconds == null || positionSeconds < 0) {
            throw BusinessException.validation("Pozitsiya noto'g'ri");
        }

        Integer duration = durationSeconds != null && durationSeconds > 0
                ? durationSeconds
                : null;

        // ⚠️ Davomiylikdan oshib ketgan pozitsiya KESILADI.
        //
        // Pleyer ba'zan davomiylikdan biroz katta qiymat beradi
        // (oxirgi segment to'liq emas). Kesilmasa foiz 100 dan oshib
        // ketardi va progress chizig'i tashqariga chiqib ketardi.
        int position = duration != null && positionSeconds > duration
                ? duration
                : positionSeconds;

        WatchProgress progress = repo
                .findByUserIdAndTypeAndTargetId(user.getId(), type, targetId)
                .orElseGet(() -> WatchProgress.builder()
                        .user(user)
                        .type(type)
                        .targetId(targetId)
                        .build());

        progress.setPositionSeconds(position);
        progress.setDurationSeconds(duration);
        progress.setQuality(normalizeQuality(quality));
        progress.setCompleted(isCompleted(position, duration));
        progress.setUpdatedAt(LocalDateTime.now());

        return repo.save(progress);
    }

    /** Bitta video uchun saqlangan holat — ochilishda o'qiladi. */
    @Transactional(readOnly = true)
    public Optional<WatchProgress> find(User user, WatchTargetType type, Long targetId) {
        return repo.findByUserIdAndTypeAndTargetId(user.getId(), type, targetId);
    }

    /** «Ko'rishda davom eting» lentasi — faqat raqamlar. */
    @Transactional(readOnly = true)
    public List<WatchProgress> continueWatching(User user) {
        return repo.findContinueWatching(
                user.getId(), MIN_CONTINUE_SECONDS, PageRequest.of(0, CONTINUE_LIMIT));
    }

    /**
     * Lenta uchun bitta element: qayerda to'xtagani + kartochka.
     *
     * @param episodeNumber qism raqami; yaxlit kontentda {@code null}
     */
    public record ContinueItem(WatchProgress progress,
                               Content content,
                               Integer episodeNumber) {
    }

    /**
     * «Ko'rishda davom eting» lentasi — chizishga tayyor holda.
     *
     * <h2>⚠️ Nega kartochka SERVERDA yig'iladi</h2>
     * Progress jadvalida faqat raqamlar bor: qaysi video va nechanchi
     * soniya. Lentaga esa afisha va sarlavha kerak. Klient ularni o'zi
     * so'rasa, yigirma element uchun yigirma so'rov ketardi — bosh
     * sahifa ochilishida.
     *
     * <h2>⚠️ Nega {@link HomeFeedService#contentCard} qayta ishlatiladi</h2>
     * Lenta bosh sahifadagi boshqa qatorlar yonida turadi. Kartochka
     * boshqa joyda yig'ilsa, u yerdagi har o'zgarish (yangi maydon,
     * boshqacha afisha tanlash) bu lentani chetda qoldirardi va u
     * asta-sekin qolganlaridan farq qila boshlardi.
     *
     * <h2>⚠️ Nashrdan olingan kontent CHIQARIB tashlanadi</h2>
     * Odam ko'rgan film keyin yopilgan bo'lishi mumkin. Uni lentada
     * qoldirish «bosdim — ochilmadi» degan holatga olib kelardi.
     */
    @Transactional(readOnly = true)
    public List<ContinueItem> continueWatchingCards(User user, Locale lang) {
        List<WatchProgress> rows = continueWatching(user);
        if (rows.isEmpty()) {
            return List.of();
        }

        // ⚠️ Ikkita to'plamli so'rov, har satr uchun bittadan emas.
        // Yigirma element uchun bu 2 ta so'rov o'rniga 40 ta bo'lardi.
        Map<Long, Episode> episodes = load(rows, WatchTargetType.EPISODE,
                ids -> episodeRepo.findAllById(ids), Episode::getId);
        Map<Long, Content> contents = load(rows, WatchTargetType.CONTENT,
                ids -> contentRepo.findAllById(ids), Content::getId);

        List<ContinueItem> items = new ArrayList<>();
        for (WatchProgress row : rows) {
            Content content;
            Integer episodeNumber = null;

            if (row.getType() == WatchTargetType.EPISODE) {
                Episode episode = episodes.get(row.getTargetId());
                // Qism o'chirilgan — satr osilib qolgan. Chet el kaliti
                // ataylab yo'q (turga qarab ikki jadval), shuning uchun
                // bunday satr bo'lishi MUMKIN.
                if (episode == null) continue;
                content = episode.getContent();
                episodeNumber = episode.getEpisodeNumber();
            } else {
                content = contents.get(row.getTargetId());
            }

            if (!homeFeedService.isVisible(content, user)) {
                continue;
            }
            items.add(new ContinueItem(row, content, episodeNumber));
        }
        return items;
    }

    /** Bitta turdagi barcha maqsadlarni bitta so'rov bilan yuklaydi. */
    private <T> Map<Long, T> load(List<WatchProgress> rows,
                                  WatchTargetType type,
                                  Function<List<Long>, List<T>> fetch,
                                  Function<T, Long> id) {
        List<Long> ids = rows.stream()
                .filter(r -> r.getType() == type)
                .map(WatchProgress::getTargetId)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }
        return fetch.apply(ids).stream()
                .collect(Collectors.toMap(id, Function.identity(), (a, b) -> a, HashMap::new));
    }

    /**
     * Videoni ro'yxatdan olib tashlaydi — «ro'yxatdan o'chirish».
     *
     * ⚠️ Yo'q yozuv uchun ham xatosiz tugaydi: klient so'rovni
     * takrorlagan bo'lishi mumkin va natija bir xil.
     */
    @Transactional
    public void forget(User user, WatchTargetType type, Long targetId) {
        repo.findByUserIdAndTypeAndTargetId(user.getId(), type, targetId)
                .ifPresent(repo::delete);
    }

    /**
     * Tugallangan-tugallanmaganini hisoblaydi.
     *
     * ⚠️ Davomiylik noma'lum bo'lsa HECH QACHON tugallangan
     * hisoblanmaydi. Aks holda transkodlash tugamagan video
     * birinchi so'rovdayoq «ko'rilgan» bo'lib ro'yxatdan tushib
     * ketardi.
     */
    private boolean isCompleted(int position, Integer duration) {
        return duration != null && position >= duration * COMPLETED_RATIO;
    }

    /**
     * Sifat tanlovini tekshiradi.
     *
     * Noma'lum qiymat rad ETILMAYDI, {@code null} ga aylantiriladi:
     * sifat — ikkinchi darajali ma'lumot va uning ustidan butun
     * saqlashni yiqitish pozitsiyani ham yo'qotardi. Pozitsiya esa
     * bu yerdagi asosiy qiymat.
     */
    private String normalizeQuality(String quality) {
        if (quality == null) {
            return null;
        }
        String value = quality.trim().toLowerCase();
        return ALLOWED_QUALITY.contains(value) ? value : null;
    }
}
