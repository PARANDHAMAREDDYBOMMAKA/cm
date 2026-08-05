package com.claimguard.web;

import com.claimguard.claim.ClaimNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onInvalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        exception.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "Some fields did not pass validation.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail onConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.putIfAbsent(String.valueOf(violation.getPropertyPath()), violation.getMessage());
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "Some values did not pass validation.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ProblemDetail onUnreadableRequest(Exception exception) {
        log.debug("Rejected malformed request", exception);
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The request could not be read.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail onUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
                "This endpoint does not accept that content type.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail onUploadTooLarge(MaxUploadSizeExceededException exception) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "File too large",
                "The uploaded file exceeds the maximum allowed size.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Rejected request that violated a database constraint", exception);
        return problem(HttpStatus.CONFLICT, "Conflict",
                "That change conflicts with data that already exists.");
    }

    @ExceptionHandler(ClaimNotFoundException.class)
    ProblemDetail onNotFound(ClaimNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Not found", "No such claim or document.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail onAccessDenied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "You are not allowed to perform that action.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail onResponseStatus(ResponseStatusException exception) {
        HttpStatusCode status = exception.getStatusCode();
        String reason = exception.getReason();
        return problem(status, HttpStatus.valueOf(status.value()).getReasonPhrase(),
                reason != null ? reason : "The request could not be completed.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpected(Exception exception) {
        log.error("Unhandled exception while serving a request", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "Something went wrong while handling the request.");
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        return problem;
    }
}
