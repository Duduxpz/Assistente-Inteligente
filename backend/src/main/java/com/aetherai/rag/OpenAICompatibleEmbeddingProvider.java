package com.aetherai.rag;

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

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
public class OpenAICompatibleEmbeddingProvider implements EmbeddingProvider {

	private static final int VECTOR_DIMENSIONS = 1536;
	private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleEmbeddingProvider.class);

	private final RestClient restClient;
	private final EmbeddingProperties properties;

	public OpenAICompatibleEmbeddingProvider(
			RestClient.Builder builder,
			EmbeddingProperties properties) {
		if (properties.baseUrl() == null || properties.baseUrl().isBlank()
				|| properties.apiKey() == null || properties.apiKey().isBlank()
				|| properties.model() == null || properties.model().isBlank()
				|| properties.dimensions() != VECTOR_DIMENSIONS) {
			throw new IllegalStateException(
					"RAG requires an embedding provider configured for 1536 dimensions.");
		}
		this.restClient = builder
				.baseUrl(properties.baseUrl())
				.defaultHeader("Authorization", "Bearer " + properties.apiKey())
				.build();
		this.properties = properties;
	}

	@Override
	public float[] embed(String text) {
		try {
			JsonNode response = restClient.post()
					.uri("/embeddings")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("model", properties.model(), "input", text))
					.retrieve()
					.body(JsonNode.class);
			JsonNode values = response == null ? null : response.path("data").path(0).path("embedding");
			if (values == null || !values.isArray() || values.size() != properties.dimensions()) {
				throw new ApiException(HttpStatus.BAD_GATEWAY, "The embedding provider returned an invalid vector.");
			}
			float[] vector = new float[values.size()];
			for (int index = 0; index < vector.length; index++) {
				JsonNode value = values.get(index);
				if (value == null || !value.isNumber() || !Float.isFinite(value.floatValue())) {
					throw new ApiException(HttpStatus.BAD_GATEWAY, "The embedding provider returned an invalid vector.");
				}
				vector[index] = value.floatValue();
			}
			return vector;
		} catch (RestClientResponseException exception) {
			logger.warn("Embedding provider returned HTTP {}", exception.getStatusCode().value());
			throw new ApiException(HttpStatus.BAD_GATEWAY, "The embedding provider could not process the request.");
		} catch (ResourceAccessException exception) {
			logger.warn("Embedding provider is unreachable: {}", exception.getMessage());
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The embedding provider is temporarily unavailable.");
		} catch (RestClientException exception) {
			logger.warn("Could not read the embedding response.", exception);
			throw new ApiException(HttpStatus.BAD_GATEWAY, "The embedding provider returned an invalid response.");
		}
	}
}
