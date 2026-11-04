package com.aetherai.conversation.controller;

import com.aetherai.ai.dto.ChatMessageRequest;
import com.aetherai.ai.dto.ChatMessageResponse;
import com.aetherai.conversation.dto.ConversationRequest;
import com.aetherai.conversation.dto.ConversationResponse;
import com.aetherai.conversation.service.ConversationService;
import com.aetherai.message.dto.MessageResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

	private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);
	private final ConversationService conversationService;

	public ConversationController(ConversationService conversationService) {
		this.conversationService = conversationService;
	}

	@GetMapping
	public List<ConversationResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return conversationService.list(userId(jwt));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ConversationResponse create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ConversationRequest request) {
		return conversationService.create(userId(jwt), request);
	}

	@PutMapping("/{conversationId}")
	public ConversationResponse rename(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID conversationId,
			@Valid @RequestBody ConversationRequest request) {
		return conversationService.rename(userId(jwt), conversationId, request);
	}

	@GetMapping("/{conversationId}/messages")
	public List<MessageResponse> listMessages(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID conversationId) {
		return conversationService.listMessages(userId(jwt), conversationId);
	}

	@DeleteMapping("/{conversationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID conversationId) {
		conversationService.delete(userId(jwt), conversationId);
	}

	@PostMapping("/{conversationId}/messages")
	public ChatMessageResponse sendMessage(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID conversationId,
			@Valid @RequestBody ChatMessageRequest request) {
		return conversationService.sendMessage(userId(jwt), conversationId, request.content().trim());
	}

	@PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamMessage(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID conversationId,
			@Valid @RequestBody ChatMessageRequest request) {
		UUID ownerId = userId(jwt);
		return streamResponse(
				conversationId,
				onToken -> conversationService.streamMessage(
						ownerId,
						conversationId,
						request.content().trim(),
						onToken));
	}

	@PostMapping(
			value = "/{conversationId}/messages/{messageId}/regenerate/stream",
			produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter regenerateMessage(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID conversationId,
			@PathVariable UUID messageId) {
		UUID ownerId = userId(jwt);
		return streamResponse(
				conversationId,
				onToken -> conversationService.regenerateLastResponse(
						ownerId,
						conversationId,
						messageId,
						onToken));
	}

	private SseEmitter streamResponse(
			UUID conversationId,
			Function<Consumer<String>, ChatMessageResponse> completion) {
		SseEmitter emitter = new SseEmitter(180_000L);
		AtomicBoolean cancelled = new AtomicBoolean();
		emitter.onCompletion(() -> cancelled.set(true));
		emitter.onTimeout(() -> {
			cancelled.set(true);
			emitter.complete();
		});
		emitter.onError(ignored -> cancelled.set(true));

		Thread.startVirtualThread(() -> {
			try {
				var result = completion.apply(token -> {
					if (cancelled.get()) {
						throw new IllegalStateException("The client cancelled the stream.");
					}
					try {
						emitter.send(SseEmitter.event().name("token").data(token));
					} catch (java.io.IOException exception) {
						throw new IllegalStateException("Could not send the chat stream.", exception);
					}
				});
				if (!cancelled.get()) {
					emitter.send(SseEmitter.event().name("complete").data(result));
					emitter.complete();
				}
			} catch (RuntimeException exception) {
				if (cancelled.get()) {
					return;
				}
				logger.warn("Chat stream failed for conversation {}.", conversationId, exception);
				String message = exception instanceof com.aetherai.common.exception.ApiException apiException
						? apiException.getMessage()
						: "Não foi possível gerar uma resposta. Tente novamente.";
				try {
					emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
					emitter.complete();
				} catch (java.io.IOException sendException) {
					emitter.completeWithError(sendException);
				}
			} catch (java.io.IOException exception) {
				logger.warn("Could not complete chat stream for conversation {}.", conversationId, exception);
				emitter.completeWithError(exception);
			}
		});
		return emitter;
	}

	private UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
