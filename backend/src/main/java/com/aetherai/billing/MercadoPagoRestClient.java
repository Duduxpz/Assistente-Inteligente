package com.aetherai.billing;

import com.aetherai.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class MercadoPagoRestClient implements MercadoPagoGateway {

	private final RestClient restClient;
	private final MercadoPagoProperties properties;

	public MercadoPagoRestClient(RestClient.Builder builder, MercadoPagoProperties properties) {
		this.restClient = builder.baseUrl("https://api.mercadopago.com").build();
		this.properties = properties;
	}

	@Override
	public Checkout createSubscription(UUID userId, String email, String planName, long amountCents) {
		JsonNode response = execute(() -> client().post()
					.uri("/preapproval")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"reason", "AETHER AI " + planName,
							"external_reference", userId.toString(),
							"payer_email", email,
							"auto_recurring", Map.of(
									"frequency", 1,
									"frequency_type", "months",
									"transaction_amount", BigDecimal.valueOf(amountCents, 2),
									"currency_id", "BRL"),
							"back_url", properties.frontendBaseUrl() + "/plans?billing=return",
							"status", "pending"))
					.retrieve()
					.body(JsonNode.class));
		String id = text(response, "id");
		String checkoutUrl = text(response, "init_point");
		if (id == null || id.isBlank() || checkoutUrl == null || checkoutUrl.isBlank()) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago retornou uma resposta de assinatura inválida.");
		}
		try {
			URI uri = URI.create(checkoutUrl);
			String host = uri.getHost();
			if (!"https".equalsIgnoreCase(uri.getScheme())
					|| host == null
					|| !(host.equals("mercadopago.com.br") || host.endsWith(".mercadopago.com.br"))) {
				throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago retornou um endereço de pagamento inválido.");
			}
		} catch (IllegalArgumentException exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago retornou um endereço de pagamento inválido.");
		}
		return new Checkout(id, checkoutUrl);
	}

	@Override
	public Subscription getSubscription(String subscriptionId) {
		return toSubscription(execute(() -> client().get()
				.uri("/preapproval/{id}", subscriptionId)
				.retrieve()
				.body(JsonNode.class)));
	}

	@Override
	public Subscription cancelSubscription(String subscriptionId) {
		JsonNode response = execute(() -> client().put()
				.uri("/preapproval/{id}", subscriptionId)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("status", "cancelled"))
				.retrieve()
				.body(JsonNode.class));
		return toSubscription(response);
	}

	private <T> T execute(Supplier<T> request) {
		try {
			return request.get();
		} catch (RestClientResponseException exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY,
					"O Mercado Pago recusou a solicitação (HTTP " + exception.getStatusCode().value() + ").");
		} catch (ResourceAccessException exception) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível conectar ao Mercado Pago.");
		} catch (RestClientException exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago retornou uma resposta inválida.");
		}
	}

	private RestClient client() {
		if (properties.accessToken() == null || properties.accessToken().isBlank()) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "A integração com o Mercado Pago ainda não está configurada.");
		}
		return restClient.mutate()
				.defaultHeader("Authorization", "Bearer " + properties.accessToken())
				.build();
	}

	private Subscription toSubscription(JsonNode response) {
		String id = text(response, "id");
		String externalReference = text(response, "external_reference");
		String status = text(response, "status");
		String nextPaymentAt = text(response, "next_payment_date");
		if (id == null || status == null) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago retornou uma resposta de assinatura inválida.");
		}
		Instant nextPayment;
		try {
			nextPayment = nextPaymentAt == null || nextPaymentAt.isBlank()
					? null
					: OffsetDateTime.parse(nextPaymentAt).toInstant();
		} catch (java.time.DateTimeException exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O Mercado Pago retornou uma data de cobrança inválida.");
		}
		return new Subscription(id, externalReference, status, nextPayment);
	}

	private String text(JsonNode node, String field) {
		JsonNode value = node == null ? null : node.get(field);
		return value != null && value.isString() ? value.stringValue() : null;
	}
}
