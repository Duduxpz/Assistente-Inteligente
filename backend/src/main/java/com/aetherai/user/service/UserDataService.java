package com.aetherai.user.service;

import com.aetherai.auth.service.AuthService;
import com.aetherai.billing.BillingProperties;
import com.aetherai.billing.BillingService;
import com.aetherai.billing.SubscriptionEntity;
import com.aetherai.billing.SubscriptionRepository;
import com.aetherai.billing.dto.BillingStatusResponse;
import com.aetherai.common.exception.ApiException;
import com.aetherai.conversation.dto.ConversationResponse;
import com.aetherai.conversation.repository.ConversationRepository;
import com.aetherai.document.dto.DocumentResponse;
import com.aetherai.document.entity.DocumentEntity;
import com.aetherai.document.repository.DocumentRepository;
import com.aetherai.document.service.DocumentService;
import com.aetherai.message.dto.MessageResponse;
import com.aetherai.message.repository.MessageRepository;
import com.aetherai.task.service.TaskService;
import com.aetherai.usage.service.UsageService;
import com.aetherai.user.dto.UserDataExport;
import com.aetherai.user.dto.UserResponse;
import com.aetherai.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class UserDataService {

	private final AuthService authService;
	private final UserSettingsService settingsService;
	private final UserRepository userRepository;
	private final TaskService taskService;
	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final DocumentRepository documentRepository;
	private final DocumentService documentService;
	private final UsageService usageService;
	private final SubscriptionRepository subscriptionRepository;
	private final BillingService billingService;
	private final BillingProperties billingProperties;
	private final ObjectMapper objectMapper;

	public UserDataService(
			AuthService authService,
			UserSettingsService settingsService,
			UserRepository userRepository,
			TaskService taskService,
			ConversationRepository conversationRepository,
			MessageRepository messageRepository,
			DocumentRepository documentRepository,
			DocumentService documentService,
			UsageService usageService,
			SubscriptionRepository subscriptionRepository,
			BillingService billingService,
			BillingProperties billingProperties,
			ObjectMapper objectMapper) {
		this.authService = authService;
		this.settingsService = settingsService;
		this.userRepository = userRepository;
		this.taskService = taskService;
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.documentRepository = documentRepository;
		this.documentService = documentService;
		this.usageService = usageService;
		this.subscriptionRepository = subscriptionRepository;
		this.billingService = billingService;
		this.billingProperties = billingProperties;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public void writeExport(UUID userId, OutputStream outputStream) throws IOException {
		UserResponse user = authService.getCurrentUser(userId);
		List<DocumentEntity> documents = documentRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
		UserDataExport export = new UserDataExport(
				Instant.now(),
				user,
				settingsService.get(userId),
				billingStatus(userId),
				taskService.list(userId),
				conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
						.map(conversation -> new UserDataExport.ArchivedConversation(
								ConversationResponse.from(conversation),
								messageRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId())
										.stream()
										.map(MessageResponse::from)
										.toList()))
						.toList(),
				documents.stream().map(DocumentResponse::from).toList(),
				usageService.forUser(userId));

		try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
			zip.putNextEntry(new ZipEntry("account.json"));
			objectMapper.writeValue(zip, export);
			zip.closeEntry();

			for (DocumentEntity document : documents) {
				String entryName = "documents/" + document.getId() + "-" + safeArchiveName(document.getFileName());
				zip.putNextEntry(new ZipEntry(entryName));
				documentService.copyTo(userId, document.getId(), zip);
				zip.closeEntry();
			}
			zip.finish();
		}
	}

	@Transactional
	public void deleteAccount(UUID userId) {
		if (!userRepository.existsById(userId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "User not found.");
		}
		billingService.cancelForAccountDeletion(userId);
		for (DocumentResponse document : documentService.list(userId)) {
			documentService.delete(userId, document.id());
		}
		userRepository.deleteById(userId);
		userRepository.flush();
	}

	private BillingStatusResponse billingStatus(UUID userId) {
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
		SubscriptionEntity subscription = subscriptionRepository.findById(userId).orElse(null);
		Instant periodEnd = subscription == null ? null : subscription.getCurrentPeriodEnd();
		boolean cancellationScheduled = subscription != null
				&& "cancelled".equals(subscription.getStatus())
				&& periodEnd != null
				&& periodEnd.isAfter(Instant.now());
		boolean testSubscription = subscription != null
				&& List.of("authorized", "cancelled").contains(subscription.getStatus())
				&& subscription.getProviderSubscriptionId() == null;
		return new BillingStatusResponse(
				user.getPlanKey(),
				subscription == null ? null : subscription.getStatus(),
				periodEnd,
				cancellationScheduled,
				billingProperties.testModeEnabled(),
				testSubscription);
	}

	private String safeArchiveName(String originalName) {
		return originalName.replace('\\', '_').replace('/', '_');
	}
}
