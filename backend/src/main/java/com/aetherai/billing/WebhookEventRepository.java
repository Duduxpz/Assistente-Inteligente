package com.aetherai.billing;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, String> {

	boolean existsByRequestIdAndResourceId(String requestId, String resourceId);
}
