package com.example.backend.Cms.Service;

import com.example.backend.Admin.Dto.TariffSaveRequest;
import com.example.backend.Admin.Dto.CurrencyPackageSaveRequest;
import com.example.backend.Cms.Entity.CurrencyPackage;
import com.example.backend.Cms.Entity.Tariff;
import com.example.backend.Cms.Entity.TariffTranslation;
import com.example.backend.Cms.Enums.Locale;
import com.example.backend.Cms.Repository.CurrencyPackageRepo;
import com.example.backend.Cms.Repository.DonationRepo;
import com.example.backend.Cms.Repository.TariffRepo;
import com.example.backend.Entity.User;
import com.example.backend.Services.AuditService.AuditAction;
import com.example.backend.Services.AuditService.AuditService;
import com.example.backend.exceptions.BusinessException;
import com.example.backend.Admin.Dto.DonationReportDto;
import com.example.backend.Cms.Entity.DonationTransaction;
import com.example.backend.Cms.Enums.DonationTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Tariflar, valyuta paketlari va donat hisobotlari.
 *
 * Narxlar kodda qotirilmaydi (§36) — hammasi shu yerdan boshqariladi.
 */
@Service
@RequiredArgsConstructor
public class MonetizationService {

    private final TariffRepo tariffRepo;
    private final CurrencyPackageRepo packageRepo;
    private final DonationRepo donationRepo;
    private final AuditService auditService;

    // ---------------------------------------------------------------- tarif

    @Transactional(readOnly = true)
    public List<Tariff> tariffs() {
        return tariffRepo.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public Tariff saveTariff(User actor, Long id, TariffSaveRequest request) {
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.validation("Narx manfiy bo'lishi mumkin emas");
        }
        var uz = request.getTranslations().get(Locale.UZ);
        if (uz == null || uz.getTitle() == null || uz.getTitle().isBlank()) {
            throw BusinessException.validation("O'zbekcha nom majburiy - u asosiy til");
        }

        Tariff tariff = id == null ? new Tariff()
                : tariffRepo.findById(id).orElseThrow(() -> BusinessException.notFound("Tariff", id));

        // Kod barqaror identifikator - tahrirlashda o'zgarmaydi
        if (id == null) {
            String code = request.getCode() == null || request.getCode().isBlank()
                    ? "m" + request.getDurationMonths() : request.getCode().trim();
            if (tariffRepo.findByCode(code).isPresent()) {
                throw BusinessException.duplicate("DUPLICATE_TARIFF_CODE",
                        "Bu kod bilan tarif allaqachon mavjud: " + code);
            }
            tariff.setCode(code);
        }

        BigDecimal before = tariff.getPrice();
        tariff.setDurationMonths(request.getDurationMonths());
        tariff.setPrice(request.getPrice());
        tariff.setCurrency(request.getCurrency() == null ? "UZS" : request.getCurrency());
        tariff.setActive(!Boolean.FALSE.equals(request.getActive()));
        tariff.setHighlighted(Boolean.TRUE.equals(request.getHighlighted()));
        tariff.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());

