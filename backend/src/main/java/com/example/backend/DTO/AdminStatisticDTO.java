package com.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminStatisticDTO {
    private Long dailyCount;
    private Long totalCount;
    private Long rejectedCount;
    private Long acceptedCount;
    private Long pendingCount;
}
