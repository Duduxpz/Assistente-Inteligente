package com.aetherai.user.dto;

import com.aetherai.user.entity.UserSettingsEntity;

public record UserSettingsResponse(String theme, String locale) {

	public static UserSettingsResponse from(UserSettingsEntity settings) {
		return new UserSettingsResponse(settings.getTheme(), settings.getLocale());
	}
}
