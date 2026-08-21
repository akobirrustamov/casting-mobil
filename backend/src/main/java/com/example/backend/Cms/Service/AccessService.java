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
import java.util.List;
import java.util.UUID;

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
     * Foydalanuvchi shu qismni ko'ra oladimi.
     *
     * @param user    tizimga kirgan foydalanuvchi, yoki {@code null} — anonim
     * @param episode tekshirilayotgan qism
     */
    @Transactional(readOnly = true)
    public AccessDecision canWatch(User user, Episode episode) {
        if (episode == null || episode.getContent() == null) {
            return AccessDecision.deny(AccessDecision.Reason.NOT_PUBLISHED,
                    AccessDecision.RequiredAction.NONE);
        }

        // 1. Nashr qilinmagan kontentni hech kim ko'rmaydi — hatto sotib olgan ham.
        //    (Adminlar uchun alohida oldindan ko'rish oqimi bo'ladi.)
        boolean contentLive = episode.getContent().getStatus().isVisibleToUsers()
                && episode.getContent().getDeletedAt() == null
                // Kontent PRIVATE bo'lsa uning qismlari ham yopiq.
                && (episode.getContent().getVisibility() == null
                    || episode.getContent().getVisibility().reachableByLink()
                    || (user != null && permissionService.canAccessAdminPanel(user)));
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
        UserAccount account = accountRepo.findByUserId(user.getId()).orElse(null);
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
            if (hasValidPurchase(user.getId(), PurchaseType.PREMIERE,
                    episode.getContent().getId())) {
                return AccessDecision.allow(AccessDecision.Reason.PREMIERE_PURCHASE);
            }
            if (hasValidPurchase(user.getId(), PurchaseType.EPISODE, episode.getId())) {
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

        boolean live = content.getStatus().isVisibleToUsers()
                && content.getDeletedAt() == null
                // PRIVATE — faqat panel xodimlari uchun. Havola bilan ham
                // ochilmaydi: u tayyorlanayotgan kontentni tekshirish uchun.
                // UNLISTED esa havola bilan OCHILADI, faqat katalogda yo'q.
                && (content.getVisibility() == null
                    || content.getVisibility().reachableByLink()
                    || (user != null && permissionService.canAccessAdminPanel(user)));
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
     * <b>Buyurtmachi talabi:</b> bir martalik xarid bu huquqni BERMAYDI,
     * faqat faol Premium obuna beradi.
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
        return account.hasActivePremium();
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

        // Biriktirilmagan video - ommaga tegishli emas.
        return false;
    }
}
