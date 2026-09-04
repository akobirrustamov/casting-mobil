package com.example.backend.Cms.Service;

import com.example.backend.Cms.Entity.Promocode;
import com.example.backend.Cms.Entity.PromocodeRedemption;
import com.example.backend.Cms.Entity.Subscription;
import com.example.backend.Cms.Enums.PromocodeGrantType;
import com.example.backend.Cms.Enums.SubscriptionSource;
import com.example.backend.Cms.Repository.PromocodeRedemptionRepo;
import com.example.backend.Cms.Repository.PromocodeRepo;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Promokodlar — yaratish (admin) va ishlatish (foydalanuvchi).
 *
 * <h2>Nima beradi — admin tanlaydi</h2>
 * Buyurtmachi (04.09.2026): «promokodlar adminka tomonidan yaratiladi,
 * nima uchun yaratilsa o'shanga ulanib ketaveradigan qilish kerak».
 * Shuning uchun kodda {@code grantType} bor:
 *
 * <ul>
 *   <li>{@code PREMIUM_DAYS} — N kun Premium (u casting'ni ham ochadi);</li>
 *   <li>{@code CASTING_DAYS} — N kun FAQAT Casting bo'limi.</li>
 * </ul>
 *
 * Ikkalasida ham kunlar mavjud muddat USTIGA qo'shiladi. Qo'shish
 * arifmetikasi {@link PremiumGrantService} da — admin sovg'asi bilan
 * bitta qoida.
 *
 * <h2>Rad etish sabablari ANIQ</h2>
 * Har holat o'z kodi bilan: topilmadi, muddati o'tgan, allaqachon
 * ishlatilgan, o'rin qolmagan. Bitta umumiy «kod yaroqsiz» odamga hech
 * narsa aytmasdi — u kodni qayta-qayta terardi.
 *
 * <h2>Poyga holati</h2>
 * Ikki himoya, ikkalasi bazada:
 * <ul>
 *   <li>bitta odam ikki marta — {@code uk_promocode_user};</li>
 *   <li>umumiy limit — {@code lockByCode}: qator qulflanadi, sanash
 *       qulf ostida bo'ladi.</li>
 * </ul>
 * Xizmat darajasidagi «avval tekshir, keyin yoz» ikkalasida ham
 * ishlamasdi.
 */
@Service
@RequiredArgsConstructor
public class PromocodeService {

    /** Ruxsat etilgan belgilar — chalkash 0/O, 1/I lar generatsiyada yo'q. */
    private static final Pattern CODE_SHAPE = Pattern.compile("[A-Z0-9][A-Z0-9-]{2,31}");
    private static final String GENERATE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int GENERATED_LENGTH = 8;
    private static final int MAX_DAYS = 3650;

    private final PromocodeRepo promocodeRepo;
    private final PromocodeRedemptionRepo redemptionRepo;
    private final PremiumGrantService premiumGrantService;
    private final AuditService auditService;

    private final SecureRandom random = new SecureRandom();

    // ------------------------------------------------------------ ishlatish

    /**
     * Kodni ishlatadi.
     *
     * @return nima berildi — ilova «30 kun qo'shildi, 04.10 gacha» deb aytadi
     */
    @Transactional
    public Redemption redeem(User user, String rawCode) {
        String code = normalize(rawCode);
        LocalDateTime now = LocalDateTime.now();

        // ⚠️ Qulf bilan: shu tranzaksiya tugaguncha boshqa so'rov shu
        // kodni o'qiy olmaydi — limit sanash shu qulf ostida.
        Promocode promo = promocodeRepo.lockByCode(code)
                .orElseThrow(() -> new BusinessException("PROMO_NOT_FOUND",
                        "Bunday promokod topilmadi", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(promo.getActive())) {
            throw new BusinessException("PROMO_INACTIVE",
                    "Bu promokod to'xtatilgan", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!promo.isWithinWindow(now)) {
            throw new BusinessException("PROMO_EXPIRED",
                    "Bu promokodning muddati o'tgan", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (redemptionRepo.existsByPromocodeIdAndUserId(promo.getId(), user.getId())) {
            throw alreadyUsed();
        }
        if (promo.getMaxRedemptions() != null
                && redemptionRepo.countByPromocodeId(promo.getId()) >= promo.getMaxRedemptions()) {
            throw new BusinessException("PROMO_EXHAUSTED",
                    "Bu promokod limiti tugagan", HttpStatus.CONFLICT);
        }

        Period period = Period.ofDays(promo.getGrantDays());

        // ⚠️ Ikki tur — ikki boshqa yozuv. Casting huquqi obuna EMAS va
        // `cms_subscription` ga tushmaydi: aks holda «faol obunachilar»
        // soni casting kodlari hisobiga shishib ketardi.
        Subscription subscription = null;
        LocalDateTime grantedUntil;

        if (promo.getGrantType() == PromocodeGrantType.CASTING_DAYS) {
            grantedUntil = premiumGrantService.extendCasting(user, period);
        } else {
            PremiumGrantService.Grant grant = premiumGrantService.extend(
                    user, period, SubscriptionSource.PROMO, null, null);
            subscription = grant.subscription();
            grantedUntil = grant.until();
        }

        PromocodeRedemption redemption;
        try {
            redemption = redemptionRepo.saveAndFlush(PromocodeRedemption.builder()
                    .promocode(promo)
                    .user(user)
                    .subscription(subscription)
                    .grantedUntil(grantedUntil)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Qulf kodni qo'riqlaydi, lekin bu cheklov boshqa yo'l bilan
            // ham buzilishi mumkin — masalan kelajakdagi kod. Bazaning
            // javobi odamga tushunarli xatoga aylanadi.
            throw alreadyUsed();
        }

        auditService.log(user, AuditAction.PROMOCODE_REDEEMED, "Promocode", promo.getId(), null,
                Map.of("code", promo.getCode(),
                        "type", promo.getGrantType().name(),
                        "days", promo.getGrantDays(),
                        "until", String.valueOf(grantedUntil)));

        return new Redemption(promo, redemption, grantedUntil);
    }

    @Transactional(readOnly = true)
    public List<PromocodeRedemption> mine(UUID userId) {
        return redemptionRepo.findAllByUserIdOrderByRedeemedAtDesc(userId);
    }

    // ----------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public List<Promocode> all() {
        return promocodeRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Promocode get(Long id) {
        return promocodeRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Promocode", id));
    }

    @Transactional(readOnly = true)
    public long redemptionCount(Long promocodeId) {
        return redemptionRepo.countByPromocodeId(promocodeId);
    }

    @Transactional(readOnly = true)
    public List<PromocodeRedemption> redemptions(Long promocodeId) {
        return redemptionRepo.findAllByPromocodeIdOrderByRedeemedAtDesc(promocodeId);
    }

    /**
     * Yangi kod.
     *
     * Kod bo'sh kelsa — generatsiya qilinadi: admin «Instagram uchun 50 ta
     * kod» deganda har birini o'zi o'ylab o'tirmaydi.
     */
    @Transactional
    public Promocode create(User actor, Draft draft) {
        validate(draft);

        String code = draft.code() == null || draft.code().isBlank()
                ? generateUnique()
                : normalize(draft.code());

        if (promocodeRepo.existsByCode(code)) {
            throw BusinessException.duplicate("PROMO_CODE_TAKEN",
                    "Bunday kod allaqachon bor: " + code);
        }

        Promocode saved = promocodeRepo.save(Promocode.builder()
                .code(code)
                // ⚠️ Tur FAQAT yaratishda belgilanadi — pastdagi `update`
                // unga tegmaydi: tarqatilgan kod boshqa narsa bera
                // boshlashi odamlarni aldash bo'lardi.
                .grantType(draft.grantType() == null
                        ? PromocodeGrantType.PREMIUM_DAYS : draft.grantType())
                .grantDays(draft.grantDays())
                .maxRedemptions(draft.maxRedemptions())
                .validFrom(draft.validFrom())
                .validUntil(draft.validUntil())
                .active(draft.active() == null || draft.active())
                .note(trimNote(draft.note()))
                .createdBy(actor == null ? null : actor.getId())
                .build());

        auditService.log(actor, AuditAction.PROMOCODE_CREATED, "Promocode", saved.getId(), null,
                Map.of("code", saved.getCode(),
                        "type", saved.getGrantType().name(),
                        "days", saved.getGrantDays()));
        return saved;
    }

    /**
     * Tahrirlash.
     *
     * ⚠️ Kodning o'zi ham, TURI ham O'ZGARMAYDI: kod allaqachon
     * tarqatilgan bo'lishi mumkin. Kodni o'zgartirish odamlar qo'lidagi
     * qog'ozni yaroqsiz qilardi; turni o'zgartirish esa undan ham
     * yomoni — kod ishlaydi, lekin va'da qilingandan boshqa narsa
     * beradi. Boshqasi kerak bo'lsa — yangisi yaratiladi, eskisi
     * to'xtatiladi.
     */
    @Transactional
    public Promocode update(User actor, Long id, Draft draft) {
        validate(draft);
        Promocode promo = get(id);

        Map<String, Object> before = snapshot(promo);

        promo.setGrantDays(draft.grantDays());
        promo.setMaxRedemptions(draft.maxRedemptions());
        promo.setValidFrom(draft.validFrom());
        promo.setValidUntil(draft.validUntil());
        if (draft.active() != null) {
            promo.setActive(draft.active());
        }
        promo.setNote(trimNote(draft.note()));

        Promocode saved = promocodeRepo.save(promo);
        auditService.log(actor, AuditAction.PROMOCODE_UPDATED, "Promocode", id,
                before, snapshot(saved));
        return saved;
    }

    // ----------------------------------------------------------------- ichki

    /** Bo'shliqlarni olib, KATTA harfga o'giradi — telefon klaviaturasi hurmati. */
    static String normalize(String raw) {
        if (raw == null) {
            throw BusinessException.validation("Promokod kiritilmadi");
        }
        String code = raw.trim().toUpperCase().replace(" ", "");
        if (code.isEmpty()) {
            throw BusinessException.validation("Promokod kiritilmadi");
        }
        if (!CODE_SHAPE.matcher(code).matches()) {
            throw BusinessException.validation(
                    "Promokod 3–32 belgi: lotin harflari, raqamlar va chiziqcha");
        }
        return code;
    }

    private void validate(Draft draft) {
        if (draft.grantDays() == null || draft.grantDays() <= 0 || draft.grantDays() > MAX_DAYS) {
            throw BusinessException.validation("Kunlar soni 1 dan " + MAX_DAYS + " gacha bo'lsin");
        }
        if (draft.maxRedemptions() != null && draft.maxRedemptions() <= 0) {
            throw BusinessException.validation("Limit noldan katta bo'lsin yoki bo'sh qolsin");
        }
        if (draft.validFrom() != null && draft.validUntil() != null
                && !draft.validUntil().isAfter(draft.validFrom())) {
            throw BusinessException.validation("Tugash sanasi boshlanishdan keyin bo'lsin");
        }
    }

    private String generateUnique() {
        // 32^8 variant — takror ehtimoli nazariy; lekin tekshirish arzon.
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(GENERATED_LENGTH);
            for (int i = 0; i < GENERATED_LENGTH; i++) {
                sb.append(GENERATE_ALPHABET.charAt(random.nextInt(GENERATE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!promocodeRepo.existsByCode(code)) {
                return code;
            }
        }
        throw new BusinessException("PROMO_GENERATE_FAILED",
                "Kod yaratib bo'lmadi, qaytadan urinib ko'ring", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static BusinessException alreadyUsed() {
        return new BusinessException("PROMO_ALREADY_USED",
                "Siz bu promokodni allaqachon ishlatgansiz", HttpStatus.CONFLICT);
    }

    private static String trimNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= 255 ? trimmed : trimmed.substring(0, 255);
    }

    private static Map<String, Object> snapshot(Promocode p) {
        return Map.of(
                "days", p.getGrantDays(),
                "max", String.valueOf(p.getMaxRedemptions()),
                "from", String.valueOf(p.getValidFrom()),
                "until", String.valueOf(p.getValidUntil()),
                "active", p.getActive());
    }

    // ------------------------------------------------------------------ DTO

    /**
     * Admin yuboradigan maydonlar.
     *
     * @param code      bo'sh bo'lsa generatsiya qilinadi (faqat yaratishda)
     * @param grantType kod nima berishi; {@code null} — {@code PREMIUM_DAYS}.
     *                  Faqat yaratishda o'qiladi
     * @param active    {@code null} — o'zgartirilmaydi
     */
    public record Draft(String code, PromocodeGrantType grantType, Integer grantDays,
                        Integer maxRedemptions, LocalDateTime validFrom,
                        LocalDateTime validUntil, Boolean active, String note) {
    }

    /** Ishlatish natijasi. */
    public record Redemption(Promocode promocode, PromocodeRedemption redemption,
                             LocalDateTime premiumUntil) {
    }

    /** Test va admin uchun: kod hozir ishlatilsa nima bo'lardi — yozmasdan. */
    @Transactional(readOnly = true)
    public Optional<String> whyNotRedeemable(Promocode promo, UUID userId, LocalDateTime now) {
        if (!Boolean.TRUE.equals(promo.getActive())) {
            return Optional.of("PROMO_INACTIVE");
        }
        if (!promo.isWithinWindow(now)) {
            return Optional.of("PROMO_EXPIRED");
        }
        if (userId != null && redemptionRepo.existsByPromocodeIdAndUserId(promo.getId(), userId)) {
            return Optional.of("PROMO_ALREADY_USED");
        }
        if (promo.getMaxRedemptions() != null
                && redemptionRepo.countByPromocodeId(promo.getId()) >= promo.getMaxRedemptions()) {
            return Optional.of("PROMO_EXHAUSTED");
        }
        return Optional.empty();
    }
}
