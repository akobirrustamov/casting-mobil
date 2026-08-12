package com.example.backend.Security;

import com.example.backend.Repository.UserRepo;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@CrossOrigin
@Configuration
@RequiredArgsConstructor
public class MyFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepo userRepo;

    // 🔥 Extract REAL client IP
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For: clientIp, proxy1, proxy2 ...
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();  // fallback
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws IOException, ServletException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        String realIp = getClientIp(request);

        System.out.println("==============================================");
        System.out.println("Method: " + method);
        System.out.println("Path:   " + requestPath);
        System.out.println("Client: " + realIp);
        System.out.println("==============================================");

        // ⚠️ IMPORTANT: Call next filter
        filterChain.doFilter(request, response);
    }
}
