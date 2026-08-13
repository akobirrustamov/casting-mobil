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
            String subject = jwtService.extractSubjectFromJwt(token);
            Optional<User> user = userRepo.findById(UUID.fromString(subject));
            if (user.isEmpty()) {
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
}
