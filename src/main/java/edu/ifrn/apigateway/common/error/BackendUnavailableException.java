package edu.ifrn.apigateway.common.error;

public class BackendUnavailableException extends RuntimeException {
    public BackendUnavailableException(String message, Throwable cause) { super(message, cause); }
}
