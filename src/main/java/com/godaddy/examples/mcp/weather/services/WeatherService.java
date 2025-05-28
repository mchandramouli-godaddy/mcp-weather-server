package com.godaddy.examples.mcp.weather.services;

import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.godaddy.examples.mcp.weather.exceptions.WeatherServerException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WeatherService {
    
    private final RestTemplate restTemplate;
    private static final String NWS_BASE_URL = "https://api.weather.gov";
    private static final String GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/v1/search";
    
    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    // Coordinate-based tools
    @WithSpan("weather.getCurrentWeather")
    @Tool(name = "GetCurrentWeather", description = "Get current weather information for a specific location using latitude and longitude")
    public Map<String, Object> getCurrentWeather(double latitude, double longitude) {
        try {
            // First, get the grid point for the coordinates
            String pointUrl = String.format("%s/points/%.4f,%.4f", NWS_BASE_URL, latitude, longitude);
            log.debug("NWS points URL: {}", pointUrl);
            
            ResponseEntity<Map> pointResponse = restTemplate.getForEntity(pointUrl, Map.class);
            log.debug("NWS points response status: {}", pointResponse.getStatusCode());
            
            if (pointResponse.getBody() == null) {
                throw new WeatherServerException("Unable to get grid point information", "NWS_GRID_ERROR", "getCurrentWeather");
            }
            
            Map<String, Object> pointData = pointResponse.getBody();
            Map<String, Object> properties = (Map<String, Object>) pointData.get("properties");
            String forecastUrl = (String) properties.get("forecast");
            
            // Get the current conditions from the forecast
            ResponseEntity<Map> forecastResponse = restTemplate.getForEntity(forecastUrl, Map.class);
            
            if (forecastResponse.getBody() == null) {
                throw new WeatherServerException("Unable to get weather forecast", "NWS_FORECAST_ERROR", "getCurrentWeather");
            }
            
            Map<String, Object> forecastData = forecastResponse.getBody();
            Map<String, Object> forecastProperties = (Map<String, Object>) forecastData.get("properties");
            List<Map<String, Object>> periods = (List<Map<String, Object>>) forecastProperties.get("periods");
            
            if (periods.isEmpty()) {
                throw new WeatherServerException("No weather data available", "NO_WEATHER_DATA", "getCurrentWeather");
            }
            
            // Return the current period (first in the list)
            Map<String, Object> currentPeriod = periods.get(0);
            
            // Use HashMap instead of Map.of() to create a mutable map
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("location", String.format("%.4f, %.4f", latitude, longitude));
            result.put("name", currentPeriod.get("name"));
            result.put("temperature", currentPeriod.get("temperature") + "°" + currentPeriod.get("temperatureUnit"));
            result.put("windSpeed", currentPeriod.get("windSpeed"));
            result.put("windDirection", currentPeriod.get("windDirection"));
            result.put("shortForecast", currentPeriod.get("shortForecast"));
            result.put("detailedForecast", currentPeriod.get("detailedForecast"));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error fetching weather data for coordinates {}, {}: {}", latitude, longitude, e.getMessage(), e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new WeatherServerException("Failed to fetch weather data: " + errorMsg, "WEATHER_FETCH_ERROR", "getCurrentWeather", e);
        }
    }

    @WithSpan("weather.getWeatherForecast")
    @Tool(name = "GetWeatherForecast", description = "Get extended weather forecast for a specific location using latitude and longitude")
    public List<Map<String, Object>> getWeatherForecast(double latitude, double longitude) {
        try {
            // Get grid point for coordinates
            String pointUrl = String.format("%s/points/%.4f,%.4f", NWS_BASE_URL, latitude, longitude);
            ResponseEntity<Map> pointResponse = restTemplate.getForEntity(pointUrl, Map.class);
            
            if (pointResponse.getBody() == null) {
                throw new WeatherServerException("Unable to get grid point information", "NWS_GRID_ERROR", "getWeatherForecast");
            }
            
            Map<String, Object> pointData = pointResponse.getBody();
            Map<String, Object> properties = (Map<String, Object>) pointData.get("properties");
            String forecastUrl = (String) properties.get("forecast");
            
            // Get the forecast
            ResponseEntity<Map> forecastResponse = restTemplate.getForEntity(forecastUrl, Map.class);
            
            if (forecastResponse.getBody() == null) {
                throw new WeatherServerException("Unable to get weather forecast", "NWS_FORECAST_ERROR", "getWeatherForecast");
            }
            
            Map<String, Object> forecastData = forecastResponse.getBody();
            Map<String, Object> forecastProperties = (Map<String, Object>) forecastData.get("properties");
            List<Map<String, Object>> periods = (List<Map<String, Object>>) forecastProperties.get("periods");
            
            return periods.stream()
                .map(period -> {
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("name", period.get("name"));
                    result.put("temperature", period.get("temperature") + "°" + period.get("temperatureUnit"));
                    result.put("windSpeed", period.get("windSpeed"));
                    result.put("windDirection", period.get("windDirection"));
                    result.put("shortForecast", period.get("shortForecast"));
                    result.put("detailedForecast", period.get("detailedForecast"));
                    result.put("isDaytime", period.get("isDaytime"));
                    return result;
                })
                .toList();
            
        } catch (Exception e) {
            log.error("Error fetching forecast data for coordinates {}, {}: {}", latitude, longitude, e.getMessage());
            throw new WeatherServerException("Failed to fetch forecast data: " + e.getMessage(), "FORECAST_FETCH_ERROR", "getWeatherForecast", e);
        }
    }

    @WithSpan("weather.getWeatherAlerts")
    @Tool(name = "GetWeatherAlerts", description = "Get active weather alerts for a specific location using latitude and longitude")
    public List<Map<String, Object>> getWeatherAlerts(double latitude, double longitude) {
        try {
            String alertsUrl = String.format("%s/alerts/active?point=%.4f,%.4f", NWS_BASE_URL, latitude, longitude);
            ResponseEntity<Map> alertsResponse = restTemplate.getForEntity(alertsUrl, Map.class);
            
            if (alertsResponse.getBody() == null) {
                return List.of(); // No alerts
            }
            
            Map<String, Object> alertsData = alertsResponse.getBody();
            List<Map<String, Object>> features = (List<Map<String, Object>>) alertsData.get("features");
            
            if (features == null || features.isEmpty()) {
                return List.of(); // No active alerts
            }
            
            return features.stream()
                .map(feature -> {
                    Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("event", properties.get("event"));
                    result.put("headline", properties.get("headline"));
                    result.put("description", properties.get("description"));
                    result.put("severity", properties.get("severity"));
                    result.put("urgency", properties.get("urgency"));
                    result.put("areas", properties.get("areaDesc"));
                    result.put("effective", properties.get("effective"));
                    result.put("expires", properties.get("expires"));
                    return result;
                })
                .toList();
            
        } catch (Exception e) {
            log.error("Error fetching weather alerts for coordinates {}, {}: {}", latitude, longitude, e.getMessage());
            return List.of(); // Return empty list if there's an error
        }
    }

    @WithSpan("weather.getLocationInfo")
    @Tool(name = "GetLocationInfo", description = "Get location information (city, state) from coordinates using reverse geocoding")
    public Map<String, Object> getLocationInfo(double latitude, double longitude) {
        try {
            String pointUrl = String.format("%s/points/%.4f,%.4f", NWS_BASE_URL, latitude, longitude);
            ResponseEntity<Map> pointResponse = restTemplate.getForEntity(pointUrl, Map.class);
            
            if (pointResponse.getBody() == null) {
                throw new WeatherServerException("Unable to get location information", "NWS_LOCATION_ERROR", "getLocationInfo");
            }
            
            Map<String, Object> pointData = pointResponse.getBody();
            Map<String, Object> properties = (Map<String, Object>) pointData.get("properties");
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("city", properties.get("relativeLocation") != null ? 
                ((Map<String, Object>) ((Map<String, Object>) properties.get("relativeLocation")).get("properties")).get("city") : "Unknown");
            result.put("state", properties.get("relativeLocation") != null ? 
                ((Map<String, Object>) ((Map<String, Object>) properties.get("relativeLocation")).get("properties")).get("state") : "Unknown");
            result.put("gridId", properties.get("gridId"));
            result.put("gridX", properties.get("gridX"));
            result.put("gridY", properties.get("gridY"));
            result.put("timeZone", properties.get("timeZone"));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error fetching location info for coordinates {}, {}: {}", latitude, longitude, e.getMessage());
            throw new WeatherServerException("Failed to fetch location information: " + e.getMessage(), "LOCATION_FETCH_ERROR", "getLocationInfo", e);
        }
    }

    // City-based tools
    @WithSpan("weather.getCurrentWeatherByCity")
    @Tool(name = "GetCurrentWeatherByCity", description = "Get current weather information for a city by name")
    public Map<String, Object> getCurrentWeatherByCity(String cityName) {
        try {
            Map<String, Object> coordinates = getCityCoordinates(cityName);
            double latitude = (Double) coordinates.get("latitude");
            double longitude = (Double) coordinates.get("longitude");
            
            log.debug("Retrieved coordinates for {}: lat={}, lon={}", cityName, latitude, longitude);
            
            Map<String, Object> weather = getCurrentWeather(latitude, longitude);
            
            // Create a new mutable map to add city information
            Map<String, Object> result = new java.util.HashMap<>(weather);
            result.put("cityName", cityName);
            result.put("coordinates", String.format("%.4f, %.4f", latitude, longitude));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error fetching weather data for city {}: {}", cityName, e.getMessage(), e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new WeatherServerException("Failed to fetch weather data for " + cityName + ": " + errorMsg, "CITY_WEATHER_ERROR", "getCurrentWeatherByCity", e);
        }
    }

    @WithSpan("weather.getWeatherForecastByCity")
    @Tool(name = "GetWeatherForecastByCity", description = "Get extended weather forecast for a city by name")
    public List<Map<String, Object>> getWeatherForecastByCity(String cityName) {
        try {
            Map<String, Object> coordinates = getCityCoordinates(cityName);
            double latitude = (Double) coordinates.get("latitude");
            double longitude = (Double) coordinates.get("longitude");
            
            List<Map<String, Object>> forecast = getWeatherForecast(latitude, longitude);
            
            // Add city name to each forecast period
            return forecast.stream()
                .map(period -> {
                    Map<String, Object> enhancedPeriod = new java.util.HashMap<>(period);
                    enhancedPeriod.put("cityName", cityName);
                    return enhancedPeriod;
                })
                .toList();
            
        } catch (Exception e) {
            log.error("Error fetching forecast data for city {}: {}", cityName, e.getMessage());
            throw new WeatherServerException("Failed to fetch forecast data for " + cityName + ": " + e.getMessage(), "CITY_FORECAST_ERROR", "getWeatherForecastByCity", e);
        }
    }

    @WithSpan("weather.getWeatherAlertsByCity")
    @Tool(name = "GetWeatherAlertsByCity", description = "Get active weather alerts for a city by name")
    public List<Map<String, Object>> getWeatherAlertsByCity(String cityName) {
        try {
            Map<String, Object> coordinates = getCityCoordinates(cityName);
            double latitude = (Double) coordinates.get("latitude");
            double longitude = (Double) coordinates.get("longitude");
            
            List<Map<String, Object>> alerts = getWeatherAlerts(latitude, longitude);
            
            // Add city name to each alert
            return alerts.stream()
                .map(alert -> {
                    Map<String, Object> enhancedAlert = new java.util.HashMap<>(alert);
                    enhancedAlert.put("cityName", cityName);
                    return enhancedAlert;
                })
                .toList();
            
        } catch (Exception e) {
            log.error("Error fetching weather alerts for city {}: {}", cityName, e.getMessage());
            return List.of(); // Return empty list if there's an error
        }
    }

    @WithSpan("weather.getCityCoordinates")
    @Tool(name = "GetCityCoordinates", description = "Get latitude and longitude coordinates for a city name")
    public Map<String, Object> getCityCoordinates(String cityName) {
        try {
            String encodedCityName = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
            String geocodingUrl = String.format("%s?name=%s&count=1&language=en&format=json", 
                GEOCODING_BASE_URL, encodedCityName);
            
            log.debug("Geocoding URL for {}: {}", cityName, geocodingUrl);
            
            ResponseEntity<Map> geocodingResponse = restTemplate.getForEntity(geocodingUrl, Map.class);
            log.debug("Geocoding response status: {}", geocodingResponse.getStatusCode());
            
            if (geocodingResponse.getBody() == null) {
                throw new WeatherServerException("Unable to geocode city: " + cityName, "GEOCODING_ERROR", "getCityCoordinates");
            }
            
            Map<String, Object> geocodingData = geocodingResponse.getBody();
            List<Map<String, Object>> results = (List<Map<String, Object>>) geocodingData.get("results");
            
            if (results == null || results.isEmpty()) {
                throw new WeatherServerException("City not found: " + cityName, "CITY_NOT_FOUND", "getCityCoordinates");
            }
            
            Map<String, Object> firstResult = results.get(0);
            
            // Use HashMap instead of Map.of() to create a mutable map
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("cityName", firstResult.get("name"));
            result.put("latitude", firstResult.get("latitude"));
            result.put("longitude", firstResult.get("longitude"));
            result.put("country", firstResult.get("country"));
            result.put("state", firstResult.getOrDefault("admin1", ""));
            result.put("timezone", firstResult.getOrDefault("timezone", ""));
            
            return result;
            
        } catch (Exception e) {
            log.error("Error geocoding city {}: {}", cityName, e.getMessage(), e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new WeatherServerException("Failed to find coordinates for " + cityName + ": " + errorMsg, "COORDINATES_ERROR", "getCityCoordinates", e);
        }
    }

    // Additional methods for REST endpoints
    public Map<String, Object> getCurrentWeather(String cityName) {
        return getCurrentWeatherByCity(cityName);
    }

    public List<Map<String, Object>> getWeatherForecast(String cityName, int days) {
        return getWeatherForecastByCity(cityName);
    }

    public List<String> getSupportedCities() {
        return List.of(
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", 
            "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose",
            "Austin", "Jacksonville", "Fort Worth", "Columbus", "Charlotte",
            "Seattle", "Denver", "Boston", "Nashville", "Baltimore"
        );
    }
}