package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceRepo extends JpaRepository<UserDevice, Long> {

    List<UserDevice> findAllByUserIdOrderByLastActiveAtDesc(UUID userId);

    List<UserDevice> findAllByUserIdAndActiveTrueOrderByLastActiveAtAsc(UUID userId);

    Optional<UserDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);
}
