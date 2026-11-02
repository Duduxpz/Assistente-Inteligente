package com.aetherai.auth;

import com.jayway.jsonpath.JsonPath;
import com.aetherai.common.exception.ApiException;
import com.aetherai.plan.service.PlanLimitService;
import com.aetherai.usage.entity.UsageLogEntity;
import com.aetherai.usage.repository.UsageRepository;
import com.aetherai.user.entity.UserEntity;
import com.aetherai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:aetherai-auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"app.documents.storage-path=target/test-documents",
		"security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"ai.provider=none"
})
@AutoConfigureMockMvc
class AuthAndTaskIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PlanLimitService planLimitService;

	@Autowired
	private UsageRepository usageRepository;

	private String firstUserToken;
	private String firstUserId;
	private String firstUserEmail;

	@BeforeEach
	void registerFirstUser() throws Exception {
		firstUserEmail = "first-" + UUID.randomUUID() + "@example.com";
		String response = registerResponse(firstUserEmail);
		firstUserToken = JsonPath.read(response, "$.accessToken");
		firstUserId = JsonPath.read(response, "$.user.id");
	}

	@Test
	void registrationIssuesTokenAndCurrentUserEndpointReturnsSafeProfile() throws Exception {
		assertNotEquals("", firstUserToken);

		mockMvc.perform(get("/api/v1/users/me")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(firstUserId))
				.andExpect(jsonPath("$.email").exists())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void loginRejectsIncorrectPassword() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"missing@example.com","password":"incorrect-password"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password."));
	}

	@Test
	void loginReturnsTokenAndSettingsAreScopedToTheCurrentUser() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"a-secure-test-password-123"}
								""".formatted(firstUserEmail)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());

		mockMvc.perform(get("/api/v1/users/me/settings")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.theme").value("dark"))
				.andExpect(jsonPath("$.locale").value("pt-BR"));

		mockMvc.perform(put("/api/v1/users/me/settings")
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"theme":"system","locale":"en-US"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.theme").value("system"))
				.andExpect(jsonPath("$.locale").value("en-US"));

		mockMvc.perform(put("/api/v1/users/me")
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Updated Name"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Updated Name"));

		mockMvc.perform(get("/api/v1/usage")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totals.requests").value(0));
	}

	@Test
	void regularUserCannotAccessAdminRoutesAndDisabledAccountsLoseAccess() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isForbidden());

		UserEntity user = userRepository.findById(UUID.fromString(firstUserId)).orElseThrow();
		user.setEnabled(false);
		userRepository.save(user);

		mockMvc.perform(get("/api/v1/users/me")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registrationValidatesPasswordAndRejectsDuplicateEmail() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"weak@example.com","password":"short","displayName":"Weak"}
								"""))
				.andExpect(status().isBadRequest());

		String email = "duplicate-" + UUID.randomUUID() + "@example.com";
		register(email);
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerRequest(email)))
				.andExpect(status().isConflict());
	}

	@Test
	void taskCrudIsScopedToAuthenticatedOwner() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/tasks")
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Revisar proposta","description":"Até sexta","priority":"high"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.priority").value("high"))
				.andExpect(jsonPath("$.completed").value(false))
				.andReturn();
		String taskId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		String otherToken = register("other-" + UUID.randomUUID() + "@example.com");

		mockMvc.perform(get("/api/v1/tasks/" + taskId)
						.header("Authorization", bearer(otherToken)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/tasks")
						.header("Authorization", bearer(otherToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(put("/api/v1/tasks/" + taskId)
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Proposta revisada","description":"","priority":"medium","completed":true}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Proposta revisada"))
				.andExpect(jsonPath("$.completed").value(true));

		mockMvc.perform(delete("/api/v1/tasks/" + taskId)
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isNoContent());
	}

	@Test
	void chatFailsClearlyWhenNoAiProviderIsConfigured() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/conversations")
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Planejamento"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String conversationId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Como começo?"}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("The AI provider is not configured."));

		mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void aiStatusReportsWhenNoProviderIsConfigured() throws Exception {
		mockMvc.perform(get("/api/v1/ai/status")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.configured").value(false));
	}

	@Test
	void planCatalogReturnsPricesAndLimitsForTheAuthenticatedUser() throws Exception {
		mockMvc.perform(get("/api/v1/plans")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currentPlanKey").value("free"))
				.andExpect(jsonPath("$.plans[0].key").value("free"))
				.andExpect(jsonPath("$.plans[0].monthlyPriceCents").value(0))
				.andExpect(jsonPath("$.plans[0].monthlyMessages").value(15))
				.andExpect(jsonPath("$.plans[0].activeTaskLimit").value(10))
				.andExpect(jsonPath("$.plans[0].conversationLimit").value(1))
				.andExpect(jsonPath("$.plans[0].documentStorageBytes").value(50L * 1024 * 1024))
				.andExpect(jsonPath("$.plans[1].monthlyPriceCents").value(2500))
				.andExpect(jsonPath("$.plans[1].monthlyMessages").value(250))
				.andExpect(jsonPath("$.plans[1].activeTaskLimit").value(100))
				.andExpect(jsonPath("$.plans[1].documentStorageBytes").value(1024L * 1024 * 1024))
				.andExpect(jsonPath("$.plans[2].key").value("ultra"))
				.andExpect(jsonPath("$.plans[2].monthlyPriceCents").value(10000))
				.andExpect(jsonPath("$.plans[2].monthlyMessages").value(1000))
				.andExpect(jsonPath("$.plans[2].documentStorageBytes").value(5L * 1024 * 1024 * 1024));
	}

	@Test
	void freePlanEnforcesActiveTaskConversationAndMonthlyMessageLimits() throws Exception {
		for (int index = 0; index < 10; index++) {
			mockMvc.perform(post("/api/v1/tasks")
							.header("Authorization", bearer(firstUserToken))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"title":"Tarefa %d"}
									""".formatted(index)))
					.andExpect(status().isCreated());
		}
		mockMvc.perform(post("/api/v1/tasks")
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Tarefa extra"}
								"""))
				.andExpect(status().is(429))
				.andExpect(jsonPath("$.message").value(
						"Você atingiu o limite de tarefas ativas do plano Free. Consulte os planos disponíveis."));

		mockMvc.perform(post("/api/v1/conversations")
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Minha conversa"}
								"""))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/conversations")
						.header("Authorization", bearer(firstUserToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Conversa extra"}
								"""))
				.andExpect(status().is(429));

		UUID userId = UUID.fromString(firstUserId);
		for (int index = 0; index < 15; index++) {
			usageRepository.save(new UsageLogEntity(userId, null, "chat.completion", 1, 1));
		}
		ApiException messageLimit = assertThrows(
				ApiException.class,
				() -> planLimitService.ensureCanSendMessage(userId));
		assertEquals(HttpStatus.TOO_MANY_REQUESTS, messageLimit.getStatus());

		ApiException documentLimit = assertThrows(
				ApiException.class,
				() -> planLimitService.ensureCanStoreDocument(userId, 50L * 1024 * 1024 + 1));
		assertEquals(HttpStatus.TOO_MANY_REQUESTS, documentLimit.getStatus());
	}

	@Test
	void documentUploadAndDownloadAreScopedToTheOwner() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"notes.txt",
				"text/plain",
				"private notes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		MvcResult uploaded = mockMvc.perform(multipart("/api/v1/documents")
						.file(file)
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fileName").value("notes.txt"))
				.andReturn();
		String documentId = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.id");

		String otherToken = register("document-owner-" + UUID.randomUUID() + "@example.com");
		mockMvc.perform(get("/api/v1/documents/" + documentId + "/download")
						.header("Authorization", bearer(otherToken)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/documents/" + documentId + "/download")
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.string("private notes"));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.delete("/api/v1/documents/" + documentId)
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isNoContent());
	}

	@Test
	void documentUploadRejectsContentThatDoesNotMatchItsDeclaredType() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"notes.txt",
				"text/plain",
				new byte[] {0, 1, 2, 3});

		mockMvc.perform(multipart("/api/v1/documents")
						.file(file)
						.header("Authorization", bearer(firstUserToken)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Text files cannot contain binary control characters."));
	}

	private String register(String email) throws Exception {
		return JsonPath.read(registerResponse(email), "$.accessToken");
	}

	private String registerResponse(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerRequest(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return result.getResponse().getContentAsString();
	}

	private String registerRequest(String email) {
		return """
				{"email":"%s","password":"a-secure-test-password-123","displayName":"Test User"}
				""".formatted(email);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
