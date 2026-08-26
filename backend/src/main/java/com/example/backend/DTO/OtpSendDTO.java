package com.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mobil ilova telefon raqamga SMS-kod so'raganda yuboradi. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpSendDTO {
    private String phone;
}
