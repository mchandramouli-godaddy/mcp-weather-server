package com.godaddy.examples.mcp.weather.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherServerException Tests")
class WeatherServerExceptionTest {

    @Test
    @DisplayName("Should create exception with message only")
    void constructor_MessageOnly_SetsDefaultValues() {
        // Given
        String message = "Test error message";

        // When
        WeatherServerException exception = new WeatherServerException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getErrorCode()).isEqualTo("WEATHER_ERROR");
        assertThat(exception.getOperation()).isEqualTo("UNKNOWN");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void constructor_MessageAndCause_SetsDefaultValues() {
        // Given
        String message = "Test error message";
        RuntimeException cause = new RuntimeException("Root cause");

        // When
        WeatherServerException exception = new WeatherServerException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getErrorCode()).isEqualTo("WEATHER_ERROR");
        assertThat(exception.getOperation()).isEqualTo("UNKNOWN");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should create exception with message, operation, and cause")
    void constructor_MessageOperationAndCause_SetsValues() {
        // Given
        String message = "Test error message";
        String operation = "testOperation";
        RuntimeException cause = new RuntimeException("Root cause");

        // When
        WeatherServerException exception = new WeatherServerException(message, operation, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getErrorCode()).isEqualTo("WEATHER_ERROR");
        assertThat(exception.getOperation()).isEqualTo(operation);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should create exception with message, error code, and operation")
    void constructor_MessageErrorCodeAndOperation_SetsValues() {
        // Given
        String message = "Test error message";
        String errorCode = "TEST_ERROR";
        String operation = "testOperation";

        // When
        WeatherServerException exception = new WeatherServerException(message, errorCode, operation);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getOperation()).isEqualTo(operation);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("Should create exception with all parameters")
    void constructor_AllParameters_SetsAllValues() {
        // Given
        String message = "Test error message";
        String errorCode = "TEST_ERROR";
        String operation = "testOperation";
        RuntimeException cause = new RuntimeException("Root cause");

        // When
        WeatherServerException exception = new WeatherServerException(message, errorCode, operation, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getOperation()).isEqualTo(operation);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should format message correctly")
    void getFormattedMessage_ReturnsFormattedString() {
        // Given
        String message = "Test error message";
        String errorCode = "TEST_ERROR";
        String operation = "testOperation";
        WeatherServerException exception = new WeatherServerException(message, errorCode, operation);

        // When
        String formattedMessage = exception.getFormattedMessage();

        // Then
        assertThat(formattedMessage).isEqualTo("[TEST_ERROR] Test error message (Operation: testOperation)");
    }

    @Test
    @DisplayName("Should override toString correctly")
    void toString_ReturnsFormattedString() {
        // Given
        String message = "Test error message";
        String errorCode = "TEST_ERROR";
        String operation = "testOperation";
        WeatherServerException exception = new WeatherServerException(message, errorCode, operation);

        // When
        String toString = exception.toString();

        // Then
        assertThat(toString).isEqualTo("WeatherServerException{errorCode='TEST_ERROR', operation='testOperation', message='Test error message'}");
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void constructor_NullMessage_HandlesGracefully() {
        // Given
        String errorCode = "TEST_ERROR";
        String operation = "testOperation";

        // When
        WeatherServerException exception = new WeatherServerException(null, errorCode, operation);

        // Then
        assertThat(exception.getMessage()).isNull();
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getOperation()).isEqualTo(operation);
        assertThat(exception.getFormattedMessage()).contains("null");
        assertThat(exception.toString()).contains("null");
    }

    @Test
    @DisplayName("Should be instance of RuntimeException")
    void exception_IsRuntimeException() {
        // Given
        WeatherServerException exception = new WeatherServerException("Test message");

        // When & Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should preserve original exception hierarchy")
    void exception_PreservesExceptionHierarchy() {
        // Given
        RuntimeException originalCause = new IllegalArgumentException("Original cause");
        WeatherServerException exception = new WeatherServerException("Test message", originalCause);

        // When & Then
        assertThat(exception.getCause()).isEqualTo(originalCause);
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
    }
}