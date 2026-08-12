package com.example.backend.Services.UserService;

import com.example.backend.DTO.UserDTO;
import com.example.backend.Entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User create(UserDTO dto);

    User update(UUID id, UserDTO dto);

    User getById(UUID id);

    List<User> getAll();

    void delete(UUID id);

}
