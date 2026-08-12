package com.example.backend.Controller;

import com.example.backend.Entity.Role;
import com.example.backend.Repository.RoleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {


    private final RoleRepo roleRepo;

    @GetMapping
    public HttpEntity<?> getAllRoles(){
        List<Role> all = roleRepo.findAll();
        return ResponseEntity.ok(all);
    }
}
