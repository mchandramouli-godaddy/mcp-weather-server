# MCP Weather Server

A Spring Boot-based Model Context Protocol (MCP) server that provides weather information from the National Weather Service (NWS) API. This server exposes weather tools that can be used by MCP clients and also provides REST endpoints for direct HTTP access.

## Features

### MCP Tools (8 total)
- **GetCurrentWeather** - Get current weather by latitude/longitude
- **GetWeatherForecast** - Get extended forecast by latitude/longitude  
- **GetWeatherAlerts** - Get active weather alerts by latitude/longitude
- **GetLocationInfo** - Get location details from coordinates
- **GetCurrentWeatherByCity** - Get current weather by city name
- **GetWeatherForecastByCity** - Get extended forecast by city name
- **GetWeatherAlertsByCity** - Get active alerts by city name
- **GetCityCoordinates** - Get coordinates for a city name

### REST Endpoints
- `GET /weather/{city}` - Current weather for a city
- `GET /weather/{city}/forecast` - 5-day forecast for a city
- `GET /cities` - List of supported cities

### Data Sources
- **National Weather Service (NWS)** - Primary weather data source
- **Open-Meteo Geocoding API** - City name to coordinate conversion

## Prerequisites

- Java 17 or higher
- No Maven installation required (uses Maven Wrapper)

## Building

```bash
# Clone the repository
git clone <repository-url>
cd mcp-weather-server

# Make Maven Wrapper executable (Unix/macOS)
chmod +x ./mvnw

# Clean and compile
./mvnw clean compile

# Build the JAR file
./mvnw clean package
```

## Running

### Development Mode
```bash
./mvnw spring-boot:run
```

### Production Mode
```bash
java -jar target/mcp-weather-server-1.0.0-SNAPSHOT.jar
```

The server will start on port **8085** by default.

## Testing

### Test REST Endpoints

1. **Get supported cities:**
```bash
curl http://localhost:8085/cities
```

2. **Get current weather for a city:**
```bash
curl http://localhost:8085/weather/Chicago
```

3. **Get forecast for a city:**
```bash
curl http://localhost:8085/weather/Chicago/forecast
```

4. **Test error handling (invalid city):**
```bash
curl http://localhost:8085/weather/InvalidCity
```

### Run Unit Tests

```bash
# Run all tests
./mvnw test

# Run tests with coverage
./mvnw test jacoco:report

# Run specific test class
./mvnw test -Dtest=WeatherControllerTest
```

### Test MCP Integration

The MCP server exposes tools via Server-Sent Events at `/mcp/messages`. You can connect MCP clients to:
```
http://localhost:8085/mcp/messages
```

### Sample Responses

**Current Weather:**
```json
{
  "location": "41.8781, -87.6298",
  "name": "Tonight",
  "temperature": "32°F",
  "windSpeed": "5 mph",
  "windDirection": "NW",
  "shortForecast": "Partly Cloudy",
  "detailedForecast": "Partly cloudy, with a low around 32..."
}
```

**Supported Cities:**
```json
[
  "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
  "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose",
  "Austin", "Jacksonville", "Fort Worth", "Columbus", "Charlotte",
  "Seattle", "Denver", "Boston", "Nashville", "Baltimore"
]
```

## Configuration

The application can be configured via `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: weather-mcp-server
  ai:
    mcp:
      server:
        name: weather-mcp-server
        stdio: false
        version: 1.0.0
        type: ASYNC
        sse-message-endpoint: /mcp/messages

server:
  port: 8085

logging:
  level:
    com.godaddy.examples.mcp.weather: DEBUG
    org.springframework.ai: DEBUG
```

## Architecture

- **Spring Boot 3.2.0** - Application framework
- **Spring AI MCP Server** - MCP protocol implementation
- **RestTemplate** - HTTP client for external APIs
- **Lombok** - Code generation for boilerplate reduction
- **Spring Retry** - Retry mechanism for API calls
- **Custom Exception Handling** - Structured error responses with proper HTTP status codes

## Error Handling

The server includes comprehensive error handling with a custom `WeatherServerException`:

### **Custom Exception Features:**
- **Structured error codes** - Specific error identification
- **Operation context** - Know which operation failed
- **Proper HTTP status mapping** - RESTful error responses
- **Enhanced logging** - Formatted error messages with full context

### **Error Codes:**
| **Error Code** | **HTTP Status** | **Description** |
|---|---|---|
| `CITY_NOT_FOUND` | 404 Not Found | City not found in geocoding API |
| `GEOCODING_ERROR` | 502 Bad Gateway | Failed to geocode city name |
| `NWS_GRID_ERROR` | 502 Bad Gateway | Failed to get NWS grid point |
| `NWS_FORECAST_ERROR` | 502 Bad Gateway | Failed to get NWS forecast data |
| `NWS_LOCATION_ERROR` | 502 Bad Gateway | Failed to get NWS location info |
| `NO_WEATHER_DATA` | 500 Internal Server Error | No weather periods available |
| `WEATHER_FETCH_ERROR` | 500 Internal Server Error | General weather data fetch failure |
| `CITY_WEATHER_ERROR` | 500 Internal Server Error | Failed to get weather for city |
| `COORDINATES_ERROR` | 500 Internal Server Error | General coordinate lookup failure |

### **Error Response Format:**
```json
{
  "error": true,
  "message": "City not found: InvalidCity",
  "errorCode": "CITY_NOT_FOUND",
  "operation": "getCityCoordinates",
  "timestamp": 1640995200000
}
```

### **Error Handling Benefits:**
- **Better debugging** - Clear error identification and context
- **Client-friendly** - Structured JSON responses with meaningful HTTP status codes
- **Comprehensive logging** - Full stack traces with operation context
- **Consistent format** - All errors follow the same response structure

## Development

### Project Structure
```
src/
├── main/
│   ├── java/com/godaddy/examples/mcp/weather/
│   │   ├── WeatherMcpApplication.java      # Main application class
│   │   ├── controllers/
│   │   │   └── WeatherController.java     # REST endpoints
│   │   └── services/
│   │       └── WeatherService.java        # Business logic & MCP tools
│   └── resources/
│       └── application.yaml               # Configuration
└── target/                                # Build output
```

### Adding New Tools

1. Add a new method to `WeatherService.java`
2. Annotate with `@Tool(name="ToolName", description="Description")`
3. The tool will be automatically registered by Spring AI

### Extending Functionality

- Add new weather data sources by extending `WeatherService`
- Add new REST endpoints in `WeatherController`
- Modify city list in `WeatherService.getSupportedCities()`

## License

This project is an example implementation for educational purposes.

## Support

For issues or questions, please check the application logs which include detailed DEBUG information for weather operations and Spring AI MCP server activities.