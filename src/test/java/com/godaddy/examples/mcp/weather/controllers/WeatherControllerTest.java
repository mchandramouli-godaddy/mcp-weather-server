package com.godaddy.examples.mcp.weather.controllers;

import com.godaddy.examples.mcp.weather.exceptions.WeatherServerException;
import com.godaddy.examples.mcp.weather.services.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherController Tests")
class WeatherControllerTest {

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController weatherController;

    private Map<String, Object> sampleWeatherData;
    private List<Map<String, Object>> sampleForecastData;
    private List<String> sampleCities;

    @BeforeEach
    void setUp() {
        sampleWeatherData = Map.of(
            "location", "41.8781, -87.6298",
            "name", "Tonight",
            "temperature", "32°F",
            "windSpeed", "5 mph",
            "windDirection", "NW",
            "shortForecast", "Partly Cloudy",
            "detailedForecast", "Partly cloudy, with a low around 32...",
            "cityName", "Chicago",
            "coordinates", "41.8781, -87.6298"
        );

        sampleForecastData = List.of(
            Map.of(
                "name", "Tonight",
                "temperature", "32°F",
                "windSpeed", "5 mph",
                "shortForecast", "Partly Cloudy",
                "isDaytime", false
            ),
            Map.of(
                "name", "Tomorrow",
                "temperature", "45°F",
                "windSpeed", "8 mph",
                "shortForecast", "Sunny",
                "isDaytime", true
            )
        );

        sampleCities = List.of("New York", "Los Angeles", "Chicago", "Houston", "Phoenix");
    }

    @Test
    @DisplayName("Should return weather data for valid city")
    void getWeather_ValidCity_ReturnsWeatherData() {
        // Given
        String cityName = "Chicago";
        when(weatherService.getCurrentWeather(cityName)).thenReturn(sampleWeatherData);

        // When
        ResponseEntity<Map<String, Object>> response = weatherController.getWeather(cityName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("cityName", "Chicago");
        assertThat(response.getBody()).containsEntry("temperature", "32°F");
        assertThat(response.getBody()).containsEntry("shortForecast", "Partly Cloudy");
    }

    @Test
    @DisplayName("Should return forecast data for valid city")
    void getForecast_ValidCity_ReturnsForecastData() {
        // Given
        String cityName = "Chicago";
        when(weatherService.getWeatherForecast(cityName, 5)).thenReturn(sampleForecastData);

        // When
        ResponseEntity<List<Map<String, Object>>> response = weatherController.getForecast(cityName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0)).containsEntry("name", "Tonight");
        assertThat(response.getBody().get(1)).containsEntry("name", "Tomorrow");
    }

    @Test
    @DisplayName("Should return supported cities list")
    void getSupportedCities_ReturnsListOfCities() {
        // Given
        when(weatherService.getSupportedCities()).thenReturn(sampleCities);

        // When
        ResponseEntity<List<String>> response = weatherController.getSupportedCities();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(5);
        assertThat(response.getBody()).contains("Chicago", "New York", "Los Angeles");
    }

    @Test
    @DisplayName("Should handle city not found exception with 404 status")
    void handleWeatherServerException_CityNotFound_Returns404() {
        // Given
        WeatherServerException exception = new WeatherServerException(
            "City not found: InvalidCity",
            "CITY_NOT_FOUND",
            "getCityCoordinates"
        );

        // When
        ResponseEntity<Map<String, Object>> response = weatherController.handleWeatherServerException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("error", true);
        assertThat(response.getBody()).containsEntry("errorCode", "CITY_NOT_FOUND");
        assertThat(response.getBody()).containsEntry("operation", "getCityCoordinates");
        assertThat(response.getBody()).containsEntry("message", "City not found: InvalidCity");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Should handle geocoding error with 502 status")
    void handleWeatherServerException_GeocodingError_Returns502() {
        // Given
        WeatherServerException exception = new WeatherServerException(
            "Unable to geocode city",
            "GEOCODING_ERROR",
            "getCityCoordinates"
        );

        // When
        ResponseEntity<Map<String, Object>> response = weatherController.handleWeatherServerException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("error", true);
        assertThat(response.getBody()).containsEntry("errorCode", "GEOCODING_ERROR");
    }

    @Test
    @DisplayName("Should handle NWS grid error with 502 status")
    void handleWeatherServerException_NWSGridError_Returns502() {
        // Given
        WeatherServerException exception = new WeatherServerException(
            "Unable to get grid point information",
            "NWS_GRID_ERROR",
            "getCurrentWeather"
        );

        // When
        ResponseEntity<Map<String, Object>> response = weatherController.handleWeatherServerException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("errorCode", "NWS_GRID_ERROR");
    }

    @Test
    @DisplayName("Should handle general weather fetch error with 500 status")
    void handleWeatherServerException_GeneralError_Returns500() {
        // Given
        WeatherServerException exception = new WeatherServerException(
            "Failed to fetch weather data",
            "WEATHER_FETCH_ERROR",
            "getCurrentWeather"
        );

        // When
        ResponseEntity<Map<String, Object>> response = weatherController.handleWeatherServerException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("errorCode", "WEATHER_FETCH_ERROR");
    }

    @Test
    @DisplayName("Should handle unknown error code with 500 status")
    void handleWeatherServerException_UnknownError_Returns500() {
        // Given
        WeatherServerException exception = new WeatherServerException(
            "Unknown error occurred",
            "UNKNOWN_ERROR",
            "someOperation"
        );

        // When
        ResponseEntity<Map<String, Object>> response = weatherController.handleWeatherServerException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("errorCode", "UNKNOWN_ERROR");
        assertThat(response.getBody()).containsEntry("operation", "someOperation");
    }

    @Test
    @DisplayName("Should format error response correctly")
    void handleWeatherServerException_FormatsResponseCorrectly() {
        // Given
        WeatherServerException exception = new WeatherServerException(
            "Test error message",
            "TEST_ERROR",
            "testOperation"
        );

        // When
        ResponseEntity<Map<String, Object>> response = weatherController.handleWeatherServerException(exception);

        // Then
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).hasSize(5); // error, message, errorCode, operation, timestamp
        assertThat(body.get("timestamp")).isInstanceOf(Long.class);
        assertThat((Long) body.get("timestamp")).isGreaterThan(0);
    }
}