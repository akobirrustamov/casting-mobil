package com.example.backend.Security;

import com.example.backend.DTO.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Himoyalangan endpointga ruxsatsiz kirishda HTML o'rniga JSON qaytaradi.
 *
 * Ikki holat farqlanadi:
 *   401 — token umuman yo'q yoki yaroqsiz;
 *   403 — token bor, lekin huquq yetmaydi.
 *
 * Format {@link ApiError} bilan bir xil, ya'ni klient barcha xatolarni
 * bitta usulda o'qiydi.
 */
@Component
@RequiredArgsConstructor
public class RestAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED", "Avtorizatsiya talab qilinadi");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED", "Bu amal uchun ruxsat yo'q");
    }

    private void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ApiError(code, message));
    }
}
