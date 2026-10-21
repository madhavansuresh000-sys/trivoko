package com.trivoko.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** The payment company is down or refused: 503 "please try again" (nothing was held - the order rolled back). */
@RestControllerAdvice
class PaymentProblems {

	@ExceptionHandler(PaymentProviderException.class)
	ProblemDetail handle(PaymentProviderException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
	}

}
