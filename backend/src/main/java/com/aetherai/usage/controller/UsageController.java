package com.aetherai.usage.controller;

import com.aetherai.usage.dto.UsageSummary;
import com.aetherai.usage.service.UsageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usage")
public class UsageController {

	private final UsageService usageService;

	public UsageController(UsageService usageService) {
		this.usageService = usageService;
	}

	@GetMapping
	public UsageSummary getUsage(@AuthenticationPrincipal Jwt jwt) {
		return usageService.forUser(UUID.fromString(jwt.getSubject()));
	}
}
