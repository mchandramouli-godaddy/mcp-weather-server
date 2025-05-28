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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService Tests")
class WeatherServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WeatherService weatherService;

    private Map<String, Object> sampleNWSPointResponse;
    private Map<String, Object> sampleNWSForecastResponse;
    private Map<String, Object> sampleGeocodingResponse;
    private Map<String, Object> sampleNWSAlertsResponse;

    @BeforeEach
    void setUp() {
        // Sample NWS Points API response
        sampleNWSPointResponse = Map.of(
            "properties", Map.of(
                "forecast", "https://api.weather.gov/gridpoints/LOT/31,76/forecast",
                "gridId", "LOT",
                "gridX", 31,
                "gridY", 76,
                "timeZone", "America/Chicago",
                "relativeLocation", Map.of(
                    "properties", Map.of(
                        "city", "Chicago",
                        "state", "IL"
                    )
                )
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
                    ),
                    Map.of(
                        "name", "Tomorrow",
                        "temperature", 45,
                        "temperatureUnit", "F",
                        "windSpeed", "8 mph",
                        "windDirection", "SW",
                        "shortForecast", "Sunny",
                        "detailedForecast", "Sunny, with a high near 45...",
                        "isDaytime", true
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

        // Sample NWS Alerts API response
        sampleNWSAlertsResponse = Map.of(
            "features", List.of(
                Map.of(
                    "properties", Map.of(
                        "event", "Winter Storm Warning",
                        "headline", "Winter Storm Warning issued for Chicago",
                        "description", "Heavy snow expected...",
                        "severity", "Severe",
                        "urgency", "Expected",
                        "areaDesc", "Chicago Metro Area",
                        "effective", "2023-01-15T12:00:00Z",
                        "expires", "2023-01-16T06:00:00Z"
                    )
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
            .hasMessageContaining("Unable to get grid point information")
            .satisfies(ex -> {
                WeatherServerException wse = (WeatherServerException) ex;
                assertThat(wse.getErrorCode()).isEqualTo("WEATHER_FETCH_ERROR");
                assertThat(wse.getOperation()).isEqualTo("getCurrentWeather");
            });
    }

    @Test
    @DisplayName("Should throw exception when NWS forecast API returns null")
    void getCurrentWeather_NullForecastResponse_ThrowsException() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // When & Then
        assertThatThrownBy(() -> weatherService.getCurrentWeather(latitude, longitude))
            .isInstanceOf(WeatherServerException.class)
            .hasMessageContaining("Unable to get weather forecast")
            .satisfies(ex -> {
                WeatherServerException wse = (WeatherServerException) ex;
                assertThat(wse.getErrorCode()).isEqualTo("WEATHER_FETCH_ERROR");
            });
    }

    @Test
    @DisplayName("Should throw exception when no weather data available")
    void getCurrentWeather_NoWeatherData_ThrowsException() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        Map<String, Object> emptyForecastResponse = Map.of(
            "properties", Map.of("periods", List.of())
        );
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(emptyForecastResponse, HttpStatus.OK));

        // When & Then
        assertThatThrownBy(() -> weatherService.getCurrentWeather(latitude, longitude))
            .isInstanceOf(WeatherServerException.class)
            .hasMessageContaining("No weather data available")
            .satisfies(ex -> {
                WeatherServerException wse = (WeatherServerException) ex;
                assertThat(wse.getErrorCode()).isEqualTo("WEATHER_FETCH_ERROR");
            });
    }

    @Test
    @DisplayName("Should get weather forecast by coordinates successfully")
    void getWeatherForecast_ValidCoordinates_ReturnsForecastData() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSForecastResponse, HttpStatus.OK));

        // When
        List<Map<String, Object>> result = weatherService.getWeatherForecast(latitude, longitude);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("name", "Tonight");
        assertThat(result.get(1)).containsEntry("name", "Tomorrow");
        assertThat(result.get(0)).containsEntry("temperature", "32°F");
        assertThat(result.get(1)).containsEntry("temperature", "45°F");
    }

    @Test
    @DisplayName("Should get weather alerts by coordinates successfully")
    void getWeatherAlerts_ValidCoordinates_ReturnsAlertsData() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleNWSAlertsResponse, HttpStatus.OK));

        // When
        List<Map<String, Object>> result = weatherService.getWeatherAlerts(latitude, longitude);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("event", "Winter Storm Warning");
        assertThat(result.get(0)).containsEntry("severity", "Severe");
        assertThat(result.get(0)).containsEntry("urgency", "Expected");
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

    @Test
    @DisplayName("Should get location info by coordinates successfully")
    void getLocationInfo_ValidCoordinates_ReturnsLocationData() {
        // Given
        double latitude = 41.8781;
        double longitude = -87.6298;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK));

        // When
        Map<String, Object> result = weatherService.getLocationInfo(latitude, longitude);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("city", "Chicago");
        assertThat(result).containsEntry("state", "IL");
        assertThat(result).containsEntry("gridId", "LOT");
        assertThat(result).containsEntry("timeZone", "America/Chicago");
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
    @DisplayName("Should throw exception when geocoding API returns null")
    void getCityCoordinates_NullGeocodingResponse_ThrowsException() {
        // Given
        String cityName = "Chicago";
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // When & Then
        assertThatThrownBy(() -> weatherService.getCityCoordinates(cityName))
            .isInstanceOf(WeatherServerException.class)
            .hasMessageContaining("Unable to geocode city: Chicago")
            .satisfies(ex -> {
                WeatherServerException wse = (WeatherServerException) ex;
                assertThat(wse.getErrorCode()).isEqualTo("COORDINATES_ERROR");
                assertThat(wse.getOperation()).isEqualTo("getCityCoordinates");
            });
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
            .hasMessageContaining("City not found: InvalidCity")
            .satisfies(ex -> {
                WeatherServerException wse = (WeatherServerException) ex;
                assertThat(wse.getErrorCode()).isEqualTo("COORDINATES_ERROR");
                assertThat(wse.getOperation()).isEqualTo("getCityCoordinates");
            });
    }

    @Test
    @DisplayName("Should get current weather by city successfully")
    void getCurrentWeatherByCity_ValidCity_ReturnsWeatherData() {
        // Given
        String cityName = "Chicago";
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleGeocodingResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSForecastResponse, HttpStatus.OK));

        // When
        Map<String, Object> result = weatherService.getCurrentWeatherByCity(cityName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("cityName", "Chicago");
        assertThat(result).containsEntry("name", "Tonight");
        assertThat(result).containsEntry("temperature", "32°F");
        assertThat(result).containsKey("coordinates");
    }

    @Test
    @DisplayName("Should get weather forecast by city successfully")
    void getWeatherForecastByCity_ValidCity_ReturnsForecastData() {
        // Given
        String cityName = "Chicago";
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleGeocodingResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSForecastResponse, HttpStatus.OK));

        // When
        List<Map<String, Object>> result = weatherService.getWeatherForecastByCity(cityName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("cityName", "Chicago");
        assertThat(result.get(1)).containsEntry("cityName", "Chicago");
    }

    @Test
    @DisplayName("Should get weather alerts by city successfully")
    void getWeatherAlertsByCity_ValidCity_ReturnsAlertsData() {
        // Given
        String cityName = "Chicago";
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleGeocodingResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSAlertsResponse, HttpStatus.OK));

        // When
        List<Map<String, Object>> result = weatherService.getWeatherAlertsByCity(cityName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("cityName", "Chicago");
        assertThat(result.get(0)).containsEntry("event", "Winter Storm Warning");
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
    @DisplayName("Should delegate getCurrentWeather correctly")
    void getCurrentWeather_StringParameter_DelegatesCorrectly() {
        // Given
        String cityName = "Chicago";
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleGeocodingResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSForecastResponse, HttpStatus.OK));

        // When
        Map<String, Object> result = weatherService.getCurrentWeather(cityName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("cityName", "Chicago");
    }

    @Test
    @DisplayName("Should delegate getWeatherForecast correctly")
    void getWeatherForecast_StringParameters_DelegatesCorrectly() {
        // Given
        String cityName = "Chicago";
        int days = 5;
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(sampleGeocodingResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSPointResponse, HttpStatus.OK))
            .thenReturn(new ResponseEntity<>(sampleNWSForecastResponse, HttpStatus.OK));

        // When
        List<Map<String, Object>> result = weatherService.getWeatherForecast(cityName, days);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("cityName", "Chicago");
    }
}