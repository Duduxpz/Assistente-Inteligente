package com.aetherai.auth.service;

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

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailSender implements TransactionalEmailSender {

	private static final Logger logger = LoggerFactory.getLogger(ResendEmailSender.class);

	private final RestClient restClient;
	private final String from;

	public ResendEmailSender(EmailProviderProperties properties) {
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			throw new IllegalStateException("EMAIL_PROVIDER is set to resend, but RESEND_API_KEY is missing.");
		}
		if (properties.from() == null || properties.from().isBlank()) {
			throw new IllegalStateException("RESEND_FROM must be a verified sender address.");
		}
		this.restClient = RestClient.builder()
				.baseUrl("https://api.resend.com")
				.defaultHeader("Authorization", "Bearer " + properties.apiKey())
				.build();
		this.from = properties.from();
	}

	@Override
	public void sendPasswordReset(String recipient, String resetUrl) {
		String html = """
				<p>Recebemos um pedido para redefinir a senha da sua conta AETHER AI.</p>
				<p><a href="%s">Criar uma nova senha</a></p>
				<p>O link expira em 30 minutos. Se você não fez este pedido, ignore esta mensagem.</p>
				""".formatted(resetUrl);
		try {
			restClient.post()
					.uri("/emails")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"from", from,
							"to", recipient,
							"subject", "Redefina sua senha do AETHER AI",
							"html", html))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientResponseException exception) {
			logger.warn("Resend returned HTTP {}", exception.getStatusCode().value());
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "O serviço de e-mail não conseguiu enviar a mensagem.");
		} catch (ResourceAccessException exception) {
			logger.warn("Resend is unreachable: {}", exception.getMessage());
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "O serviço de e-mail está temporariamente indisponível.");
		} catch (RestClientException exception) {
			logger.warn("Could not send the transactional email.", exception);
			throw new ApiException(HttpStatus.BAD_GATEWAY, "O serviço de e-mail retornou uma resposta inválida.");
		}
	}
}
