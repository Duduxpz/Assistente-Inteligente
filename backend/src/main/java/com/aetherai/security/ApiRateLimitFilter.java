package com.aetherai.security;

import com.aetherai.common.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;
	private final boolean enabled;
	private final Duration window;
	private final int loginLimit;
	private final int registerLimit;
	private final int passwordResetLimit;
	private final int chatLimit;
	private final ConcurrentHashMap<String, WindowBucket> buckets = new ConcurrentHashMap<>();
	private final AtomicLong lastCleanup = new AtomicLong();

	public ApiRateLimitFilter(
			ObjectMapper objectMapper,
			@Value("${app.rate-limit.enabled:true}") boolean enabled,
			@Value("${app.rate-limit.window:PT1M}") Duration window,
			@Value("${app.rate-limit.login.max-requests:10}") int loginLimit,
			@Value("${app.rate-limit.register.max-requests:5}") int registerLimit,
			@Value("${app.rate-limit.password-reset.max-requests:5}") int passwordResetLimit,
			@Value("${app.rate-limit.chat.max-requests:30}") int chatLimit) {
		this.objectMapper = objectMapper;
		this.enabled = enabled;
		this.window = window;
		this.loginLimit = loginLimit;
		this.registerLimit = registerLimit;
		this.passwordResetLimit = passwordResetLimit;
		this.chatLimit = chatLimit;
		if (window.isZero() || window.isNegative()
				|| loginLimit < 1 || registerLimit < 1 || passwordResetLimit < 1 || chatLimit < 1) {
			throw new IllegalArgumentException("Rate-limit window and limits must be positive.");
		}
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		EndpointLimit endpoint = endpointLimit(request);
		if (!enabled || endpoint == null) {
			filterChain.doFilter(request, response);
			return;
		}

		long now = System.currentTimeMillis();
		String key = endpoint.name() + ":" + request.getRemoteAddr();
		if (!allow(key, endpoint.limit(), now)) {
			HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
			response.setStatus(status.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			objectMapper.writeValue(response.getOutputStream(), new ApiError(
					Instant.ofEpochMilli(now),
					status.value(),
					status.getReasonPhrase(),
					"Limite de requisições atingido. Tente novamente em instantes.",
					request.getRequestURI()));
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean allow(String key, int limit, long now) {
		AtomicBoolean allowed = new AtomicBoolean();
		long durationMillis = window.toMillis();
		buckets.compute(key, (ignored, current) -> {
			if (current == null || current.expiresAtMillis() <= now) {
				allowed.set(true);
				return new WindowBucket(now + durationMillis, 1);
			}
			if (current.count() >= limit) {
				allowed.set(false);
				return current;
			}
			allowed.set(true);
			return new WindowBucket(current.expiresAtMillis(), current.count() + 1);
		});
		cleanupExpired(now, durationMillis);
		return allowed.get();
	}

	private void cleanupExpired(long now, long durationMillis) {
		long previousCleanup = lastCleanup.get();
		if (previousCleanup + durationMillis <= now && lastCleanup.compareAndSet(previousCleanup, now)) {
			buckets.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
		}
	}

	private EndpointLimit endpointLimit(HttpServletRequest request) {
		if (!"POST".equals(request.getMethod())) {
			return null;
		}
		return switch (request.getRequestURI()) {
			case "/api/v1/auth/login" -> new EndpointLimit("login", loginLimit);
			case "/api/v1/auth/register" -> new EndpointLimit("register", registerLimit);
			case "/api/v1/auth/password-reset/request",
					"/api/v1/auth/password-reset/complete" -> new EndpointLimit("password-reset", passwordResetLimit);
			default -> request.getRequestURI().matches("/api/v1/conversations/[^/]+/messages")
					? new EndpointLimit("chat", chatLimit)
					: null;
		};
	}

	private record EndpointLimit(String name, int limit) {
	}

	private record WindowBucket(long expiresAtMillis, int count) {
	}
}
