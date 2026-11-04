package com.aetherai.ai;

import com.aetherai.ai.entity.AIModelEntity;
import com.aetherai.ai.repository.AIModelRepository;
import com.aetherai.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class OpenRouterAIProviderTest {

	private static final String MODEL = "openai/gpt-4o-mini";
	private static final String API_KEY = "local-test-key";

	@Test
	void sendsChatAndReturnsModelUsageAndAssistantContent() {
		AIModelRepository modelRepository = mock(AIModelRepository.class);
		when(modelRepository.findByProviderAndModelKey("openrouter", MODEL))
				.thenReturn(Optional.of(new AIModelEntity("openrouter", MODEL, MODEL, true)));

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenRouterAIProvider provider = new OpenRouterAIProvider(
				builder,
				modelRepository,
				new ObjectMapper(),
				new AIProviderProperties(API_KEY, MODEL, "http://localhost:5173"));

		server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer " + API_KEY))
				.andExpect(header("X-Title", "AETHER AI"))
				.andExpect(content().json("""
						{"model":"openai/gpt-4o-mini","messages":[{"role":"user","content":"Olá"}]}
						"""))
				.andRespond(withSuccess("""
						{
						  "model": "openai/gpt-4o-mini",
						  "choices": [{"message": {"content": "Olá! Como posso ajudar?"}}],
						  "usage": {"prompt_tokens": 4, "completion_tokens": 7}
						}
						""", MediaType.APPLICATION_JSON));

		AIResponse response = provider.complete(List.of(new ChatTurn("user", "Olá")));

		assertEquals("Olá! Como posso ajudar?", response.content());
		assertEquals(4, response.inputTokens());
		assertEquals(7, response.outputTokens());
		verify(modelRepository).findByProviderAndModelKey("openrouter", MODEL);
		server.verify();
	}

	@Test
	void doesNotExposeProviderErrorOrApiKey() {
		AIModelRepository modelRepository = mock(AIModelRepository.class);
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenRouterAIProvider provider = new OpenRouterAIProvider(
				builder,
				modelRepository,
				new ObjectMapper(),
				new AIProviderProperties(API_KEY, MODEL, "http://localhost:5173"));
		server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED)
						.contentType(MediaType.APPLICATION_JSON)
						.body("{\"error\":\"invalid API key\"}"));

		ApiException exception = assertThrows(
				ApiException.class,
				() -> provider.complete(List.of(new ChatTurn("user", "Olá"))));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
		org.junit.jupiter.api.Assertions.assertFalse(exception.getMessage().contains(API_KEY));
		server.verify();
	}

	@Test
	void streamsProviderTokensAndReturnsUsage() {
		AIModelRepository modelRepository = mock(AIModelRepository.class);
		when(modelRepository.findByProviderAndModelKey("openrouter", MODEL))
				.thenReturn(Optional.of(new AIModelEntity("openrouter", MODEL, MODEL, true)));
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenRouterAIProvider provider = new OpenRouterAIProvider(
				builder,
				modelRepository,
				new ObjectMapper(),
				new AIProviderProperties(API_KEY, MODEL, "http://localhost:5173"));
		server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("""
						{"model":"openai/gpt-4o-mini-plus","messages":[{"role":"user","content":"Olá"}],"stream":true,"stream_options":{"include_usage":true}}
						"""))
				.andRespond(withSuccess("""
						data: {"model":"openai/gpt-4o-mini","choices":[{"delta":{"content":"Olá"}}]}

						data: {"choices":[{"delta":{"content":"!"}}]}

						data: {"choices":[],"usage":{"prompt_tokens":4,"completion_tokens":2}}

						data: [DONE]

						""", MediaType.TEXT_EVENT_STREAM));

		AtomicReference<String> streamed = new AtomicReference<>("");
		AIResponse response = provider.stream(
				List.of(new ChatTurn("user", "Olá")),
				token -> streamed.updateAndGet(current -> current + token),
				"openai/gpt-4o-mini-plus");

		assertEquals("Olá!", streamed.get());
		assertEquals("Olá!", response.content());
		assertEquals(4, response.inputTokens());
		assertEquals(2, response.outputTokens());
		server.verify();
	}
}
