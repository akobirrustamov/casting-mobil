package com.example.backend.Cms.Controller;

import com.example.backend.Admin.CurrentUser;
import com.example.backend.Admin.Dto.DonationTransactionDto;
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
import org.springframework.web.bind.annotation.RestController;

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
                        .starsBalance(b.getStarsBalance() == null ? 0L : b.getStarsBalance())
                        .coinBalance(b.getCoinBalance() == null ? 0L : b.getCoinBalance())
                        .build())
                // Hisob hali yaratilmagan — nol, bu haqiqiy nol.
                .orElse(BalanceDto.builder().starsBalance(0L).coinBalance(0L).build()));
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
        private Long starsBalance;
        private Long coinBalance;
    }
}
