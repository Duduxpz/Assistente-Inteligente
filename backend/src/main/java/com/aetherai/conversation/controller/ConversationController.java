package com.aetherai.conversation.controller;

import com.aetherai.ai.dto.ChatMessageRequest;
import com.aetherai.ai.dto.ChatMessageResponse;
import com.aetherai.conversation.dto.ConversationRequest;
import com.aetherai.conversation.dto.ConversationResponse;
import com.aetherai.conversation.service.ConversationService;
import com.aetherai.message.dto.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

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

	private UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