        // Joyida yangilanadi - clear()+add UNIQUE(tariff, locale) ni buzadi
        Map<Locale, TariffTranslation> existing = new HashMap<>();
        tariff.getTranslations().forEach(t -> existing.put(t.getLocale(), t));
        Set<Locale> keep = new HashSet<>();
        request.getTranslations().forEach((locale, dto) -> {
            if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
                return;
            }
            keep.add(locale);
            TariffTranslation row = existing.get(locale);
            if (row == null) {
                row = TariffTranslation.builder().locale(locale).build();
                tariff.addTranslation(row);
            }
            row.setName(dto.getTitle().trim());
            row.setBadge(dto.getShortDescription());
            row.setFeatures(dto.getDescription());
        });
        tariff.getTranslations().removeIf(t -> !keep.contains(t.getLocale()));

        Tariff saved = tariffRepo.save(tariff);
        auditService.log(actor, AuditAction.TARIFF_CHANGED, "Tariff", saved.getId(),
                before == null ? null : Map.of("price", before.toPlainString()),
                Map.of("price", saved.getPrice().toPlainString(),
                        "months", saved.getDurationMonths()));
        return saved;
    }

    // ------------------------------------------------------- valyuta paketi

    @Transactional(readOnly = true)
    public List<CurrencyPackage> packages() {
        return packageRepo.findAllByOrderByKindAscSortOrderAsc();
    }

    @Transactional
    /**
     * Valyuta paketini saqlash (ТЗ §40, §41).
     *
     * <h2>Nima uchun so'rov DTO'si, entity emas</h2>
     * Ilgari bu metod {@code CurrencyPackage} entity'sini qabul qilardi va
     * uni controller to'g'ridan-to'g'ri {@code @RequestBody} dan olardi.
     * Tekshiruv esa faqat shu yerda edi va {@code kind} umuman
     * tekshirilmasdi — bo'sh yuborilsa xato bazada chiqib, panelda
     * «500» ko'rinardi.
     *
     * DTO'da {@code @NotNull} bor, ya'ni xato so'rov servisga yetib ham
     * kelmaydi va admin aniq xabar oladi.
     */
    public CurrencyPackage savePackage(User actor, Long id,
                                       CurrencyPackageSaveRequest incoming) {
        // Servis to'g'ridan-to'g'ri chaqirilsa DTO annotatsiyalari
        // ishlamaydi — shuning uchun bu yerda ham tekshiriladi.
        if (incoming.getKind() == null) {
            throw BusinessException.validation("Valyuta turi tanlanmagan");
        }
        if (incoming.getAmount() == null || incoming.getAmount() <= 0) {
            throw BusinessException.validation("Miqdor noldan katta bo'lishi kerak");
        }
        if (incoming.getPrice() == null || incoming.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.validation("Narx manfiy bo'lishi mumkin emas");
        }

        CurrencyPackage p = id == null ? new CurrencyPackage()
                : packageRepo.findById(id)
                        .orElseThrow(() -> BusinessException.notFound("CurrencyPackage", id));

        p.setKind(incoming.getKind());
        p.setAmount(incoming.getAmount());
        p.setPrice(incoming.getPrice());
        p.setActive(!Boolean.FALSE.equals(incoming.getActive()));
        p.setSortOrder(incoming.getSortOrder() == null ? 0 : incoming.getSortOrder());

        CurrencyPackage saved = packageRepo.save(p);
        auditService.log(actor, id == null ? "PACKAGE_CREATED" : "PACKAGE_UPDATED",
                "CurrencyPackage", saved.getId(), null,
                Map.of("kind", saved.getKind(), "amount", saved.getAmount(),
                        "price", saved.getPrice().toPlainString()));
        return saved;
    }

    @Transactional
    public void deletePackage(User actor, Long id) {
        CurrencyPackage p = packageRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CurrencyPackage", id));
        packageRepo.delete(p);
        auditService.log(actor, "PACKAGE_DELETED", "CurrencyPackage", id);
    }

    // ---------------------------------------------------------------- donat

    /** Reyting: eng ko'p donat olgan nishonlar (§42). */
    @Transactional(readOnly = true)
    public List<DonationRepo.TargetTotal> topDonationTargets(int limit) {
        return donationRepo.topTargets(PageRequest.of(0, Math.min(Math.max(limit, 1), 100)));
    }

    /**
     * To'liq donat hisoboti (ТЗ §42).
     *
     * <h2>Nima uchun valyutalar qo'shilmaydi</h2>
     * STARS va COIN — ikki xil valyuta, kursi admin panelida alohida
     * belgilanadi (§40, §41). Ularni bitta «jami» ga qo'shish 10 so'm va
     * 10 dollarni qo'shishday bo'lardi. Kurs hozircha 0 (buyurtmachi
     * aytmagan), shuning uchun so'mdagi ekvivalent ham hisoblanmaydi —
     * soxta raqam chiqmasin.
     *
     * @param days nechа kun orqaga kunlik kesim olinadi
     */
    @Transactional(readOnly = true)
    public DonationReportDto donationReport(int limit, int days) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int window = Math.min(Math.max(days, 1), 365);
        LocalDateTime from = LocalDate.now().minusDays(window - 1L).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();

        return DonationReportDto.builder()
                .totalTransactions(donationRepo.count())
                .byKind(donationRepo.totalsByKind().stream()
                        .map(k -> DonationReportDto.KindTotal.builder()
                                .kind(k.getKind())
                                .total(k.getTotal())
                                .transactions(k.getTransactions())
                                .build())
                        .toList())
                .topCreators(targetRows(DonationTargetType.CREATOR, safeLimit))
                .topContent(targetRows(DonationTargetType.CONTENT, safeLimit))
                .daily(donationRepo.dailyTotals(from, to).stream()
                        .map(d -> DonationReportDto.DayRow.builder()
                                .date(d.getDay())
                                .kind(d.getKind())
                                .total(d.getTotal())
                                .transactions(d.getTransactions())
                                .build())
                        .toList())
                .build();
    }

    private List<DonationReportDto.TargetRow> targetRows(DonationTargetType type, int limit) {
        return donationRepo.topTargetsOfType(type, PageRequest.of(0, limit)).stream()
                .map(r -> DonationReportDto.TargetRow.builder()
                        .targetType(r.getTargetType())
                        .targetId(r.getTargetId())
                        .kind(r.getKind())
                        .total(r.getTotal())
                        .transactions(r.getTransactions())
                        .build())
                .toList();
    }

    /**
     * Tranzaksiyalar ro'yxati (ТЗ §42).
     *
     * ⚠️ Faqat O'QISH. Moliyaviy tarix o'zgarmas: tahrirlash va o'chirish
     * endpointi ataylab YO'Q.
     */
    @Transactional(readOnly = true)
    public Page<DonationTransaction> donationTransactions(Pageable pageable) {
        return donationRepo.findAllByOrderByCreatedAtDesc(pageable);
    }

    public long donationCount() {
        return donationRepo.count();
    }
}
