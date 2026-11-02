package com.aetherai.conversation.service;

import com.aetherai.ai.AIProvider;
import com.aetherai.ai.AIResponse;
import com.aetherai.ai.ChatTurn;
import com.aetherai.ai.dto.ChatMessageItem;
import com.aetherai.ai.dto.ChatMessageResponse;
import com.aetherai.common.exception.ApiException;
import com.aetherai.conversation.dto.ConversationRequest;
import com.aetherai.conversation.dto.ConversationResponse;
import com.aetherai.conversation.entity.ConversationEntity;
import com.aetherai.conversation.repository.ConversationRepository;
import com.aetherai.message.dto.MessageResponse;
import com.aetherai.message.entity.MessageEntity;
import com.aetherai.message.repository.MessageRepository;
import com.aetherai.usage.service.UsageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final ObjectProvider<AIProvider> aiProvider;
	private final UsageService usageService;

	public ConversationService(
			ConversationRepository conversationRepository,
			MessageRepository messageRepository,
			ObjectProvider<AIProvider> aiProvider,
			UsageService usageService) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.aiProvider = aiProvider;
		this.usageService = usageService;
	}

	@Transactional(readOnly = true)
	public List<ConversationResponse> list(UUID userId) {
		return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
				.map(ConversationResponse::from)
				.toList();
	}

	@Transactional
	public ConversationResponse create(UUID userId, ConversationRequest request) {
		ConversationEntity conversation = conversationRepository.save(
				new ConversationEntity(userId, request.title().trim()));
		return ConversationResponse.from(conversation);
	}

	@Transactional
	public ConversationResponse rename(UUID userId, UUID conversationId, ConversationRequest request) {
		ConversationEntity conversation = getOwnedConversation(userId, conversationId);
		conversation.rename(request.title().trim());
		return ConversationResponse.from(conversationRepository.save(conversation));
	}

	@Transactional
	public void delete(UUID userId, UUID conversationId) {
		conversationRepository.delete(getOwnedConversation(userId, conversationId));
	}

	@Transactional
	public ChatMessageResponse sendMessage(UUID userId, UUID conversationId, String content) {
		ConversationEntity conversation = getOwnedConversation(userId, conversationId);
		AIProvider provider = aiProvider.getIfAvailable();
		if (provider == null) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The AI provider is not configured.");
		}
		List<ChatTurn> turns = messageRepository
				.findAllByConversationIdOrderByCreatedAtAsc(conversationId).stream()
				.map(message -> new ChatTurn(message.getRole(), message.getContent()))
				.collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
		turns.add(new ChatTurn("user", content));
		AIResponse response = provider.complete(List.copyOf(turns));
		usageService.recordCompletion(userId, response);
		MessageEntity userMessage = new MessageEntity(conversationId, "user", content, null, null, null);
		MessageEntity assistantMessage = new MessageEntity(
				conversationId,
				"assistant",
				response.content(),
				response.modelId(),
				response.inputTokens(),
				response.outputTokens());
		messageRepository.saveAll(List.of(userMessage, assistantMessage));
		conversation.rename(conversation.getTitle());
		conversationRepository.save(conversation);
		List<ChatMessageItem> messages = messageRepository
				.findAllByConversationIdOrderByCreatedAtAsc(conversationId).stream()
				.map(message -> new ChatMessageItem(
						message.getId(),
						message.getRole(),
						message.getContent(),
						message.getCreatedAt()))
				.toList();
		return new ChatMessageResponse(messages);
	}

	@Transactional(readOnly = true)
	public List<MessageResponse> listMessages(UUID userId, UUID conversationId) {
		getOwnedConversation(userId, conversationId);
		return messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId).stream()
				.map(MessageResponse::from)
				.toList();
	}

	private ConversationEntity getOwnedConversation(UUID userId, UUID conversationId) {
		return conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found."));
	}
}
