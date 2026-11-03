package com.aetherai.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

	Optional<SubscriptionEntity> findByProviderSubscriptionId(String providerSubscriptionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select subscription from SubscriptionEntity subscription where subscription.providerSubscriptionId = :providerSubscriptionId")
	Optional<SubscriptionEntity> findByProviderSubscriptionIdForUpdate(
			@Param("providerSubscriptionId") String providerSubscriptionId);
}
