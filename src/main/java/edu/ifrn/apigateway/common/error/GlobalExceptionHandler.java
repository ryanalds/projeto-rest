package edu.ifrn.apigateway.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage(), request);
    }

    @ExceptionHandler(BackendUnavailableException.class)
    ResponseEntity<ProblemDetail> unavailable(BackendUnavailableException exception, HttpServletRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Backend indisponível", exception.getMessage(), request);
    }

    @ExceptionHandler(BackendTimeoutException.class)
    ResponseEntity<ProblemDetail> timeout(BackendTimeoutException exception, HttpServletRequest request) {
        return problem(HttpStatus.GATEWAY_TIMEOUT, "Tempo limite excedido", exception.getMessage(), request);
    }

    @ExceptionHandler(BadGatewayException.class)
    ResponseEntity<ProblemDetail> badGateway(BadGatewayException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_GATEWAY, "Resposta inválida do backend", exception.getMessage(), request);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class, IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> badRequest(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage(), request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail,
                                                   HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }
}
