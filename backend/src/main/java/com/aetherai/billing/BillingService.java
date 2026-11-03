package com.aetherai.billing;

import com.aetherai.billing.dto.BillingStatusResponse;
import com.aetherai.billing.dto.CheckoutResponse;
import com.aetherai.common.exception.ApiException;
import com.aetherai.plan.PlanTier;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class BillingService {

	private final UserRepository userRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final MercadoPagoGateway mercadoPagoGateway;
	private final WebhookEventRepository webhookEventRepository;

	public BillingService(
			UserRepository userRepository,
			SubscriptionRepository subscriptionRepository,
			MercadoPagoGateway mercadoPagoGateway,
			WebhookEventRepository webhookEventRepository) {
		this.userRepository = userRepository;
		this.subscriptionRepository = subscriptionRepository;
		this.mercadoPagoGateway = mercadoPagoGateway;
		this.webhookEventRepository = webhookEventRepository;
	}

	@Transactional
	public CheckoutResponse startCheckout(UUID userId, String planKey) {
		PlanTier plan = planForCheckout(planKey);
		UserEntity user = findUserForUpdate(userId);
		SubscriptionEntity existing = subscriptionRepository.findById(userId).orElse(null);
		if (existing != null) {
			if ("pending".equals(existing.getStatus())) {
				if (existing.getPlanKey().equals(plan.key()) && existing.getCheckoutUrl() != null) {
					return new CheckoutResponse(existing.getCheckoutUrl());
				}
				throw new ApiException(HttpStatus.CONFLICT,
						"Cancele ou continue o pagamento pendente antes de escolher outro plano.");
			}
			if (Arrays.asList("authorized", "paused").contains(existing.getStatus())
					|| existing.getCurrentPeriodEnd() != null
					&& existing.getCurrentPeriodEnd().isAfter(Instant.now())) {
				throw new ApiException(HttpStatus.CONFLICT,
						"Cancele a renovação atual antes de iniciar outra assinatura.");
			}
		}

		MercadoPagoGateway.Checkout checkout = mercadoPagoGateway.createSubscription(
				userId,
				user.getEmail(),
				plan.displayName(),
				plan.monthlyPriceCents());
		subscriptionRepository.save(new SubscriptionEntity(
				userId,
				plan.key(),
				checkout.subscriptionId(),
				checkout.checkoutUrl()));
		return new CheckoutResponse(checkout.checkoutUrl());
	}

	@Transactional
	public BillingStatusResponse status(UUID userId, boolean testModeEnabled) {
		UserEntity user = findUser(userId);
		SubscriptionEntity subscription = subscriptionRepository.findById(userId).orElse(null);
		expirePlanIfNeeded(user, subscription);
		return new BillingStatusResponse(
				user.getPlanKey(),
				subscription == null ? null : subscription.getStatus(),
				subscription == null ? null : subscription.getCurrentPeriodEnd(),
				subscription != null && "cancelled".equals(subscription.getStatus())
						&& subscription.getCurrentPeriodEnd() != null
						&& subscription.getCurrentPeriodEnd().isAfter(Instant.now()),
				testModeEnabled,
				subscription != null
						&& Arrays.asList("authorized", "cancelled").contains(subscription.getStatus())
						&& subscription.getProviderSubscriptionId() == null);
	}

	@Transactional
	public BillingStatusResponse cancelSubscription(UUID userId) {
		UserEntity user = findUserForUpdate(userId);
		SubscriptionEntity subscription = subscriptionRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Sua conta não possui uma assinatura ativa."));
		if ("cancelled".equals(subscription.getStatus())) {
			return status(userId, false);
		}
		if (!Arrays.asList("pending", "authorized", "paused").contains(subscription.getStatus())
				|| subscription.getProviderSubscriptionId() == null) {
			throw new ApiException(HttpStatus.CONFLICT, "Não há assinatura pendente ou ativa para cancelar.");
		}

		MercadoPagoGateway.Subscription providerSubscription =
				mercadoPagoGateway.cancelSubscription(subscription.getProviderSubscriptionId());
		if (!"cancelled".equals(providerSubscription.status())
				|| !subscription.getProviderSubscriptionId().equals(providerSubscription.id())
				|| !userId.toString().equals(providerSubscription.externalReference())) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago não confirmou o cancelamento.");
		}
		updateFromProvider(subscription, user, providerSubscription);
		return status(userId, false);
	}

	@Transactional
	public void cancelForAccountDeletion(UUID userId) {
		SubscriptionEntity subscription = subscriptionRepository.findById(userId).orElse(null);
		if (subscription == null || subscription.getProviderSubscriptionId() == null
				|| !Arrays.asList("pending", "authorized", "paused").contains(subscription.getStatus())) {
			return;
		}

		MercadoPagoGateway.Subscription providerSubscription =
				mercadoPagoGateway.cancelSubscription(subscription.getProviderSubscriptionId());
		if (!"cancelled".equals(providerSubscription.status())
				|| !subscription.getProviderSubscriptionId().equals(providerSubscription.id())
				|| !userId.toString().equals(providerSubscription.externalReference())) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "Não foi possível cancelar a assinatura antes de excluir a conta.");
		}
		UserEntity user = findUserForUpdate(userId);
		updateFromProvider(subscription, user, providerSubscription);
	}

	@Transactional
	public void processWebhook(String providerSubscriptionId, String requestId) {
		if (requestId == null || requestId.isBlank() || requestId.length() > 128
				|| providerSubscriptionId == null || providerSubscriptionId.isBlank()
				|| providerSubscriptionId.length() > 80) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "O webhook contém identificadores inválidos.");
		}
		SubscriptionEntity subscription = subscriptionRepository
				.findByProviderSubscriptionIdForUpdate(providerSubscriptionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assinatura não encontrada."));
		if (webhookEventRepository.existsByRequestIdAndResourceId(requestId, providerSubscriptionId)) {
			return;
		}
		UserEntity user = findUserForUpdate(subscription.getUserId());
		MercadoPagoGateway.Subscription providerSubscription =
				mercadoPagoGateway.getSubscription(providerSubscriptionId);
		if (!providerSubscriptionId.equals(providerSubscription.id())) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago retornou uma assinatura diferente da notificada.");
		}
		if (!subscription.getUserId().toString().equals(providerSubscription.externalReference())) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "A referência da assinatura não corresponde à conta.");
		}
		updateFromProvider(subscription, user, providerSubscription);
		webhookEventRepository.save(new WebhookEventEntity(
				webhookEventKey(requestId, providerSubscriptionId),
				requestId,
				providerSubscriptionId));
	}

	@Transactional
	public void refreshPlanAccess(UUID userId) {
		UserEntity user = findUser(userId);
		expirePlanIfNeeded(user, subscriptionRepository.findById(userId).orElse(null));
	}

	private void updateFromProvider(
			SubscriptionEntity subscription,
			UserEntity user,
			MercadoPagoGateway.Subscription providerSubscription) {
		String status = providerSubscription.status();
		if (!Arrays.asList("pending", "authorized", "cancelled", "paused", "expired").contains(status)) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago retornou um status de assinatura desconhecido.");
		}
		if ("authorized".equals(status) && providerSubscription.nextPaymentAt() == null) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago não informou a próxima cobrança.");
		}

		subscription.updateFromProvider(status, providerSubscription.nextPaymentAt());
		if ("authorized".equals(status)) {
			user.setPlanKey(subscription.getPlanKey());
		} else if (subscription.getCurrentPeriodEnd() == null
				|| !subscription.getCurrentPeriodEnd().isAfter(Instant.now())) {
			user.setPlanKey(PlanTier.FREE.key());
		}
	}

	private void expirePlanIfNeeded(UserEntity user, SubscriptionEntity subscription) {
		if (subscription != null
				&& !PlanTier.FREE.key().equals(user.getPlanKey())
				&& subscription.getCurrentPeriodEnd() != null
				&& !subscription.getCurrentPeriodEnd().isAfter(Instant.now())) {
			user.setPlanKey(PlanTier.FREE.key());
		}
	}

	private PlanTier planForCheckout(String planKey) {
		return Arrays.stream(PlanTier.values())
				.filter(plan -> plan.key().equals(planKey))
				.filter(plan -> plan.monthlyPriceCents() > 0)
				.findFirst()
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "O plano solicitado não está disponível para compra."));
	}

	private UserEntity findUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
	}

	private UserEntity findUserForUpdate(UUID userId) {
		return userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
	}

	private String webhookEventKey(String requestId, String resourceId) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest((requestId + ":" + resourceId).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is required by the runtime.", exception);
		}
	}
}
