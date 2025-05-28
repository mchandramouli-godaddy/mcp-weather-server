package com.godaddy.examples.mcp.weather.controllers;

import com.godaddy.examples.mcp.weather.exceptions.WeatherServerException;
import com.godaddy.examples.mcp.weather.services.WeatherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/{city}")
    public ResponseEntity<Map<String, Object>> getWeather(@PathVariable("city") String city) {
        return ResponseEntity.ok().body(weatherService.getCurrentWeather(city));
    }

    @GetMapping("/weather/{city}/forecast")
    public ResponseEntity<List<Map<String, Object>>> getForecast(@PathVariable("city") String city) {
        return ResponseEntity.ok().body(weatherService.getWeatherForecast(city, 5));
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getSupportedCities() {
        return ResponseEntity.ok().body(weatherService.getSupportedCities());
    }
    
    @ExceptionHandler(WeatherServerException.class)
    public ResponseEntity<Map<String, Object>> handleWeatherServerException(WeatherServerException ex) {
        log.error("Weather server error: {}", ex.getFormattedMessage(), ex);
        
        Map<String, Object> errorResponse = Map.of(
            "error", true,
            "message", ex.getMessage(),
            "errorCode", ex.getErrorCode(),
            "operation", ex.getOperation(),
            "timestamp", System.currentTimeMillis()
        );
        
        // Determine HTTP status based on error code
        HttpStatus status = switch (ex.getErrorCode()) {
            case "CITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "GEOCODING_ERROR", "NWS_GRID_ERROR", "NWS_FORECAST_ERROR", 
                 "NWS_LOCATION_ERROR" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        return ResponseEntity.status(status).body(errorResponse);
    }
}