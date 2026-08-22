package com.example.backend.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI hujjati (ТЗ §106).
 *
 * <h2>Nega kerak</h2>
 * Mobil va frontend jamoasi endpointni backend kodini o'qimasdan
 * ishlata olishi kerak. Hujjatsiz har bir savol backend dasturchisiga
 * borardi va kontrakt og'zaki kelishuvda qolardi.
 *
 * <h2>⚠️ Ishlab chiqarishda yopiq</h2>
 * Hujjat API'ning butun xaritasini beradi — qaysi yo'llar bor, qanday
 * maydonlar kutiladi. Bu hujum uchun tayyor reja, shuning uchun
 * {@code springdoc.swagger-ui.enabled} prod'da {@code false} bo'ladi
 * ({@code application.properties} ga qarang).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI uzcastingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("UZCASTING API")
                        .version("v1")
                        .description("""
                                Admin panel va mobil ilova uchun API.

                                Yo'l prefikslari:
                                • /api/v1/app/admin/** — admin panel (rol va ruxsat talab qiladi)
                                • /api/v1/app/**       — mobil klient
                                • /api/v1/**           — eski casting moduli (o'zgartirilmagan)

                                Xato formati barcha endpointlarda bir xil:
                                {"code": "...", "message": "...", "errors": [{"field": "...", "message": "..."}]}

                                Ko'p tilli maydonlar `translations` xaritasida keladi:
                                {"UZ": {...}, "RU": {...}, "EN": {...}}. Nashr paytida
                                uchala til ham majburiy.
                                """))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token. Refresh token API kaliti sifatida "
                                        + "QABUL QILINMAYDI (§61).")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
