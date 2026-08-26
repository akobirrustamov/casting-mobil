package com.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mobil ilova SMS'dan kelgan kodni shu yerga yuboradi — muvaffaqiyatda JWT qaytadi. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpVerifyDTO {
    private String phone;
    private String code;
}
