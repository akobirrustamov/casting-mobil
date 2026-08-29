package com.example.backend.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mobil ilova refresh tokenni shu yerga yuboradi.
 *
 * <h2>⚠️ Nega TANADA, URL da emas</h2>
 * Eski {@code /api/v1/auth/refresh} uni {@code @RequestParam} bilan,
 * ya'ni URL so'rov qatorida qabul qiladi. Bunday token:
 *
 * <ul>
 *   <li>server kirish jurnaliga tushadi;</li>
 *   <li>proksi va CDN loglariga tushadi;</li>
 *   <li>brauzer tarixida qoladi.</li>
 * </ul>
 *
 * Ya'ni bir kunlik kirish huquqi bir nechta jurnalda ochiq yotadi.
 * Yangi endpoint uni tanada oladi — {@code AdminAuthController}
 * bu masalani cookie bilan hal qilgani kabi.
 *
 * <h2>Nom uslubi</h2>
 * {@code refresh_token} — kirish javobidagi maydon bilan AYNAN bir
 * xil. Klient uni qaytarib yuboradi va nomni o'zgartirishi shart
 * emas.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshRequestDTO {

    @JsonProperty("refresh_token")
    private String refreshToken;
}
