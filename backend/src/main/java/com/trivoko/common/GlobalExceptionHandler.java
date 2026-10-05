package com.trivoko.common;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.trivoko.auth.TooManyLoginAttemptsException;

/**
 * Turns every error into clean JSON (RFC 9457 "problem details"), e.g.
 *
 * <pre>
 * { "status": 400, "title": "Bad Request", "detail": "Validation failed",
 *   "errors": { "price": "price must be greater than 0" } }
 * </pre>
 *
 * The parent class already handles Spring's own errors (bad JSON, wrong HTTP method,
 * unknown URL ...) in the same format. (Copied from EventHub; login errors are added in Phase 2.)
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** @Valid failed: list every bad field and its message. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new TreeMap<>();
		ex.getBindingResult().getFieldErrors()
			.forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	/**
	 * A URL that does not exist, e.g. /api/nope (spec section 8). Spring's own text ("No static resource
	 * api/nope.") would confuse a frontend developer, so say plainly what happened.
	 */
	@Override
	protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String path = ex.getResourcePath().startsWith("/") ? ex.getResourcePath() : "/" + ex.getResourcePath();
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "There is no API at " + path));
	}

	/** A query value of the wrong type, e.g. page=abc or min=ten. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Invalid value '" + ex.getValue() + "' for '" + ex.getName() + "'");
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail handleBadRequest(BadRequestException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	/** A feature needs keys or a service that is not set up on this computer/server yet. */
	@ExceptionHandler(ServiceNotConfiguredException.class)
	ProblemDetail handleNotConfigured(ServiceNotConfiguredException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
	}

	/** Two people saved the same row at the same moment (@Version). */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"This record was changed by someone else. Please reload and try again.");
	}

	/** Login failed. Same message for "no such email" and "wrong password" (do not reveal which emails exist). */
	@ExceptionHandler(BadCredentialsException.class)
	ProblemDetail handleBadCredentials(BadCredentialsException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Wrong email or password.");
	}

	/** The admin blocked this account. */
	@ExceptionHandler(DisabledException.class)
	ProblemDetail handleDisabled(DisabledException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "This account is blocked. Please contact TriVoKo support.");
	}

	/** 429 + Retry-After header (seconds), the standard way to say "slow down". */
	@ExceptionHandler(TooManyLoginAttemptsException.class)
	ResponseEntity<ProblemDetail> handleTooManyLogins(TooManyLoginAttemptsException ex) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfter().toSeconds()))
			.body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
	}

	/** @PreAuthorize said no. Without this, the catch-all below would turn it into a 500. */
	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You do not have permission to do this.");
	}

	/** Anything unexpected: log the details, but never send the stack trace to the user. */
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Something went wrong on our side. Please try again later.");
	}

}
