package com.aetherai.billing;

import com.aetherai.billing.dto.BillingStatusResponse;
import com.aetherai.billing.dto.CheckoutRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Profile("dev & !prod")
@RequestMapping("/api/v1/billing/test")
public class DevBillingController {

	private final DevBillingService devBillingService;

	public DevBillingController(DevBillingService devBillingService) {
		this.devBillingService = devBillingService;
	}

	@PostMapping("/activate")
	public BillingStatusResponse activate(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CheckoutRequest request) {
		return devBillingService.activatePlan(UUID.fromString(jwt.getSubject()), request.planKey());
	}

	@PostMapping("/cancel")
	public BillingStatusResponse cancel(@AuthenticationPrincipal Jwt jwt) {
		return devBillingService.cancelPlan(UUID.fromString(jwt.getSubject()));
	}
}
