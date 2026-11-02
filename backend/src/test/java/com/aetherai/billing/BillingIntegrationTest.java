package com.aetherai.billing;

import com.aetherai.billing.MercadoPagoGateway.Checkout;
import com.aetherai.billing.MercadoPagoGateway.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:aetherai-billing;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"app.documents.storage-path=target/test-documents",
		"security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.email.provider=none",
		"ai.provider=none",
		"app.mercadopago.webhook-secret=test-webhook-secret"
})
@AutoConfigureMockMvc
@Import(BillingIntegrationTest.GatewayTestConfiguration.class)
class BillingIntegrationTest {

	private static final String PASSWORD = "a-secure-test-password-123";
	private static final String REQUEST_ID = "mp-request-456";
	private static final String TIMESTAMP = "1710000000";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RecordingMercadoPagoGateway gateway;

	private String email;
	private String token;
	private UUID userId;
	private String providerId;

	@BeforeEach
	void registerUser() throws Exception {
		email = "billing-" + UUID.randomUUID() + "@example.com";
		MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s","displayName":"Billing User"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
		String response = registration.getResponse().getContentAsString();
		token = com.jayway.jsonpath.JsonPath.read(response, "$.accessToken");
		userId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.user.id"));
		providerId = "mp-subscription-" + UUID.randomUUID();
		gateway.setSubscription(null);
		gateway.setProviderId(providerId);
	}

	@Test
	void planChangesOnlyAfterSignedWebhookAndCancellationKeepsPaidTime() throws Exception {
		mockMvc.perform(post("/api/v1/billing/checkout")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"planKey\":\"plus\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkoutUrl").value("https://www.mercadopago.com.br/subscriptions/checkout/test"));
		assertEquals(userId, gateway.createdForUser());

		mockMvc.perform(get("/api/v1/plans")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentPlanKey").value("free"));

		Instant nextPayment = Instant.now().plusSeconds(30L * 24 * 60 * 60);
		gateway.setSubscription(new Subscription(providerId, userId.toString(), "authorized", nextPayment));
		mockMvc.perform(post("/api/v1/billing/webhooks/mercadopago")
						.param("data.id", providerId)
						.header("x-request-id", REQUEST_ID)
						.header("x-signature", signature(providerId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/plans")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentPlanKey").value("plus"));

		gateway.setSubscription(new Subscription(providerId, userId.toString(), "cancelled", null));
		mockMvc.perform(post("/api/v1/billing/cancel")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cancellationScheduled").value(true));

		mockMvc.perform(get("/api/v1/plans")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentPlanKey").value("plus"));
	}

	@Test
	void invalidWebhookSignatureDoesNotChangeThePlan() throws Exception {
		mockMvc.perform(post("/api/v1/billing/checkout")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"planKey\":\"ultra\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/billing/webhooks/mercadopago")
						.param("data.id", providerId)
						.header("x-request-id", REQUEST_ID)
						.header("x-signature", "ts=" + TIMESTAMP + ",v1=" + "0".repeat(64))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void developmentOnlyTestSubscriptionEndpointIsUnavailableOutsideDevProfile() throws Exception {
		mockMvc.perform(post("/api/v1/billing/test/activate")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"planKey\":\"plus\"}"))
				.andExpect(status().isNotFound());
	}

	private String signature(String dataId) throws Exception {
		String manifest = "id:" + dataId.toLowerCase()
				+ ";request-id:" + REQUEST_ID
				+ ";ts:" + TIMESTAMP + ";";
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec("test-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return "ts=" + TIMESTAMP + ",v1=" + HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	static class RecordingMercadoPagoGateway implements MercadoPagoGateway {

		private final AtomicReference<Subscription> subscription = new AtomicReference<>();
		private UUID createdForUser;
		private String providerId;

		@Override
		public Checkout createSubscription(UUID userId, String email, String planName, long amountCents) {
			createdForUser = userId;
			return new Checkout(providerId, "https://www.mercadopago.com.br/subscriptions/checkout/test");
		}

		@Override
		public Subscription getSubscription(String subscriptionId) {
			return subscription.get();
		}

		@Override
		public Subscription cancelSubscription(String subscriptionId) {
			return subscription.get();
		}

		void setSubscription(Subscription value) {
			subscription.set(value);
		}

		void setProviderId(String value) {
			providerId = value;
		}

		UUID createdForUser() {
			return createdForUser;
		}
	}

	@TestConfiguration
	static class GatewayTestConfiguration {

		@Bean
		@Primary
		RecordingMercadoPagoGateway recordingMercadoPagoGateway() {
			return new RecordingMercadoPagoGateway();
		}
	}
}
