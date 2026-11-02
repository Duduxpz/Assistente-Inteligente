package com.aetherai.billing;

import com.aetherai.billing.dto.BillingStatusResponse;
import com.aetherai.billing.dto.CheckoutRequest;
import com.aetherai.billing.dto.CheckoutResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

	private final BillingService billingService;
	private final BillingProperties billingProperties;

	public BillingController(BillingService billingService, BillingProperties billingProperties) {
		this.billingService = billingService;
		this.billingProperties = billingProperties;
	}

	@GetMapping
	public BillingStatusResponse status(@AuthenticationPrincipal Jwt jwt) {
		return billingService.status(UUID.fromString(jwt.getSubject()), billingProperties.testModeEnabled());
	}

	@PostMapping("/checkout")
	public CheckoutResponse checkout(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CheckoutRequest request) {
		return billingService.startCheckout(UUID.fromString(jwt.getSubject()), request.planKey());
	}

	@PostMapping("/cancel")
	public BillingStatusResponse cancel(@AuthenticationPrincipal Jwt jwt) {
		return billingService.cancelSubscription(UUID.fromString(jwt.getSubject()));
	}
}
