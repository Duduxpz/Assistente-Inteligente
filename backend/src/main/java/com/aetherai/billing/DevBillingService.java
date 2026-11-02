package com.aetherai.billing;

import com.aetherai.billing.dto.BillingStatusResponse;
import com.aetherai.common.exception.ApiException;
import com.aetherai.plan.PlanTier;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

@Service
@Profile("dev & !prod")
public class DevBillingService {

	private final UserRepository userRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final BillingService billingService;

	public DevBillingService(
			UserRepository userRepository,
			SubscriptionRepository subscriptionRepository,
			BillingService billingService) {
		this.userRepository = userRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.billingService = billingService;
	}

	@Transactional
	public BillingStatusResponse activatePlan(UUID userId, String planKey) {
		PlanTier plan = Arrays.stream(PlanTier.values())
				.filter(value -> value.key().equals(planKey) && value.monthlyPriceCents() > 0)
				.findFirst()
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Escolha um plano pago disponível para o teste."));
		UserEntity user = findUserForUpdate(userId);
		SubscriptionEntity subscription = subscriptionRepository.findById(userId).orElse(null);
		if (subscription != null && subscription.getProviderSubscriptionId() != null
				&& ("pending".equals(subscription.getStatus())
				|| "authorized".equals(subscription.getStatus())
				|| "paused".equals(subscription.getStatus()))) {
			throw new ApiException(HttpStatus.CONFLICT,
					"Cancele a assinatura do Mercado Pago antes de ativar um plano de teste.");
		}

		Instant periodEnd = Instant.now().plus(30, ChronoUnit.DAYS);
		if (subscription == null) {
			subscription = new SubscriptionEntity(userId, plan.key(), periodEnd);
		} else {
			subscription.activateTestPlan(plan.key(), periodEnd);
		}
		subscriptionRepository.save(subscription);
		user.setPlanKey(plan.key());
		return billingService.status(userId, true);
	}

	@Transactional
	public BillingStatusResponse cancelPlan(UUID userId) {
		UserEntity user = findUserForUpdate(userId);
		SubscriptionEntity subscription = subscriptionRepository.findById(userId)
				.filter(value -> value.getProviderSubscriptionId() == null)
				.filter(value -> "authorized".equals(value.getStatus()))
				.orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Não há um plano de teste ativo para cancelar."));
		subscription.updateFromProvider("cancelled", null);
		if (subscription.getCurrentPeriodEnd() == null || !subscription.getCurrentPeriodEnd().isAfter(Instant.now())) {
			user.setPlanKey(PlanTier.FREE.key());
		}
		return billingService.status(userId, true);
	}

	private UserEntity findUserForUpdate(UUID userId) {
		return userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
	}
}
