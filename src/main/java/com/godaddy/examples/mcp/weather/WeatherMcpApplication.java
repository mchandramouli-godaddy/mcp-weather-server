package com.godaddy.examples.mcp.weather;

import java.util.List;

import com.godaddy.examples.mcp.weather.services.WeatherService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Configuration
@EnableRetry
@Slf4j
public class WeatherMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherMcpApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public List<ToolCallback> weatherTools(WeatherService weatherService) {
        return List.of(ToolCallbacks.from(weatherService));
    }
}