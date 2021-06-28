package com.oldterns.vilebot.services;

import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class WeatherServiceTest
{

    private final static String YYZ = "https://weather.gc.ca/rss/city/on-128_e.xml";

    private final static String YGK = "https://weather.gc.ca/rss/city/on-69_e.xml";

    @Inject
    WeatherService weatherService;

    @Inject
    TestUrlStreamHandler urlStreamHandler;

    @InjectMock
    ClientCreator clientCreator;

    @BeforeEach
    public void setup()
        throws MalformedURLException
    {
        urlStreamHandler.reset();
    }

    @Test
    public void testDefaultForecast()
        throws MalformedURLException
    {
        urlStreamHandler.mockConnection( YYZ, WeatherServiceTest.class.getResourceAsStream( "ytz-weather-data.xml" ) );
        assertThat( weatherService.forecastWeather() ).isEqualTo( "[Sunday night: Clear. Low 6.] [Monday: Mainly sunny. High 23.] [Monday night: Mainly cloudy. Low 13.] [Tuesday: Sunny. High 24.] [Tuesday night: Cloudy periods. Low 12.] [Wednesday: A mix of sun and cloud. High 22.] [Wednesday night: Chance of showers. Low 14. POP 60%] [Thursday: Chance of showers. High 22. POP 60%] [Thursday night: Chance of showers. Low 15. POP 60%] " );
    }

    @Test
    public void testLocationForecast()
        throws MalformedURLException
    {
        urlStreamHandler.mockConnection( YGK, WeatherServiceTest.class.getResourceAsStream( "ygk-weather-data.xml" ) );
        assertThat( weatherService.forecastWeather( "ygk" ) ).isEqualTo( "[Sunday night: Partly cloudy. Low plus 5.] [Monday: Mainly sunny. High 23.] [Monday night: Increasing cloudiness. Low 12.] [Tuesday: A mix of sun and cloud. High 20.] [Tuesday night: Clear. Low 11.] [Wednesday: A mix of sun and cloud. High 21.] [Wednesday night: Chance of showers. Low 13. POP 60%] [Thursday: Chance of showers. High 19. POP 60%] [Thursday night: Chance of showers. Low 15. POP 60%] " );
    }

    @Test
    public void testDefaultWeather()
        throws MalformedURLException
    {
        ChannelMessageEvent channelMessageEvent = Mockito.mock( ChannelMessageEvent.class );
        urlStreamHandler.mockConnection( YYZ, WeatherServiceTest.class.getResourceAsStream( "ytz-weather-data.xml" ) );
        assertThat( weatherService.onWeather( channelMessageEvent,
                                              Optional.empty() ) ).isEqualTo( "Sunny, Temperature: 14.5°C, Humidity: 46 % - Toronto City Centre Airport 7:00 PM EDT Sunday 30 May 2021" );
    }

    @Test
    public void testLocationWeather()
        throws MalformedURLException
    {
        ChannelMessageEvent channelMessageEvent = Mockito.mock( ChannelMessageEvent.class );
        urlStreamHandler.mockConnection( YGK, WeatherServiceTest.class.getResourceAsStream( "ygk-weather-data.xml" ) );
        assertThat( weatherService.onWeather( channelMessageEvent,
                                              Optional.of( "ygk" ) ) ).isEqualTo( "Partly Cloudy, Temperature: 16.7°C, Humidity: 46 % - Kingston Airport 7:00 PM EDT Sunday 30 May 2021" );
    }

    @Test
    public void testDefaultLessWeather()
        throws MalformedURLException
    {
        Client client = Mockito.mock( Client.class );
        User user = Mockito.mock( User.class );
        Channel channel = Mockito.mock( Channel.class );
        ServerMessage serverMessage = Mockito.mock( ServerMessage.class );
        Mockito.when( channel.getClient() ).thenReturn( client );
        Mockito.when( channel.getMessagingName() ).thenReturn( "#weather" );
        Mockito.when( user.getClient() ).thenReturn( client );
        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, serverMessage, user, channel, "!lessweather" );

        Client mockClient = Mockito.mock( Client.class );
        Mockito.when( clientCreator.createClient( WeatherService.LESS_NICK ) ).thenReturn( mockClient );
        AtomicReference<Consumer<String>> outputConsumer = new AtomicReference<>();
        Mockito.doAnswer( invocationOnMock -> {
            outputConsumer.set( invocationOnMock.getArgument( 0 ) );
            return null;
        } ).when( mockClient ).setOutputListener( any() );

        urlStreamHandler.mockConnection( YYZ, WeatherServiceTest.class.getResourceAsStream( "ytz-weather-data.xml" ) );
        assertThat( weatherService.onLessWeather( channelMessageEvent, Optional.empty() ) ).isNull();
        outputConsumer.get().accept( "IT'S SUNNY" );
        Mockito.verify( mockClient ).addChannel( "#weather" );
        Mockito.verify( mockClient ).connect();
        Mockito.verify( mockClient ).setOutputListener( any() );
        Mockito.verify( mockClient ).sendMessage( "#weather", "IT'S SUNNY" );
        Mockito.verify( mockClient ).shutdown();
        Mockito.verifyNoMoreInteractions( mockClient );
    }

    @Test
    public void testLocationLessWeather()
        throws MalformedURLException
    {
        Client client = Mockito.mock( Client.class );
        User user = Mockito.mock( User.class );
        Channel channel = Mockito.mock( Channel.class );
        ServerMessage serverMessage = Mockito.mock( ServerMessage.class );
        Mockito.when( channel.getClient() ).thenReturn( client );
        Mockito.when( channel.getMessagingName() ).thenReturn( "#weather" );
        Mockito.when( user.getClient() ).thenReturn( client );
        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, serverMessage, user, channel, "!lessweather" );

        Client mockClient = Mockito.mock( Client.class );
        Mockito.when( clientCreator.createClient( WeatherService.LESS_NICK ) ).thenReturn( mockClient );
        AtomicReference<Consumer<String>> outputConsumer = new AtomicReference<>();
        Mockito.doAnswer( invocationOnMock -> {
            outputConsumer.set( invocationOnMock.getArgument( 0 ) );
            return null;
        } ).when( mockClient ).setOutputListener( any() );

        urlStreamHandler.mockConnection( YGK, WeatherServiceTest.class.getResourceAsStream( "ygk-weather-data.xml" ) );

        assertThat( weatherService.onLessWeather( channelMessageEvent, Optional.of( "ygk" ) ) ).isNull();
        outputConsumer.get().accept( "IT'S PARTLY CLOUDY" );
        Mockito.verify( mockClient ).addChannel( "#weather" );
        Mockito.verify( mockClient ).connect();
        Mockito.verify( mockClient ).setOutputListener( any() );
        Mockito.verify( mockClient ).sendMessage( "#weather", "IT'S PARTLY CLOUDY" );
        Mockito.verify( mockClient ).shutdown();
        Mockito.verifyNoMoreInteractions( mockClient );
    }

    @Test
    public void testDefaultMoreWeather()
        throws MalformedURLException
    {
        ChannelMessageEvent channelMessageEvent = Mockito.mock( ChannelMessageEvent.class );
        urlStreamHandler.mockConnection( YYZ, WeatherServiceTest.class.getResourceAsStream( "ytz-weather-data.xml" ) );
        assertThat( weatherService.onMoreWeather( channelMessageEvent,
                                                  Optional.empty() ) ).isEqualTo( "[Observed at: Toronto City Centre Airport 7:00 PM EDT Sunday 30 May 2021] [Condition: Sunny] [Temperature: 14.5°C] [Pressure: 102.3 kPa] [Visibility: 16.1 km] [Humidity: 46 %] [Dewpoint: 3.1°C] [Wind: SSW 4 km/h] [Air Quality Health Index: N/A] " );
    }

    @Test
    public void testLocationMoreWeather()
        throws MalformedURLException
    {
        ChannelMessageEvent channelMessageEvent = Mockito.mock( ChannelMessageEvent.class );
        urlStreamHandler.mockConnection( YGK, WeatherServiceTest.class.getResourceAsStream( "ygk-weather-data.xml" ) );
        assertThat( weatherService.onMoreWeather( channelMessageEvent,
                                                  Optional.of( "ygk" ) ) ).isEqualTo( "[Observed at: Kingston Airport 7:00 PM EDT Sunday 30 May 2021] [Condition: Partly Cloudy] [Temperature: 16.7°C] [Pressure / Tendency: 102.3 kPa falling] [Visibility: 24.1 km] [Humidity: 46 %] [Dewpoint: 5.1°C] [Wind: SSE 13 km/h] [Air Quality Health Index: 2] " );
    }
}
