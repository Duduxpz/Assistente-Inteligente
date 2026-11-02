package com.aetherai.billing;

import com.aetherai.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoRestClientTest {

	@Test
	void createsMonthlySubscriptionUsingTheConfiguredTokenAndSafeCheckoutUrl() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		MercadoPagoRestClient client = new MercadoPagoRestClient(
				builder,
				new MercadoPagoProperties("test-access-token", "test-webhook-secret", "http://localhost:5173"));
		UUID userId = UUID.randomUUID();

		server.expect(requestTo("https://api.mercadopago.com/preapproval"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-access-token"))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json("""
						{
						  "reason": "AETHER AI Plus",
						  "external_reference": "%s",
						  "payer_email": "user@example.com",
						  "auto_recurring": {
						    "frequency": 1,
						    "frequency_type": "months",
						    "transaction_amount": 25.00,
						    "currency_id": "BRL"
						  },
						  "back_url": "http://localhost:5173/plans?billing=return",
						  "status": "pending"
						}
						""".formatted(userId)))
				.andRespond(withSuccess("""
						{"id":"preapproval-test-id","init_point":"https://www.mercadopago.com.br/subscriptions/checkout/test"}
						""", MediaType.APPLICATION_JSON));

		MercadoPagoGateway.Checkout checkout = client.createSubscription(userId, "user@example.com", "Plus", 2500);

		assertEquals("preapproval-test-id", checkout.subscriptionId());
		assertEquals("https://www.mercadopago.com.br/subscriptions/checkout/test", checkout.checkoutUrl());
		server.verify();
	}

	@Test
	void rejectsMissingCredentialsAndUntrustedCheckoutLinks() {
		MercadoPagoRestClient unconfiguredClient = new MercadoPagoRestClient(
				RestClient.builder(),
				new MercadoPagoProperties("", "", "http://localhost:5173"));
		ApiException missingCredentials = assertThrows(
				ApiException.class,
				() -> unconfiguredClient.createSubscription(UUID.randomUUID(), "user@example.com", "Plus", 2500));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, missingCredentials.getStatus());

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		MercadoPagoRestClient client = new MercadoPagoRestClient(
				builder,
				new MercadoPagoProperties("test-access-token", "test-webhook-secret", "http://localhost:5173"));
		server.expect(requestTo("https://api.mercadopago.com/preapproval"))
				.andRespond(withSuccess("""
						{"id":"preapproval-test-id","init_point":"https://attacker.example/checkout"}
						""", MediaType.APPLICATION_JSON));

		ApiException invalidLink = assertThrows(
				ApiException.class,
				() -> client.createSubscription(UUID.randomUUID(), "user@example.com", "Plus", 2500));
		assertEquals(HttpStatus.BAD_GATEWAY, invalidLink.getStatus());
		server.verify();
	}
}
