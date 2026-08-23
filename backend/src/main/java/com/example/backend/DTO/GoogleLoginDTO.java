package com.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mobil ilova Google'dan olgan ID token'ni shu yerga yuboradi.
 * Server uni tekshiradi va o'zining JWT'sini qaytaradi.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleLoginDTO {
    private String idToken;
}
