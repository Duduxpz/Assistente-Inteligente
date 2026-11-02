package com.aetherai.common.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApiException(ApiException exception) {
		return error(exception.getStatus(), exception.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ApiError> handleBadCredentials() {
		return error(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Os dados enviados são inválidos.");
		return error(HttpStatus.BAD_REQUEST, message);
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message));
	}
}
