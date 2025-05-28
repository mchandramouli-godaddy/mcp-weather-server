package com.godaddy.examples.mcp.weather.exceptions;

/**
 * Custom exception for weather server operations.
 * Provides better error handling and messaging for weather-related failures.
 */
public class WeatherServerException extends RuntimeException {
    
    private final String errorCode;
    private final String operation;
    
    /**
     * Constructor with message only.
     */
    public WeatherServerException(String message) {
        super(message);
        this.errorCode = "WEATHER_ERROR";
        this.operation = "UNKNOWN";
    }
    
    /**
     * Constructor with message and cause.
     */
    public WeatherServerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WEATHER_ERROR";
        this.operation = "UNKNOWN";
    }
    
    /**
     * Constructor with message, operation, and cause.
     */
    public WeatherServerException(String message, String operation, Throwable cause) {
        super(message, cause);
        this.errorCode = "WEATHER_ERROR";
        this.operation = operation;
    }
    
    /**
     * Constructor with message, error code, and operation.
     */
    public WeatherServerException(String message, String errorCode, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.operation = operation;
    }
    
    /**
     * Full constructor with all parameters.
     */
    public WeatherServerException(String message, String errorCode, String operation, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = operation;
    }
    
    /**
     * Get the error code associated with this exception.
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get the operation that was being performed when this exception occurred.
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Get a formatted error message including error code and operation.
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s (Operation: %s)", errorCode, getMessage(), operation);
    }
    
    @Override
    public String toString() {
        return String.format("WeatherServerException{errorCode='%s', operation='%s', message='%s'}", 
            errorCode, operation, getMessage());
    }
}