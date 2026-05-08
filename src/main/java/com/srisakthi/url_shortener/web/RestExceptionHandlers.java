package com.srisakthi.url_shortener.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.srisakthi.url_shortener.web.api.BadRequestException;
import com.srisakthi.url_shortener.web.api.ConflictException;
import com.srisakthi.url_shortener.web.api.GoneException;
import com.srisakthi.url_shortener.web.api.NotFoundException;

@RestControllerAdvice
public class RestExceptionHandlers {

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ProblemDetail> badRequest(BadRequestException e) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
		return ResponseEntity.badRequest().body(p);
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ProblemDetail> notFound(NotFoundException e) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "not found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(p);
	}

	@ExceptionHandler(GoneException.class)
	public ResponseEntity<ProblemDetail> gone(GoneException e) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, "expired");
		return ResponseEntity.status(HttpStatus.GONE).body(p);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ProblemDetail> conflict(ConflictException e) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(p);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
		String msg = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(err -> err.getField() + " " + err.getDefaultMessage())
				.orElse("validation error");
		ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
		return ResponseEntity.badRequest().body(p);
	}

}
