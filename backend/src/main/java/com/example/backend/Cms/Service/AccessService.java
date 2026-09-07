package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Content;
import com.example.backend.Cms.Entity.ContentMedia;
import com.example.backend.Cms.Entity.Episode;
import com.example.backend.Cms.Entity.EpisodeVideo;
import com.example.backend.Cms.Entity.MediaAsset;
import com.example.backend.Cms.Entity.Purchase;
import com.example.backend.Cms.Entity.UserAccount;
import com.example.backend.Cms.Enums.AccessPolicy;
import com.example.backend.Cms.Enums.MediaRole;
import com.example.backend.Cms.Enums.MediaType;
import com.example.backend.Cms.Enums.PurchaseType;
import com.example.backend.Cms.Enums.UserStatus;
import com.example.backend.Cms.Repository.ContentMediaRepo;
import com.example.backend.Cms.Repository.EpisodeVideoRepo;
import com.example.backend.Cms.Repository.PurchaseRepo;
import com.example.backend.Cms.Repository.UserAccountRepo;
import com.example.backend.Entity.User;
import com.example.backend.Services.PermissionService.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * «Kim nimani ko'ra oladi» — YAGONA manba (ТЗ §37).
 *
 * <h2>Nega bitta servis</h2>
 * Entitlement TO'RT manbadan kelishi mumkin: bepul kontent, faol Premium
 * obuna, bitta qism xaridi, butun premyera xaridi. Bu mantiq klientga
 * sochilsa — mobil ilova, sayt va backend uch xil javob berardi va
 * pullik kontent bepulga ochilib ketishi mumkin edi.
 *
 * Shuning uchun tekshiruv FAQAT shu yerda, backend tomonda.
 *
 * <h2>Casting loyihasi alohida</h2>
 * Buyurtmachi aniq yozgan: bir martalik xarid casting loyihasiga kirish
 * huquqini BERMAYDI, Premium esa beradi. {@link #canAccessCasting} shuni
 * amalga oshiradi.
 */
@Service
@RequiredArgsConstructor
public class AccessService {

    private final PurchaseRepo purchaseRepo;
    private final UserAccountRepo accountRepo;
    private final SettingsService settingsService;
    private final EpisodeVideoRepo episodeVideoRepo;
    private final ContentMediaRepo contentMediaRepo;
    private final PermissionService permissionService;

    /**
     * Kontent shu odam uchun umuman mavjudmi.
     *
     * Bu HUQUQ emas — KO'RINISH: nashr holati, o'chirilgani va havola
     * bo'yicha ochiqligi. PRIVATE faqat panel xodimlari uchun, havola bilan
     * ham ochilmaydi; UNLISTED havola bilan ochiladi, katalogda ko'rinmaydi.
     *
     * Ilgari bu uch shart ikki joyda so'zma-so'z takrorlanardi — biri
     * o'zgarsa, ikkinchisi jimgina eskirib qolardi.
     */
    public boolean isVisible(User user, Content content) {
        if (content == null) {
            return false;
        }
        return content.getStatus().isVisibleToUsers()
                && content.getDeletedAt() == null
                && (content.getVisibility() == null
                    || content.getVisibility().reachableByLink()
                    || (user != null && permissionService.canAccessAdminPanel(user)));
    }

    /**
     * Foydalanuvchi shu qismni ko'ra oladimi.
     *
     * @param user    tizimga kirgan foydalanuvchi, yoki {@code null} — anonim
     * @param episode tekshirilayotgan qism
     */
    @Transactional(readOnly = true)
    public AccessDecision canWatch(User user, Episode episode) {
        return canWatch(user, episode, null);
    }

    /**
     * Bir nechta qism uchun qaror — qismlar ro'yxati uchun.
     *
     * <h2>Nima uchun alohida metod</h2>
     * {@link #canWatch} ni sikl ichida chaqirish har bir qism uchun hisobni va
     * xaridlarni QAYTA so'raydi: 20 qismli mavsumda bu qirqdan ortiq so'rov.
     * Bu yerda ular BIR marta o'qiladi.
     *
     * ⚠️ Qoida NUSXALANMAYDI: qaror baribir {@link #canWatch} ning o'sha
     * tanasida qabul qilinadi. Aks holda ro'yxat va ochish sahifasi bir kuni
     * kelib har xil javob berardi — ТЗ §37 aynan shundan qochadi.
     */
    @Transactional(readOnly = true)
    public Map<Long, AccessDecision> canWatchAll(User user, List<Episode> episodes) {
        if (episodes == null || episodes.isEmpty()) {
            return Map.of();
        }

        Viewer viewer = viewerFor(user, episodes);

        Map<Long, AccessDecision> result = new LinkedHashMap<>();
        for (Episode episode : episodes) {
            result.put(episode.getId(), canWatch(user, episode, viewer));
        }
        return result;
    }

    /**
     * Oldindan o'qilgan holat: hisob va haqiqiy xaridlar.
     * {@code null} bo'lsa — bitta qism so'ralgan, hammasi joyida so'raladi.
     */
    private record Viewer(UserAccount account, Set<Long> boughtEpisodes,
                          Set<Long> boughtPremieres) {
    }

    private Viewer viewerFor(User user, List<Episode> episodes) {
        if (user == null) {
            return new Viewer(null, Set.of(), Set.of());
        }

        List<Long> episodeIds = episodes.stream().map(Episode::getId).toList();
        List<Long> contentIds = episodes.stream()
                .map(e -> e.getContent() == null ? null : e.getContent().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new Viewer(
                accountRepo.findByUserId(user.getId()).orElse(null),
                validTargets(user.getId(), PurchaseType.EPISODE, episodeIds),
                validTargets(user.getId(), PurchaseType.PREMIERE, contentIds));
    }

    /** Qaytarilmagan va muddati o'tmagan xaridlar — {@link Purchase#isValid}. */
    private Set<Long> validTargets(UUID userId, PurchaseType type, List<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Set.of();
        }
        return purchaseRepo.findAllByUserIdAndTypeAndTargetIdIn(userId, type, targetIds)
                .stream()
                .filter(Purchase::isValid)
                .map(Purchase::getTargetId)
                .collect(Collectors.toSet());
    }

    private AccessDecision canWatch(User user, Episode episode, Viewer viewer) {
        if (episode == null || episode.getContent() == null) {
            return AccessDecision.deny(AccessDecision.Reason.NOT_PUBLISHED,
                    AccessDecision.RequiredAction.NONE);
        }

        // 1. Nashr qilinmagan kontentni hech kim ko'rmaydi — hatto sotib olgan ham.
        //    (Adminlar uchun alohida oldindan ko'rish oqimi bo'ladi.)
        boolean contentLive = isVisible(user, episode.getContent());
        boolean episodeLive = episode.getStatus().isVisibleToUsers();
        if (!contentLive || !episodeLive) {
            return AccessDecision.deny(AccessDecision.Reason.NOT_PUBLISHED,
                    AccessDecision.RequiredAction.NONE);
        }

        AccessPolicy policy = episode.effectiveAccessPolicy();

        // 2. Bepul kontent — kirish talab qilinmaydi.
        if (policy == AccessPolicy.FREE) {
            return AccessDecision.allow(AccessDecision.Reason.FREE);
        }

        // 3. Pullik kontent uchun tizimga kirish shart.
        if (user == null) {
            return withPrices(
                    AccessDecision.deny(AccessDecision.Reason.NOT_AUTHENTICATED,
                            AccessDecision.RequiredAction.SIGN_IN),
                    episode);
        }

        // 4. Bloklangan foydalanuvchi hech narsani ko'rmaydi.
        UserAccount account = viewer != null
                ? viewer.account()
                : accountRepo.findByUserId(user.getId()).orElse(null);
        if (account != null && account.getStatus() == UserStatus.BLOCKED) {
            return AccessDecision.deny(AccessDecision.Reason.USER_BLOCKED,
                    AccessDecision.RequiredAction.NONE);
        }

        boolean premiumAllowed = policy == AccessPolicy.PREMIUM_ONLY
                || policy == AccessPolicy.PREMIUM_OR_PURCHASE;
        boolean purchaseAllowed = policy == AccessPolicy.PURCHASE_ONLY
                || policy == AccessPolicy.PREMIUM_OR_PURCHASE;

        // 5. Faol Premium.
        if (premiumAllowed && account != null && account.hasActivePremium()) {
            return AccessDecision.allow(AccessDecision.Reason.PREMIUM);
        }

        // 6. Xaridlar. Avval butun premyera — u kengroq huquq beradi.
        if (purchaseAllowed) {
            Long contentId = episode.getContent().getId();

            boolean premiereBought = viewer != null
                    ? viewer.boughtPremieres().contains(contentId)
                    : hasValidPurchase(user.getId(), PurchaseType.PREMIERE, contentId);
            if (premiereBought) {
                return AccessDecision.allow(AccessDecision.Reason.PREMIERE_PURCHASE);
            }

            boolean episodeBought = viewer != null
                    ? viewer.boughtEpisodes().contains(episode.getId())
                    : hasValidPurchase(user.getId(), PurchaseType.EPISODE, episode.getId());
            if (episodeBought) {
                return AccessDecision.allow(AccessDecision.Reason.EPISODE_PURCHASE);
            }
        }

        // 7. Hech qaysi manba ishlamadi — nima qilish kerakligini aytamiz.
        AccessDecision.RequiredAction action;
        if (premiumAllowed && purchaseAllowed) {
            action = AccessDecision.RequiredAction.BUY_OR_SUBSCRIBE;
        } else if (premiumAllowed) {
            action = AccessDecision.RequiredAction.SUBSCRIBE;
        } else {
            action = AccessDecision.RequiredAction.BUY_EPISODE;
        }

        return withPrices(
                AccessDecision.deny(AccessDecision.Reason.PAYMENT_REQUIRED, action),
                episode);
    }

    /**
     * Foydalanuvchi shu KONTENTNI ko'ra oladimi (SINGLE tuzilma).
     *
     * <h2>Nima uchun alohida metod kerak</h2>
     * SINGLE kontentda (film, qisqa metraj, klip) qism BO'LMAYDI — demak
     * {@code canWatch(user, episode)} ni chaqirib bo'lmaydi. Bu metod
     * qo'shilgunga qadar filmni tomosha qilish oqimi UMUMAN yo'q edi.
     *
     * <h2>Qism darajasidagidan farqi</h2>
     * Bu yerda {@code accessPolicyOverride} yo'q — u qism xossasi. Xarid
     * turi ham boshqacha: film uchun {@code PREMIERE} (butun kontent),
     * chunki sotib olinadigan narsa qism emas, kontentning o'zi.
     *
     * @param user    tizimga kirgan foydalanuvchi, yoki {@code null} — anonim
     * @param content tekshirilayotgan kontent
     */
    @Transactional(readOnly = true)
    public AccessDecision canWatch(User user, Content content) {
        if (content == null) {
            return AccessDecision.deny(AccessDecision.Reason.NOT_PUBLISHED,
                    AccessDecision.RequiredAction.NONE);
        }

        boolean live = isVisible(user, content);
        if (!live) {
            return AccessDecision.deny(AccessDecision.Reason.NOT_PUBLISHED,
                    AccessDecision.RequiredAction.NONE);
        }

        AccessPolicy policy = content.getAccessPolicy();

        if (policy == AccessPolicy.FREE) {
            return AccessDecision.allow(AccessDecision.Reason.FREE);
        }

        if (user == null) {
            return withContentPrice(
                    AccessDecision.deny(AccessDecision.Reason.NOT_AUTHENTICATED,
                            AccessDecision.RequiredAction.SIGN_IN),
                    content);
        }

        UserAccount account = accountRepo.findByUserId(user.getId()).orElse(null);
        if (account != null && account.getStatus() == UserStatus.BLOCKED) {
            return AccessDecision.deny(AccessDecision.Reason.USER_BLOCKED,
                    AccessDecision.RequiredAction.NONE);
        }

        boolean premiumAllowed = policy == AccessPolicy.PREMIUM_ONLY
                || policy == AccessPolicy.PREMIUM_OR_PURCHASE;
        boolean purchaseAllowed = policy == AccessPolicy.PURCHASE_ONLY
                || policy == AccessPolicy.PREMIUM_OR_PURCHASE;

        if (premiumAllowed && account != null && account.hasActivePremium()) {
            return AccessDecision.allow(AccessDecision.Reason.PREMIUM);
        }

        // Butun kontent xaridi. Qism xaridi bu yerda ma'noga ega emas -
        // SINGLE da qism yo'q.
        if (purchaseAllowed && hasValidPurchase(user.getId(),
                PurchaseType.PREMIERE, content.getId())) {
            return AccessDecision.allow(AccessDecision.Reason.PREMIERE_PURCHASE);
        }

        AccessDecision.RequiredAction action;
        if (premiumAllowed && purchaseAllowed) {
            action = AccessDecision.RequiredAction.BUY_OR_SUBSCRIBE;
        } else if (premiumAllowed) {
            action = AccessDecision.RequiredAction.SUBSCRIBE;
        } else {
            action = AccessDecision.RequiredAction.BUY_PREMIERE;
        }

        return withContentPrice(
                AccessDecision.deny(AccessDecision.Reason.PAYMENT_REQUIRED, action),
                content);
    }

    /**
     * Casting loyihasiga kirish huquqi.
     *
     * <b>Buyurtmachi talabi:</b> bir martalik xarid bu huquqni BERMAYDI.
     *
     * <h2>Ikki yo'l</h2>
     * <ul>
     *   <li>faol Premium — u hamma narsani ochadi, casting ham;</li>
     *   <li>casting muddati — {@code CASTING_DAYS} promokodi bergan
     *       alohida huquq. U FAQAT shu bo'limni ochadi.</li>
     * </ul>
     *
     * ⚠️ Tartib muhim emas, lekin ikkalasi ham SHU YERDA qaraladi:
     * casting huquqini biror kontroller o'zi hisoblasa, qoidaning
     * ikkinchi nusxasi paydo bo'lardi va bir kuni ular ajralib ketardi
     * ({@code PremiumRightsTest} shuni qo'riqlaydi).
     */
    @Transactional(readOnly = true)
    public boolean canAccessCasting(User user) {
        if (user == null) {
            return false;
        }
        UserAccount account = accountRepo.findByUserId(user.getId()).orElse(null);
        if (account == null || account.getStatus() == UserStatus.BLOCKED) {
            return false;
        }
        // Ataylab: xaridlar tekshirilmaydi — ular casting huquqini bermaydi.
        return account.hasActivePremium() || account.hasActiveCastingAccess();
    }

    /**
     * Casting huquqi qachongacha — KO'RSATISH uchun ({@code /app/me}).
     *
     * Premium bergan huquqda muddat Premiumniki bo'ladi: odam uchun
     * «casting qachongacha ochiq» degan savolning javobi shu.
     */
    @Transactional(readOnly = true)
    public CastingStatus castingStatus(User user) {
        if (user == null) {
            return new CastingStatus(false, null);
        }
        UserAccount account = accountRepo.findByUserId(user.getId()).orElse(null);
        if (account == null || account.getStatus() == UserStatus.BLOCKED) {
            return new CastingStatus(false, null);
        }

        java.time.LocalDateTime premium = account.getPremiumUntil();
        java.time.LocalDateTime casting = account.getCastingUntil();

        // Kechroq tugaydigani — haqiqiy muddat.
        java.time.LocalDateTime until;
        if (premium == null) {
            until = casting;
        } else if (casting == null) {
            until = premium;
        } else {
            until = premium.isAfter(casting) ? premium : casting;
        }

        return new CastingStatus(canAccessCasting(user), until);
    }

    /**
     * @param active hozir ochiqmi
     * @param until  qachongacha; {@code null} — hech qachon berilmagan
     */
    public record CastingStatus(boolean active, java.time.LocalDateTime until) {
    }

    /**
     * Premium holati — KO'RSATISH uchun ({@code /api/v1/app/me}).
     *
     * <h2>⚠️ Nega bu yerda, kontrollerda emas</h2>
     * «Obuna faolmi» degan savolga javob beradigan joy bitta bo'lishi
     * shart (ТЗ §37) va buni {@code PremiumRightsTest} qo'riqlaydi.
     * Kontroller {@code account.hasActivePremium()} ni o'zi chaqirsa,
     * qoidaning ikkinchi nusxasi paydo bo'lardi — va u vaqt o'tib
     * asl nusxadan chetga chiqib, «nega ilovada Premium ko'rinadi,
     * saytda yo'q» degan xatolarni keltirib chiqarardi.
     *
     * <h2>⚠️ Qaror SERVERDA qabul qilinadi</h2>
     * Klientga tayyor {@code active} beriladi, sana emas. Klient
     * «muddat o'tganmi» ni o'zi hisoblasa, telefon soati noto'g'ri
     * qo'yilgan qurilmada javob ham noto'g'ri bo'lardi.
     *
     * @return {@code until} — obuna qachongacha; {@code null} bo'lsa
     *         obuna umuman bo'lmagan. Muddati o'tganda sana SAQLANADI:
     *         ilova «obunangiz tugadi» deb aniq ayta oladi, «obuna
     *         yo'q» emas — bu ikki boshqa xabar va ikki boshqa tugma
     */
    @Transactional(readOnly = true)
    public PremiumStatus premiumStatus(User user) {
        if (user == null) {
            return new PremiumStatus(false, null);
        }
        return accountRepo.findByUserId(user.getId())
                .map(a -> new PremiumStatus(a.hasActivePremium(), a.getPremiumUntil()))
                .orElseGet(() -> new PremiumStatus(false, null));
    }

    /**
     * @param active hozir amal qilyaptimi
     * @param until  qachongacha; {@code null} — obuna bo'lmagan
     */
    public record PremiumStatus(boolean active, java.time.LocalDateTime until) {
    }

    /** Reklama ko'rsatilishi kerakmi: faol tarifi bo'lganlarga reklama yo'q. */
    @Transactional(readOnly = true)
    public boolean shouldShowAds(User user) {
        if (user == null) {
            return true;
        }
        return accountRepo.findByUserId(user.getId())
                .map(a -> !a.hasActivePremium())
                .orElse(true);
    }

    private boolean hasValidPurchase(UUID userId, PurchaseType type, Long targetId) {
        List<Purchase> purchases =
                purchaseRepo.findAllByUserIdAndTypeAndTargetId(userId, type, targetId);
        return purchases.stream().anyMatch(Purchase::isValid);
    }

    /**
     * Rad javobiga narxlarni qo'shadi.
     *
     * Qism narxi: avval qismning o'zinikidan, bo'lmasa sozlamadagi default.
     * Premyera narxi: kontentnikidan, bo'lmasa sozlamadagi default.
     */
    /** Kontent narxi: o'zinikidan, bo'lmasa sozlamadagi default. */
    private AccessDecision withContentPrice(AccessDecision decision, Content content) {
        decision.setPremierePrice(content.getPremierePrice() != null
                ? content.getPremierePrice()
                : settingsService.getMoney(SettingKeys.PREMIERE_PRICE));
        return decision;
    }

    private AccessDecision withPrices(AccessDecision decision, Episode episode) {
        BigDecimal episodePrice = episode.getPrice() != null
                ? episode.getPrice()
                : settingsService.getMoney(SettingKeys.EPISODE_PRICE);

        BigDecimal premierePrice = episode.getContent().getPremierePrice() != null
                ? episode.getContent().getPremierePrice()
                : settingsService.getMoney(SettingKeys.PREMIERE_PRICE);

        decision.setEpisodePrice(episodePrice);
        decision.setPremierePrice(premierePrice);
        return decision;
    }

    /**
     * Shu media faylni yuklab olish mumkinmi.
     *
     * <h2>Nega bu kerak</h2>
     * {@code /api/v1/app/media/{id}/raw} ochiq endpoint - afishalar {@code <img>}
     * tegida ko'rsatiladi va ular baribir hammaga ko'rinadi. Ammo VIDEO fayllar
     * ham xuddi shu yerdan uzatiladi. Tekshiruvsiz har kim id ni terib pullik
     * qismni yuklab olardi va {@link #canWatch} butunlay ma'nosiz bo'lardi:
     * klientga "sotib oling" deb aytardik, fayl esa yonida ochiq turardi.
     *
     * <h2>Qoida</h2>
     * <ul>
     *   <li>RASM - ochiq (afisha, avatar, banner);</li>
     *   <li>VIDEO qismga bog'langan - {@link #canWatch} qaroriga bo'ysunadi;</li>
     *   <li>VIDEO hech qaysi qismga bog'lanmagan - faqat panel xodimlari
     *       (hali biriktirilmagan yuklama, oldindan ko'rish).</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public boolean canReadMedia(User user, MediaAsset asset) {
        if (asset == null) {
            return false;
        }
        if (asset.getType() != MediaType.VIDEO) {
            return true;
        }

        // Panel xodimi kontentni tahrirlash uchun ko'rishi kerak.
        if (user != null && permissionService.canAccessAdminPanel(user)) {
            return true;
        }

        // 1. Qismga biriktirilgan video (EPISODIC / SEASONAL).
        EpisodeVideo link = episodeVideoRepo.findFirstByMediaId(asset.getId()).orElse(null);
        if (link != null && link.getEpisode() != null) {
            return canWatch(user, link.getEpisode()).isAllowed();
        }

        // 2. Kontentning ASOSIY videosi (SINGLE tuzilma).
        //
        // ⚠️ Rol bo'yicha qidiriladi: bitta fayl afisha sifatida ham
        // ishlatilishi mumkin va u ochiq. Faqat VIDEO roli entitlement
        // talab qiladi. TRAILER/TEASER esa reklama roligi - ular kontentning
        // o'zi emas va bu yerga tushmaydi.
        ContentMedia contentVideo = contentMediaRepo
                .findFirstByMediaIdAndRole(asset.getId(), MediaRole.VIDEO).orElse(null);
        if (contentVideo != null && contentVideo.getContent() != null) {
            return canWatch(user, contentVideo.getContent()).isAllowed();
        }

        // 3. Reklama roligi — treyler yoki tizer.
        //
        // ⚠️ Bu yerda entitlement TEKSHIRILMAYDI, va bu ataylab. Treyler
        // aynan SOTIB OLMAGAN odam uchun yuklanadi: xarid huquqi talab
        // qilinsa, uni faqat allaqachon to'lagan odam ko'rardi — ya'ni
        // rolik butunlay ma'nosini yo'qotardi.
        //
        // Bu chegirma emas, chegara: yuqoridagi ikki shox (qism videosi va
        // kontentning ASOSIY videosi) o'z kuchida qoladi, bu yerga faqat
        // TRAILER/TEASER roli bilan biriktirilgan fayl tushadi. Bitta fayl
        // ikkala rolda tursa, birinchi shox undan oldin javob beradi.
        //
        // ⚠️ Bitta shart baribir bor: kontent KO'RINADIGAN bo'lsin. Aks
        // holda hali nashr qilinmagan filmning treyleri id ni terib topilardi
        // — va chiqish sanasidan oldin tarqab ketardi.
        ContentMedia promo = contentMediaRepo.findFirstByMediaIdAndRoleIn(
                asset.getId(), List.of(MediaRole.TRAILER, MediaRole.TEASER)).orElse(null);
        if (promo != null && promo.getContent() != null) {
            return isVisible(user, promo.getContent());
        }

        // Biriktirilmagan video - ommaga tegishli emas.
        return false;
    }
}
