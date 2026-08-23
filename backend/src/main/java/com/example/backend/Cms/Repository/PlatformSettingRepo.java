package com.example.backend.Cms.Repository;

import com.example.backend.Cms.Entity.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingRepo extends JpaRepository<PlatformSetting, String> {
}
