package com.godaddy.examples.mcp.weather.services;

import com.godaddy.examples.mcp.weather.exceptions.WeatherServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService Simple Tests")
class WeatherServiceSimpleTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WeatherService weatherService;

    private Map<String, Object> sampleNWSPointResponse;
    private Map<String, Object> sampleNWSForecastResponse;
    private Map<String, Object> sampleGeocodingResponse;

    @BeforeEach
    void setUp() {
        // Sample NWS Points API response
        sampleNWSPointResponse = Map.of(
            "properties", Map.of(
                "forecast", "https://api.weather.gov/gridpoints/LOT/31,76/forecast",
                "gridId", "LOT",
                "gridX", 31,
                "gridY", 76,
                "timeZone", "America/Chicago"
            )
        );

        // Sample NWS Forecast API response
        sampleNWSForecastResponse = Map.of(
            "properties", Map.of(
                "periods", List.of(
                    Map.of(
                        "name", "Tonight",
                        "temperature", 32,
                        "temperatureUnit", "F",
                        "windSpeed", "5 mph",
                        "windDirection", "NW",
                        "shortForecast", "Partly Cloudy",
                        "detailedForecast", "Partly cloudy, with a low around 32...",
                        "isDaytime", false
                    )
                )
            )
        );

        // Sample Geocoding API response
        sampleGeocodingResponse = Map.of(
            "results", List.of(
                Map.of(
                    "name", "Chicago",
                    "latitude", 41.8781,
                    "longitude", -87.6298,
                    "country", "United States",
                    "admin1", "Illinois",
                    "timezone", "America/Chicago"
                )
            )
        );
    }

    @Test
    @DisplayName("Should get current weather by coordinates successfully")
    void getCurrentWeather_ValidCoordinates_ReturnsWeatherData() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSForecastResponse, HttpStatus.OK));

        // When
        Map<String, Object> result = weatherService.getCurrentWeather(latitude, longitude);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("name", "Tonight");
        assertThat(result).containsEntry("temperature", "32°F");
        assertThat(result).containsEntry("windSpeed", "5 mph");
        assertThat(result).containsEntry("shortForecast", "Partly Cloudy");
        assertThat(result).containsKey("location");
    }

    @Test
    @DisplayName("Should throw exception when NWS points API returns null")
    void getCurrentWeather_NullPointsResponse_ThrowsException() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // When & Then
        assertThatThrownBy(() -> weatherService.getCurrentWeather(latitude, longitude))
            .isInstanceOf(WeatherServerException.class)
            .hasMessageContaining("Failed to fetch weather data")
            .satisfies(ex -> {
                WeatherServerException wse = (WeatherServerException) ex;
                assertThat(wse.getErrorCode()).isEqualTo("WEATHER_FETCH_ERROR");
                assertThat(wse.getOperation()).isEqualTo("getCurrentWeather");
            });
    }

    @Test
    @DisplayName("Should get city coordinates successfully")
    void getCityCoordinates_ValidCity_ReturnsCoordinates() {
        // Given
        String cityName = "Chicago";
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleGeocodingResponse, HttpStatus.OK));

        // When
        Map<String, Object> result = weatherService.getCityCoordinates(cityName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("cityName", "Chicago");
        assertThat(result).containsEntry("latitude", 41.8781);
        assertThat(result).containsEntry("longitude", -87.6298);
        assertThat(result).containsEntry("country", "United States");
        assertThat(result).containsEntry("state", "Illinois");
    }

    @Test
    @DisplayName("Should throw exception when city not found")
    void getCityCoordinates_CityNotFound_ThrowsException() {
        // Given
        String cityName = "InvalidCity";
        Map<String, Object> emptyGeocodingResponse = Map.of("results", List.of());
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(emptyGeocodingResponse, HttpStatus.OK));

        // When & Then
        assertThatThrownBy(() -> weatherService.getCityCoordinates(cityName))
            .isInstanceOf(WeatherServerException.class)
            .hasMessageContaining("Failed to find coordinates for InvalidCity")
            .satisfies(ex -> {
                WeatherServerException wse = (WeatherServerException) ex;
                assertThat(wse.getErrorCode()).isEqualTo("COORDINATES_ERROR");
                assertThat(wse.getOperation()).isEqualTo("getCityCoordinates");
            });
    }

    @Test
    @DisplayName("Should handle REST client exceptions gracefully")
    void getCurrentWeather_RestClientException_ThrowsWeatherServerException() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenThrow(new RestClientException("Network error"));

        // When & Then
        assertThatThrownBy(() -> weatherService.getCurrentWeather(latitude, longitude))
            .isInstanceOf(WeatherServerException.class)
            .hasMessageContaining("Failed to fetch weather data")
            .satisfies(ex -> {
                WeatherServerException wse = (WeatherServerException) ex;
                assertThat(wse.getErrorCode()).isEqualTo("WEATHER_FETCH_ERROR");
                assertThat(wse.getOperation()).isEqualTo("getCurrentWeather");
                assertThat(wse.getCause()).isInstanceOf(RestClientException.class);
            });
    }

    @Test
    @DisplayName("Should return supported cities list")
    void getSupportedCities_ReturnsListOfCities() {
        // When
        List<String> result = weatherService.getSupportedCities();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).contains("New York", "Los Angeles", "Chicago", "Houston", "Phoenix");
        assertThat(result).hasSize(20); // As defined in the service
    }

    @Test
    @DisplayName("Should return empty list when no alerts available")
    void getWeatherAlerts_NoAlerts_ReturnsEmptyList() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // When
        List<Map<String, Object>> result = weatherService.getWeatherAlerts(latitude, longitude);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}