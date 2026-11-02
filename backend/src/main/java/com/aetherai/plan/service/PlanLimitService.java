package com.aetherai.plan.service;

import com.aetherai.common.exception.ApiException;
import com.aetherai.billing.BillingService;
import com.aetherai.plan.PlanTier;
import com.aetherai.task.repository.TaskRepository;
import com.aetherai.conversation.repository.ConversationRepository;
import com.aetherai.document.repository.DocumentRepository;
import com.aetherai.usage.repository.UsageRepository;
import com.aetherai.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class PlanLimitService {

	private final UserRepository userRepository;
	private final TaskRepository taskRepository;
	private final ConversationRepository conversationRepository;
	private final DocumentRepository documentRepository;
	private final UsageRepository usageRepository;
	private final BillingService billingService;

	public PlanLimitService(
			UserRepository userRepository,
			TaskRepository taskRepository,
			ConversationRepository conversationRepository,
			DocumentRepository documentRepository,
			UsageRepository usageRepository,
			BillingService billingService) {
		this.userRepository = userRepository;
		this.taskRepository = taskRepository;
		this.conversationRepository = conversationRepository;
		this.documentRepository = documentRepository;
		this.usageRepository = usageRepository;
		this.billingService = billingService;
	}

	public void ensureCanSendMessage(UUID userId) {
		PlanTier plan = planFor(userId);
		Instant monthStart = YearMonth.now(ZoneOffset.UTC)
				.atDay(1)
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		long used = usageRepository.countByUserIdAndOperationAndCreatedAtGreaterThanEqual(
				userId,
				"chat.completion",
				monthStart);
		if (used >= plan.monthlyMessages()) {
			throw limitReached("Você atingiu o limite mensal de mensagens do plano " + plan.displayName() + ".");
		}
	}

	public void ensureCanCreateTask(UUID userId) {
		PlanTier plan = planFor(userId);
		Integer limit = plan.activeTaskLimit();
		if (limit != null && taskRepository.countByUserIdAndCompletedFalse(userId) >= limit) {
			throw limitReached("Você atingiu o limite de tarefas ativas do plano " + plan.displayName() + ".");
		}
	}

	public void ensureCanReopenTask(UUID userId) {
		ensureCanCreateTask(userId);
	}

	public void ensureCanCreateConversation(UUID userId) {
		PlanTier plan = planFor(userId);
		Integer limit = plan.conversationLimit();
		if (limit != null && conversationRepository.countByUserId(userId) >= limit) {
			throw limitReached("Você atingiu o limite de conversas do plano " + plan.displayName() + ".");
		}
	}

	public void ensureCanStoreDocument(UUID userId, long newFileBytes) {
		PlanTier plan = planFor(userId);
		long storedBytes = documentRepository.totalSizeBytesByUserId(userId);
		if (newFileBytes > plan.documentStorageBytes() - storedBytes) {
			throw limitReached("Você atingiu o limite de armazenamento do plano " + plan.displayName() + ".");
		}
	}

	private PlanTier planFor(UUID userId) {
		billingService.refreshPlanAccess(userId);
		String planKey = userRepository.findById(userId)
				.map(user -> user.getPlanKey())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
		return PlanTier.fromKey(planKey);
	}

	private ApiException limitReached(String message) {
		return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message + " Consulte os planos disponíveis.");
	}
}
