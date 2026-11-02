package com.aetherai.user.service;

import com.aetherai.user.dto.UserSettingsRequest;
import com.aetherai.user.dto.UserSettingsResponse;
import com.aetherai.user.entity.UserSettingsEntity;
import com.aetherai.user.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserSettingsService {

	private final UserSettingsRepository settingsRepository;

	public UserSettingsService(UserSettingsRepository settingsRepository) {
		this.settingsRepository = settingsRepository;
	}

	@Transactional(readOnly = true)
	public UserSettingsResponse get(UUID userId) {
		return settingsRepository.findById(userId)
				.map(UserSettingsResponse::from)
				.orElseGet(() -> new UserSettingsResponse("dark", "pt-BR"));
	}

	@Transactional
	public UserSettingsResponse update(UUID userId, UserSettingsRequest request) {
		UserSettingsEntity current = settingsRepository.findById(userId)
				.orElseGet(() -> new UserSettingsEntity(userId, "dark", "pt-BR"));
		UserSettingsEntity updated = new UserSettingsEntity(
				userId,
				request.theme() == null ? current.getTheme() : request.theme(),
				request.locale() == null ? current.getLocale() : request.locale());
		return UserSettingsResponse.from(settingsRepository.save(updated));
	}
}
