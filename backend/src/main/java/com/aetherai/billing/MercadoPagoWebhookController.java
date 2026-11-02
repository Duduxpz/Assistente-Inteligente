package com.aetherai.billing;

import com.aetherai.billing.MercadoPagoProperties;
import com.aetherai.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/webhooks/mercadopago")
public class MercadoPagoWebhookController {

	private final BillingService billingService;
	private final MercadoPagoProperties properties;
	private final WebhookSignatureVerifier signatureVerifier;

	public MercadoPagoWebhookController(
			BillingService billingService,
			MercadoPagoProperties properties,
			WebhookSignatureVerifier signatureVerifier) {
		this.billingService = billingService;
		this.properties = properties;
		this.signatureVerifier = signatureVerifier;
	}

	@PostMapping
	public void receive(
			@RequestParam(name = "data.id") String dataId,
			@RequestHeader(name = "x-signature", required = false) String signature,
			@RequestHeader(name = "x-request-id", required = false) String requestId) {
		if (properties.webhookSecret() == null || properties.webhookSecret().isBlank()) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "A assinatura do webhook ainda não está configurada.");
		}
		if (!signatureVerifier.isValid(signature, requestId, dataId, properties.webhookSecret())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Assinatura do webhook inválida.");
		}
		billingService.processWebhook(dataId);
	}
}
