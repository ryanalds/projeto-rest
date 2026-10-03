package edu.ifrn.apigateway.common.error;

public class BackendTimeoutException extends RuntimeException {
    public BackendTimeoutException(String message, Throwable cause) { super(message, cause); }
}
