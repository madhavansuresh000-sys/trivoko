package com.trivoko.common;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 401 and 403 answers from the security filters, in the same problem-details JSON as
 * GlobalExceptionHandler, so the React app can always show a clear message.
 *   401 Unauthorized = "who are you?"  (not logged in)
 *   403 Forbidden    = "I know you, but you may not do this"
 * (Copied from EventHub.)
 */
public final class SecurityProblems {

	private SecurityProblems() {
	}

	public static AuthenticationEntryPoint notLoggedIn() {
		return (request, response, ex) -> write(response, HttpStatus.UNAUTHORIZED, "Please log in first.");
	}

	public static AccessDeniedHandler forbidden() {
		return (request, response, ex) -> write(response, HttpStatus.FORBIDDEN,
				ex instanceof CsrfException
						? "Security check failed (missing CSRF token). Reload the page and try again."
						: "You do not have permission to do this.");
	}

	/** The messages are fixed texts without quotes, so plain string building is safe here. */
	static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase()
				+ "\",\"status\":" + status.value() + ",\"detail\":\"" + detail + "\"}");
	}

}
