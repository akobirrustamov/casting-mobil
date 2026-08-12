package com.example.backend.Repository;


import com.example.backend.Entity.Role;
import com.example.backend.Enums.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoleRepo extends JpaRepository<Role, Integer> {

    Role findByName(UserRoles name);
}
