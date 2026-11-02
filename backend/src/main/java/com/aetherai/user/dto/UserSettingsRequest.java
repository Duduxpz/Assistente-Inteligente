package com.aetherai.user.dto;

import jakarta.validation.constraints.Pattern;

public record UserSettingsRequest(
		@Pattern(regexp = "dark|light|system") String theme,
		@Pattern(regexp = "pt-BR|en-US|es-ES") String locale) {
}
