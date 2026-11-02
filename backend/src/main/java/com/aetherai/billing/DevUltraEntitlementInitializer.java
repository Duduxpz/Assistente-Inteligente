package com.aetherai.billing;

import com.aetherai.plan.PlanTier;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Component
@Profile("dev & !prod")
public class DevUltraEntitlementInitializer implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(DevUltraEntitlementInitializer.class);

	private final BillingProperties billingProperties;
	private final UserRepository userRepository;
	private final SubscriptionRepository subscriptionRepository;

	public DevUltraEntitlementInitializer(
			BillingProperties billingProperties,
			UserRepository userRepository,
			SubscriptionRepository subscriptionRepository) {
		this.billingProperties = billingProperties;
		this.userRepository = userRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String configuredEmail = billingProperties.testUltraEmail();
		if (configuredEmail == null || configuredEmail.isBlank()) {
			return;
		}

		userRepository.findByEmail(configuredEmail.trim().toLowerCase(Locale.ROOT))
				.ifPresentOrElse(foundUser -> configureTestEntitlement(foundUser.getId()),
						() -> logger.warn("The configured development Ultra test account does not exist yet."));
	}

	private void configureTestEntitlement(UUID userId) {
		UserEntity user = userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new IllegalStateException("The configured test account disappeared during startup."));
		SubscriptionEntity subscription = subscriptionRepository.findById(userId).orElse(null);
		if (subscription != null && subscription.getProviderSubscriptionId() != null) {
			logger.warn("Skipped the development Ultra entitlement because the test account has a provider subscription.");
			return;
		}
		if (subscription == null) {
			subscription = new SubscriptionEntity(userId, PlanTier.ULTRA.key(), null);
		} else {
			subscription.activateTestPlan(PlanTier.ULTRA.key(), null);
		}
		subscriptionRepository.save(subscription);
		user.setPlanKey(PlanTier.ULTRA.key());
		logger.info("Configured the development Ultra plan entitlement for the test account.");
	}
}
