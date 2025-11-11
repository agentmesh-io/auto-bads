package com.therighthandapp.autobads.integration;

/**
 * Exception thrown when integration with AgentMesh fails
 */
public class IntegrationException extends RuntimeException {
    
    public IntegrationException(String message) {
        super(message);
    }
    
    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
