package com.oldterns.vilebot.services;

import com.oldterns.vilebot.util.TestUrlStreamHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class WeatherServiceTest {
    private static TestUrlStreamHandler urlStreamHandler;
    private final static String YYZ = "https://weather.gc.ca/rss/city/on-128_e.xml";
    private final static String YGK = "https://weather.gc.ca/rss/city/on-69_e.xml";
    private WeatherService weatherService;

    @BeforeAll
    public static void beforeAll() {
        urlStreamHandler = TestUrlStreamHandler.setup();
    }

    @BeforeEach
    public void setup() throws MalformedURLException {
        urlStreamHandler.reset();
        weatherService = new WeatherService();
        weatherService.setup();
    }

    @Test
    public void testDefaultForecast() throws MalformedURLException {
        urlStreamHandler.mockConnection(new URL(YYZ),
                WeatherServiceTest.class.getResourceAsStream("ytz-weather-data.xml"));
        assertThat(weatherService.forecastWeather()).isEqualTo("[Sunday night: Clear. Low 6.] [Monday: Mainly sunny. High 23.] [Monday night: Mainly cloudy. Low 13.] [Tuesday: Sunny. High 24.] [Tuesday night: Cloudy periods. Low 12.] [Wednesday: A mix of sun and cloud. High 22.] [Wednesday night: Chance of showers. Low 14. POP 60%] [Thursday: Chance of showers. High 22. POP 60%] [Thursday night: Chance of showers. Low 15. POP 60%] ");
    }

    @Test
    public void testLocationForecast() throws MalformedURLException {
        urlStreamHandler.mockConnection(new URL(YGK),
                WeatherServiceTest.class.getResourceAsStream("ygk-weather-data.xml"));
        assertThat(weatherService.forecastWeather("ygk")).isEqualTo("[Sunday night: Partly cloudy. Low plus 5.] [Monday: Mainly sunny. High 23.] [Monday night: Increasing cloudiness. Low 12.] [Tuesday: A mix of sun and cloud. High 20.] [Tuesday night: Clear. Low 11.] [Wednesday: A mix of sun and cloud. High 21.] [Wednesday night: Chance of showers. Low 13. POP 60%] [Thursday: Chance of showers. High 19. POP 60%] [Thursday night: Chance of showers. Low 15. POP 60%] ");
    }

    @Test
    public void testDefaultWeather() throws MalformedURLException {
        urlStreamHandler.mockConnection(new URL(YYZ),
                WeatherServiceTest.class.getResourceAsStream("ytz-weather-data.xml"));
        assertThat(weatherService.onWeather()).isEqualTo("Sunny, Temperature: 14.5°C, Humidity: 46 % - Toronto City Centre Airport 7:00 PM EDT Sunday 30 May 2021");
    }

    @Test
    public void testLocationWeather() throws MalformedURLException {
        urlStreamHandler.mockConnection(new URL(YGK),
                WeatherServiceTest.class.getResourceAsStream("ygk-weather-data.xml"));
        assertThat(weatherService.onWeather("ygk")).isEqualTo("Partly Cloudy, Temperature: 16.7°C, Humidity: 46 % - Kingston Airport 7:00 PM EDT Sunday 30 May 2021");
    }

    @Test
    public void testDefaultLessWeather() throws MalformedURLException {
        urlStreamHandler.mockConnection(new URL(YYZ),
                WeatherServiceTest.class.getResourceAsStream("ytz-weather-data.xml"));
        assertThat(weatherService.onLessWeather()).isEqualTo("IT'S SUNNY");
    }

    @Test
    public void testLocationLessWeather() throws MalformedURLException {
        urlStreamHandler.mockConnection(new URL(YGK),
                WeatherServiceTest.class.getResourceAsStream("ygk-weather-data.xml"));
        assertThat(weatherService.onLessWeather("ygk")).isEqualTo("IT'S PARTLY CLOUDY");
    }

    @Test
    public void testDefaultMoreWeather() throws MalformedURLException {
        urlStreamHandler.mockConnection(new URL(YYZ),
                WeatherServiceTest.class.getResourceAsStream("ytz-weather-data.xml"));
        assertThat(weatherService.onMoreWeather()).isEqualTo("[Observed at: Toronto City Centre Airport 7:00 PM EDT Sunday 30 May 2021] [Condition: Sunny] [Temperature: 14.5°C] [Pressure: 102.3 kPa] [Visibility: 16.1 km] [Humidity: 46 %] [Dewpoint: 3.1°C] [Wind: SSW 4 km/h] [Air Quality Health Index: N/A] ");
    }

    @Test
    public void testLocationMoreWeather() throws MalformedURLException {
        urlStreamHandler.mockConnection(new URL(YGK),
                WeatherServiceTest.class.getResourceAsStream("ygk-weather-data.xml"));
        assertThat(weatherService.onMoreWeather("ygk")).isEqualTo("[Observed at: Kingston Airport 7:00 PM EDT Sunday 30 May 2021] [Condition: Partly Cloudy] [Temperature: 16.7°C] [Pressure / Tendency: 102.3 kPa falling] [Visibility: 24.1 km] [Humidity: 46 %] [Dewpoint: 5.1°C] [Wind: SSE 13 km/h] [Air Quality Health Index: 2] ");
    }
}
