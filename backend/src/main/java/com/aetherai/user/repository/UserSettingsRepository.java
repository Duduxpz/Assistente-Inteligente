package com.aetherai.user.repository;

import com.aetherai.user.entity.UserSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, UUID> {
}
