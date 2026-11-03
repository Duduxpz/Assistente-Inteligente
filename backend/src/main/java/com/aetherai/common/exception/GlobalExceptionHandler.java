package com.aetherai.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
		return response(exception.getStatus(), exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparing(FieldError::getField))
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return response(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleIntegrityViolation(
			DataIntegrityViolationException exception,
			HttpServletRequest request) {
		Throwable cause = exception.getMostSpecificCause();
		String constraint = cause instanceof ConstraintViolationException violation
				? violation.getConstraintName()
				: null;
		logger.warn("Data integrity conflict for {} (cause={}, constraint={})",
				request.getRequestURI(),
				cause.getClass().getSimpleName(),
				constraint == null ? "unknown" : constraint);
		return response(HttpStatus.CONFLICT, "The request conflicts with existing data.", request);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	ResponseEntity<ApiError> handleMalformedRequest(Exception exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "The request contains invalid or malformed data.", request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> handleAccessDenied(HttpServletRequest request) {
		return response(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.", request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> handleNotFound(HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, "The requested resource was not found.", request);
	}

	private ResponseEntity<ApiError> response(
			HttpStatus status,
			String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status)
				.body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
	}
}
