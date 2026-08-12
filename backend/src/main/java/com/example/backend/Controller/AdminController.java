package com.example.backend.Controller;

import com.example.backend.DTO.AdminStatisticDTO;
import com.example.backend.Repository.CastingUserRepo;
import com.example.backend.Repository.NewsRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final CastingUserRepo castingUserRepo;
    private final NewsRepo newsRepo;

    @GetMapping("/statistic")
    public ResponseEntity<AdminStatisticDTO> getAdminStatistic() {
        Object[] result = (Object[]) castingUserRepo.getAdminStatistic();

        AdminStatisticDTO dto = new AdminStatisticDTO(
                ((Number) result[0]).longValue(), // dailyCount
                ((Number) result[1]).longValue(), // totalCount
                ((Number) result[2]).longValue(), // rejectedCount
                ((Number) result[3]).longValue(), // acceptedCount
                ((Number) result[4]).longValue()  // pendingCount
        );

        return ResponseEntity.ok(dto);
    }
}
