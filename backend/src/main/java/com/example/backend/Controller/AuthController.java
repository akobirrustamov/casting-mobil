package com.example.backend.Controller;

import com.example.backend.DTO.UserDTO;
import com.example.backend.Entity.User;
import com.example.backend.Repository.UserRepo;
import com.example.backend.Security.JwtService;
import com.example.backend.Services.AuthService.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    private final JwtService jwtService;
    private final UserRepo userRepo; // Ensure this is final and properly injected

    @PostMapping(value = "/login", consumes = "application/json")
    public HttpEntity<?> login(@RequestBody UserDTO dto) {
        System.out.println(dto);
        return service.login(dto);
    }

    @PostMapping("/refresh")
    public HttpEntity<?> refreshUser(@RequestParam String refreshToken) {
        return service.refreshToken(refreshToken);
    }

    @GetMapping("/decode")
    public HttpEntity<?> decode(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(service.decode(token));
    }

    @PutMapping("/password/{adminId}")
    public HttpEntity<?> password(@RequestBody PasswordUpdateRequest request, @PathVariable UUID adminId) {
        System.out.printf("Password update request: %s%n", request.getPassword());
        System.out.printf("Admin Id: %s%n", adminId);
        return ResponseEntity.ok(service.password(adminId, request.getPassword()));
//        if (request.getPassword() == null || request.getPassword().isEmpty()) {
//            return ResponseEntity.badRequest().body("Password cannot be empty");
//        }
//        Optional<User> byId = userRepo.findById(adminId);
//        if (byId.isEmpty()) {
//            return ResponseEntity.notFound().build();
//        }
//        System.out.printf("byId: %s%n", byId);
//
//        User user = byId.get();
//        user.setPassword); // Ensure password is hashed before saving
//
//        userRepo.save(user);
//
//        return ResponseEntity.ok().build();
    }

    // Inner class for password update request
    public static class PasswordUpdateRequest {
        private String password;

        // Getters and setters
        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}