package com.example.backend.Services.UserService;

import com.example.backend.DTO.UserDTO;
import com.example.backend.Entity.Role;
import com.example.backend.Entity.User;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.RoleRepo;
import com.example.backend.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User create(UserDTO dto) {

        userRepo.findByPhone(dto.getPhone())
                .ifPresent(u -> {
                    throw new RuntimeException("User with this phone already exists");
                });

        List<Role> roles = roleRepo.findAllById(dto.getRoleIds());

        User user = User.builder()
                .phone(dto.getPhone())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .roles(roles)
                .created_at(LocalDateTime.now())
                .build();

        return userRepo.save(user);
    }



    @Override
    public User update(UUID id, UserDTO dto) {

        User user = getById(id);

        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getName() != null) {
            user.setName(dto.getName());
        }

        if (dto.getRoleIds() != null) {
            user.setRoles(roleRepo.findAllById(dto.getRoleIds()));
        }

        return userRepo.save(user);
    }

    @Override
    public User getById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public List<User> getAll() {
        return userRepo.findAll();
    }

    @Override
    public void delete(UUID id) {
        userRepo.deleteById(id);
    }


}
