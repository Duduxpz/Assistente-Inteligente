package com.aetherai.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:aetherai-dev-billing;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"app.documents.storage-path=target/test-documents",
		"security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.email.provider=none",
		"ai.provider=none",
		"app.billing.test-ultra-email="
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class DevBillingIntegrationTest {

	private static final String PASSWORD = "a-secure-test-password-123";

	@Autowired
	private MockMvc mockMvc;

	private String email;
	private String token;

	@BeforeEach
	void registerUser() throws Exception {
		email = "dev-billing-" + UUID.randomUUID() + "@example.com";
		MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s","displayName":"Test User"}
								""".formatted(email, PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn();
		token = com.jayway.jsonpath.JsonPath.read(
				registration.getResponse().getContentAsString(),
				"$.accessToken");
	}

	@Test
	void localUserCanActivateAndCancelAPlanWithoutPayment() throws Exception {
		mockMvc.perform(get("/api/v1/billing")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.testModeEnabled").value(true));

		mockMvc.perform(post("/api/v1/billing/test/activate")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"planKey\":\"plus\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planKey").value("plus"))
				.andExpect(jsonPath("$.subscriptionStatus").value("authorized"))
				.andExpect(jsonPath("$.testSubscription").value(true))
				.andExpect(jsonPath("$.currentPeriodEnd").isNotEmpty());

		mockMvc.perform(get("/api/v1/plans")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentPlanKey").value("plus"));

		mockMvc.perform(post("/api/v1/billing/test/cancel")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cancellationScheduled").value(true))
				.andExpect(jsonPath("$.testSubscription").value(true));

		mockMvc.perform(get("/api/v1/plans")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentPlanKey").value("plus"));
	}

	@Test
	void freePlanCannotBeActivatedAsAPaidTestSubscription() throws Exception {
		mockMvc.perform(post("/api/v1/billing/test/activate")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"planKey\":\"free\"}"))
				.andExpect(status().isBadRequest());
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
