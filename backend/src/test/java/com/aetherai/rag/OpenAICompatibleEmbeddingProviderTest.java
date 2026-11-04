package com.aetherai.rag;

import com.aetherai.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAICompatibleEmbeddingProviderTest {

	@Test
	void sendsTextToConfiguredProviderAndValidatesVectorDimensions() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAICompatibleEmbeddingProvider provider = new OpenAICompatibleEmbeddingProvider(
				builder,
				new EmbeddingProperties("https://embeddings.example/v1", "local-test-key", "test-embedding", 1536));
		String values = java.util.stream.IntStream.range(0, 1536)
				.mapToObj(index -> "0.25")
				.collect(java.util.stream.Collectors.joining(","));
		server.expect(requestTo("https://embeddings.example/v1/embeddings"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer local-test-key"))
				.andExpect(content().json("""
						{"model":"test-embedding","input":"AETHER"}
						"""))
				.andRespond(withSuccess(
						"{\"data\":[{\"embedding\":[" + values + "]}]}",
						MediaType.APPLICATION_JSON));

		float[] vector = provider.embed("AETHER");

		assertEquals(1536, vector.length);
		assertEquals(0.25f, vector[0]);
		server.verify();
	}

	@Test
	void rejectsVectorsWithIncorrectDimensions() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAICompatibleEmbeddingProvider provider = new OpenAICompatibleEmbeddingProvider(
				builder,
				new EmbeddingProperties("https://embeddings.example/v1", "local-test-key", "test-embedding", 1536));
		server.expect(requestTo("https://embeddings.example/v1/embeddings"))
				.andRespond(withSuccess(
						"{\"data\":[{\"embedding\":[0.1]}]}",
						MediaType.APPLICATION_JSON));

		ApiException exception = assertThrows(ApiException.class, () -> provider.embed("AETHER"));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
		server.verify();
	}
}
