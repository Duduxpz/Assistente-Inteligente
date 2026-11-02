package com.aetherai.plan.controller;

import com.aetherai.common.exception.ApiException;
import com.aetherai.billing.BillingService;
import com.aetherai.plan.dto.PlanCatalogResponse;
import com.aetherai.plan.service.PlanCatalogService;
import com.aetherai.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

	private final PlanCatalogService planCatalogService;
	private final UserRepository userRepository;
	private final BillingService billingService;

	public PlanController(
			PlanCatalogService planCatalogService,
			UserRepository userRepository,
			BillingService billingService) {
		this.planCatalogService = planCatalogService;
		this.userRepository = userRepository;
		this.billingService = billingService;
	}

	@GetMapping
	public PlanCatalogResponse catalog(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		billingService.refreshPlanAccess(userId);
		String currentPlanKey = userRepository.findById(userId)
				.map(user -> user.getPlanKey())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
		return planCatalogService.catalog(currentPlanKey);
	}
}
