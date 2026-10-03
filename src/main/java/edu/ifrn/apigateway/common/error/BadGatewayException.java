package edu.ifrn.apigateway.common.error;

public class BadGatewayException extends RuntimeException {
    public BadGatewayException(String message, Throwable cause) { super(message, cause); }
    public BadGatewayException(String message) { super(message); }
}
