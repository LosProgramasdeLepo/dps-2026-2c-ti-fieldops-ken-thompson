package edu.itba.fieldops.api.error;

import edu.itba.fieldops.domain.expedition.ExpeditionNotApprovable;
import edu.itba.fieldops.domain.expedition.InvalidAssignment;
import edu.itba.fieldops.domain.expedition.InvalidExpeditionTransition;
import edu.itba.fieldops.domain.itinerary.InvalidItinerary;
import edu.itba.fieldops.domain.shared.DomainException;
import edu.itba.fieldops.domain.shared.InvalidValue;
import edu.itba.fieldops.domain.shared.UnknownResource;
import edu.itba.fieldops.domain.tracking.InvalidActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestControllerAdvice
public final class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UnknownResource.class)
    ResponseEntity<ProblemDetail> unknown(UnknownResource error, HttpServletRequest request) {
        if (pathContains(request, error.id())) {
            return problem(HttpStatus.NOT_FOUND, "Not Found", error.getMessage());
        }
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", error.getMessage());
    }

    @ExceptionHandler({InvalidValue.class, InvalidItinerary.class, InvalidAssignment.class})
    ResponseEntity<ProblemDetail> rejected(DomainException error) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", error.getMessage());
    }

    @ExceptionHandler({InvalidExpeditionTransition.class, ExpeditionNotApprovable.class, InvalidActivityExecution.class})
    ResponseEntity<ProblemDetail> conflict(DomainException error) {
        return problem(HttpStatus.CONFLICT, "Conflict", error.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> domain(DomainException error) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", error.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception error) {
        log.error("Unhandled failure", error);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected failure");
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException error,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.debug("Unreadable request", error);
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request could not be read"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException error,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Bad Request");
        problem.setProperty("errors", error.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .toList());
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    private static boolean pathContains(HttpServletRequest request, String id) {
        Object raw = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(raw instanceof Map<?, ?> variables)) {
            return false;
        }
        return variables.values().stream().anyMatch(value -> id.equalsIgnoreCase(String.valueOf(value)));
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
