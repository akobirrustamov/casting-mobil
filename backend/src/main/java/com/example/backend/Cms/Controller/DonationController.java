package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.CurrencyPackageDto;
import com.example.backend.Admin.Dto.DonationTransactionDto;
import com.example.backend.Admin.Dto.PageResponse;
import com.example.backend.Cms.Enums.CurrencyKind;
import com.example.backend.Cms.Enums.DonationTargetType;
import com.example.backend.Cms.Service.DonationService;
import com.example.backend.Cms.Repository.UserBalanceRepo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Donat yuborish va balansni ko'rish (ТЗ §39, §43).
 *
 * <h2>Nima uchun ochiq emas</h2>
 * Donat balansdan pul yechadi — kim yuborayotgani aniq bo'lishi shart.
 * Anonim donat balans egasini aniqlab bo'lmasligini anglatardi.
 */
@RestController
@RequestMapping("/api/v1/app/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;
    private final UserBalanceRepo balanceRepo;
    private final com.example.backend.Cms.Repository.DonationRepo donationRepo;
    private final com.example.backend.Cms.Service.PackagePurchaseService packagePurchaseService;
    private final com.example.backend.Cms.Service.MonetizationService monetizationService;
    private final com.example.backend.Cms.Service.CurrencyPricingService currencyPricingService;

    @PostMapping
    public ResponseEntity<DonationTransactionDto> donate(@Valid @RequestBody DonateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DonationTransactionDto.from(
                donationService.donate(CurrentUser.get(), request.getTargetType(),
                        request.getTargetId(), request.getKind(), request.getAmount())));
    }

    /**
     * Balans (ТЗ §43).
     *
     * ТЗ: «Hozir mobil UI yozilmaydi, lekin backend/data model buning
     * uchun tayyor bo'lsin.» Bu endpoint aynan shu tayyorlik.
     */
    @GetMapping("/balance")
    public ResponseEntity<BalanceDto> balance() {
        var user = CurrentUser.get();
        return ResponseEntity.ok(balanceRepo.findByUserId(user.getId())
                .map(b -> BalanceDto.builder()
                        .moneyBalance(b.getMoneyBalance() == null
                                ? BigDecimal.ZERO : b.getMoneyBalance())
                        .starsBalance(b.getStarsBalance() == null ? 0L : b.getStarsBalance())
                        .coinBalance(b.getCoinBalance() == null ? 0L : b.getCoinBalance())
                        .build())
                // Hisob hali yaratilmagan — nol, bu haqiqiy nol.
                .orElse(BalanceDto.builder()
                        .moneyBalance(BigDecimal.ZERO)
                        .starsBalance(0L)
                        .coinBalance(0L)
                        .build()));
    }

    /**
     * Sotib olish mumkin bo'lgan paketlar (ТЗ §40, §41).
     *
     * <h2>Nima uchun faqat sotib olinadiganlari</h2>
     * V5 barcha paketlarni {@code 0.00} narx bilan qo'shgan — buyurtmachi
     * kursni hali aytmagan. Ular {@code active = true} bo'lgani uchun
     * ro'yxatda «1000 yulduz — 0 so'm» bo'lib chiqardi, ya'ni BEPUL
     * yulduz taklifiday ko'rinardi.
     *
     * Shuning uchun narxi belgilanmagan paket ochiq ro'yxatga umuman
     * kirmaydi. Admin panelida esa ular ko'rinadi — admin narx
     * yo'qligini bilishi kerak.
     */
    /**
     * Foydalanuvchining O'Z donat tarixi (ТЗ §43).
     *
     * <h2>Nima uchun balansning yonida</h2>
     * Profilda «Stars balance» va «Coin balance» ko'rinadi. Bitta son
     * esa savolga javob bermaydi: «50 yulduzim bor edi, endi 20 ta —
     * qolgani qayerga ketdi?» Tarixsiz foydalanuvchi buni bila olmaydi.
     *
     * <h2>Nima uchun ID parametri YO'Q</h2>
     * Kimning tarixi ko'rsatilishi tokendan olinadi. ID parametri bo'lsa,
     * uni almashtirib boshqa odamning donatlarini o'qish mumkin bo'lardi.
     */
    @GetMapping("/my")
    public ResponseEntity<PageResponse<DonationTransactionDto>> myDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = donationRepo.findAllBySenderIdOrderByCreatedAtDesc(
                CurrentUser.get().getId(),
                org.springframework.data.domain.PageRequest.of(Math.max(page, 0), safeSize));

        return ResponseEntity.ok(PageResponse.of(result, DonationTransactionDto::from));
    }

    @GetMapping("/packages")
    public ResponseEntity<List<CurrencyPackageDto>> packages() {
        return ResponseEntity.ok(monetizationService.packages().stream()
                .filter(p -> !Boolean.FALSE.equals(p.getActive()))
                .filter(currencyPricingService::isPurchasable)
                .map(p -> CurrencyPackageDto.from(p,
                        currencyPricingService.effectivePrice(p), true))
                .toList());
    }

    /**
     * Paketni sotib olish (ТЗ §44).
     *
     * <h2>Ikki yo'l</h2>
     * <ul>
     *   <li>{@code INTERNAL_BALANCE} — ichki hisobdan. BUGUN ishlaydi;</li>
     *   <li>{@code PAYMENT_SYSTEM} — tashqi provayder. Ulanmagan, 503.</li>
     * </ul>
     *
     * Soxta «to'landi» javobi berilmaydi: foydalanuvchi yulduz olardi,
     * pul esa hech qayerdan kelmasdi.
     */
    @PostMapping("/packages/{id}/purchase")
    public ResponseEntity<PurchaseDto> purchasePackage(
            @PathVariable Long id, @Valid @RequestBody PurchaseRequest request) {

        var purchase = packagePurchaseService.buy(
                CurrentUser.get(), id, request.getSource());

        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseDto.builder()
                .purchaseId(purchase.getId())
                .packageId(purchase.getTargetId())
                .amountPaid(purchase.getAmount())
                .currency(purchase.getCurrency())
                .createdAt(purchase.getCreatedAt())
                .build());
    }

    @Data
    public static class PurchaseRequest {
        @NotNull(message = "To'lov usuli tanlanmagan")
        private com.example.backend.Cms.Enums.FundingSource source;
    }

    @Data
    @Builder
    public static class PurchaseDto {
        private Long purchaseId;
        private Long packageId;
        private java.math.BigDecimal amountPaid;
        private String currency;
        private java.time.LocalDateTime createdAt;
    }

    @Data
    public static class DonateRequest {

        @NotNull(message = "Kimni qo'llab-quvvatlash kerakligi ko'rsatilmagan")
        private DonationTargetType targetType;

        @NotNull(message = "Nishon ko'rsatilmagan")
        private Long targetId;

        @NotNull(message = "Valyuta tanlanmagan")
        private CurrencyKind kind;

        /** ⚠️ Butun son: yulduz va tangalar bo'linmaydi. */
        @NotNull(message = "Miqdor kiritilmagan")
        @Min(value = 1, message = "Miqdor noldan katta bo'lishi kerak")
        private Long amount;
    }

    @Data
    @Builder
    public static class BalanceDto {
        /**
         * Hisobdagi pul (so'm).
         *
         * <h2>Nima uchun qo'shildi</h2>
         * {@code UserBalance} da bu maydon BOR edi, lekin DTO uni
         * bermasdi. Natijada ilova profilida uchta sondan biri manbasiz
         * qolardi — maketda esa u aynan birinchi turadi
         * («Balance 56 000 so'm»). Sonni o'ylab topib bo'lmaydi: odam
         * o'zida yo'q pulni ko'rgan bo'lardi.
         */
        private BigDecimal moneyBalance;
        private Long starsBalance;
        private Long coinBalance;
    }
}
