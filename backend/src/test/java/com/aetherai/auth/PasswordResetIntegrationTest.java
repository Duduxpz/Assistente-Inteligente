package com.aetherai.auth;

import com.aetherai.auth.service.TransactionalEmailSender;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:aetherai-password-reset;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"app.documents.storage-path=target/test-documents",
		"security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.email.provider=none",
		"ai.provider=none"
})
@AutoConfigureMockMvc
@Import(PasswordResetIntegrationTest.EmailTestConfiguration.class)
class PasswordResetIntegrationTest {

	private static final String ORIGINAL_PASSWORD = "a-secure-test-password-123";
	private static final String RESET_PASSWORD = "a-different-secure-password-456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RecordingEmailSender emailSender;

	private String email;
	private String oldAccessToken;

	@BeforeEach
	void registerUser() throws Exception {
		emailSender.clear();
		email = "reset-" + UUID.randomUUID() + "@example.com";
		MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerRequest(email)))
				.andExpect(status().isCreated())
				.andReturn();
		oldAccessToken = com.jayway.jsonpath.JsonPath.read(
				registration.getResponse().getContentAsString(),
				"$.accessToken");
	}

	@Test
	void passwordResetRevokesOldAccessTokensAndConsumesTheLinkOnce() throws Exception {
		mockMvc.perform(get("/api/v1/auth/password-reset/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.configured").value(true));

		mockMvc.perform(post("/api/v1/auth/password-reset/request")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s"}
								""".formatted(email)))
				.andExpect(status().isAccepted());

		RecordingEmailSender.SentEmail sentEmail = emailSender.latest();
		assertNotNull(sentEmail);
		String token = UriComponentsBuilder.fromUriString(sentEmail.resetUrl())
				.build()
				.getQueryParams()
				.getFirst("token");
		assertNotNull(token);

		mockMvc.perform(post("/api/v1/auth/password-reset/complete")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"token":"%s","newPassword":"%s"}
								""".formatted(token, RESET_PASSWORD)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/users/me")
						.header("Authorization", bearer(oldAccessToken)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginRequest(email, RESET_PASSWORD)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginRequest(email, ORIGINAL_PASSWORD)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/password-reset/complete")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"token":"%s","newPassword":"%s"}
								""".formatted(token, ORIGINAL_PASSWORD)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unknownEmailGetsSameAcceptedResponseWithoutSendingEmail() throws Exception {
		mockMvc.perform(post("/api/v1/auth/password-reset/request")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"missing-%s@example.com"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isAccepted());

		org.junit.jupiter.api.Assertions.assertEquals(0, emailSender.sentCount());
	}

	private String registerRequest(String userEmail) {
		return """
				{"email":"%s","password":"%s","displayName":"Reset User"}
				""".formatted(userEmail, ORIGINAL_PASSWORD);
	}

	private String loginRequest(String userEmail, String password) {
		return """
				{"email":"%s","password":"%s"}
				""".formatted(userEmail, password);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	static class RecordingEmailSender implements TransactionalEmailSender {

		private final AtomicReference<SentEmail> latest = new AtomicReference<>();
		private final java.util.concurrent.atomic.AtomicInteger sentCount = new java.util.concurrent.atomic.AtomicInteger();

		@Override
		public void sendPasswordReset(String recipient, String resetUrl) {
			latest.set(new SentEmail(recipient, resetUrl));
			sentCount.incrementAndGet();
		}

		SentEmail latest() {
			return latest.get();
		}

		int sentCount() {
			return sentCount.get();
		}

		void clear() {
			latest.set(null);
			sentCount.set(0);
		}

		record SentEmail(String recipient, String resetUrl) {
		}
	}

	@TestConfiguration
	static class EmailTestConfiguration {

		@Bean
		@Primary
		RecordingEmailSender recordingEmailSender() {
			return new RecordingEmailSender();
		}
	}
}
