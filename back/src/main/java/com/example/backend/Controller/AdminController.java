package com.example.backend.Controller;

import com.example.backend.DTO.UserDTO;
import com.example.backend.Entity.User;
import com.example.backend.Services.UserService.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/admin/users")
public class AdminController {

    private final UserService userService;



    @PostMapping
    public ResponseEntity<User> create(@RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(
            @PathVariable UUID id,
            @RequestBody UserDTO dto
    ) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
