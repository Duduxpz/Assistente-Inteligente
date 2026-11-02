package com.aetherai.ai;

import com.aetherai.ai.entity.AIModelEntity;
import com.aetherai.ai.repository.AIModelRepository;
import com.aetherai.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openrouter")
public class OpenRouterAIProvider implements AIProvider {

	private static final Logger logger = LoggerFactory.getLogger(OpenRouterAIProvider.class);
	private static final String PROVIDER = "openrouter";

	private final RestClient restClient;
	private final AIModelRepository modelRepository;
	private final ObjectMapper objectMapper;
	private final String modelKey;

	public OpenRouterAIProvider(
			RestClient.Builder restClientBuilder,
			AIModelRepository modelRepository,
			ObjectMapper objectMapper,
			AIProviderProperties properties) {
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			throw new IllegalStateException(
					"AI_PROVIDER is set to openrouter, but OPENROUTER_API_KEY is missing.");
		}
		if (properties.model() == null || properties.model().isBlank()) {
			throw new IllegalStateException("OPENROUTER_MODEL must not be blank.");
		}
		this.restClient = restClientBuilder
				.baseUrl("https://openrouter.ai/api/v1")
				.defaultHeader("Authorization", "Bearer " + properties.apiKey())
				.defaultHeader("HTTP-Referer", properties.siteUrl())
				.defaultHeader("X-Title", "AETHER AI")
				.build();
		this.modelRepository = modelRepository;
		this.objectMapper = objectMapper;
		this.modelKey = properties.model();
	}

	@Override
	public AIResponse complete(List<ChatTurn> conversation) {
		List<Map<String, String>> messages = conversation.stream()
				.map(turn -> Map.of("role", turn.role(), "content", turn.content()))
				.toList();

		JsonNode response;
		try {
			response = restClient.post()
					.uri("/chat/completions")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("model", modelKey, "messages", messages))
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientResponseException exception) {
			int status = exception.getStatusCode().value();
			logger.warn("OpenRouter returned HTTP {}", status);
			HttpStatus responseStatus = status == 429
					? HttpStatus.SERVICE_UNAVAILABLE
					: HttpStatus.BAD_GATEWAY;
			throw new ApiException(responseStatus, "The AI provider could not complete the request.");
		} catch (ResourceAccessException exception) {
			logger.warn("OpenRouter is unreachable: {}", exception.getMessage());
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The AI provider is temporarily unavailable.");
		} catch (RestClientException exception) {
			logger.warn("Could not read the OpenRouter response.", exception);
			throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI provider returned an invalid response.");
		}

		JsonNode contentNode = response == null
				? null
				: response.path("choices").path(0).path("message").path("content");
		if (contentNode == null || !contentNode.isTextual() || contentNode.asText().isBlank()) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI provider returned an empty response.");
		}

		String responseModel = response.path("model").asText(modelKey);
		AIModelEntity model = modelRepository.findByProviderAndModelKey(PROVIDER, responseModel)
				.orElseGet(() -> modelRepository.save(
						new AIModelEntity(PROVIDER, responseModel, responseModel, true)));
		JsonNode usage = response.path("usage");

		return new AIResponse(
				contentNode.asText(),
				model.getId(),
				nullableTokenCount(usage.path("prompt_tokens")),
				nullableTokenCount(usage.path("completion_tokens")));
	}

	private Integer nullableTokenCount(JsonNode tokenCount) {
		return tokenCount.isIntegralNumber() && tokenCount.canConvertToInt()
				? tokenCount.intValue()
				: null;
	}
}
