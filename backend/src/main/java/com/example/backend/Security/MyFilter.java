package com.example.backend.Security;

import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * Token bo'lsa - foydalanuvchini SecurityContext'ga joylaydi.
 * Token yo'q yoki yaroqsiz bo'lsa - so'rovni to'xtatmaydi (endpointlar hozircha ochiq).
 * Endpointlarni yopish SecurityConfig orqali bosqichma-bosqich qilinadi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepo userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String token = jwtService.normalizeToken(request.getHeader("Authorization"));

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            if (!jwtService.validateToken(token)) {
                return;
            }
            // ⚠️ Refresh token — API kaliti EMAS (§61). Ilgari ikkalasi
            // bir xil tuzilishda edi va o'g'irlangan refresh token bilan
            // 24 soat davomida hamma narsa qilish mumkin edi.
            if (jwtService.isRefreshToken(token)) {
                log.debug("Refresh token API so'roviga ishlatilmoqda - rad etildi");
                return;
            }
            String subject = jwtService.extractSubjectFromJwt(token);
            Optional<User> user = userRepo.findById(UUID.fromString(subject));
            if (user.isEmpty()) {
                return;
            }
            // Admin majburiy chiqargan — undan oldingi token yaroqsiz, garchi
            // muddati tugamagan bo'lsa ham. Refresh token ham bekor qilingan,
            // shuning uchun ilova 401 dan keyin kirish oynasiga qaytadi.
            if (issuedBeforeCutoff(token, user.get())) {
                log.debug("Token majburiy chiqarishdan oldin berilgan - rad etildi");
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user.get(), null, user.get().getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.debug("Token bo'yicha autentifikatsiya o'tmadi: {}", e.getMessage());
        }
    }

    /**
     * ⚠️ {@code iat} yo'q eski token cheklov qo'yilgan foydalanuvchida
     * ham RAD etiladi: uning qachon berilganini bilib bo'lmaydi, va
     * «chiqarib yuborildi» degan va'da buzilgandan ko'ra qayta kirish arzon.
     */
    private boolean issuedBeforeCutoff(String token, User user) {
        LocalDateTime cutoff = user.getSessionsValidAfter();
        if (cutoff == null) {
            return false;
        }
        Instant iat = jwtService.issuedAt(token);
        return iat == null || iat.isBefore(cutoff.atZone(ZoneId.systemDefault()).toInstant());
    }
}
