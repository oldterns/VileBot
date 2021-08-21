package com.oldterns.vilebot.services;

import java.time.Duration;
import java.util.concurrent.Future;

import javax.inject.Inject;

import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.RandomProvider;
import com.oldterns.vilebot.util.TimeService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class OmgwordServiceTest
{
    @Inject
    OmgwordService omgwordService;

    @InjectMock
    KarmaDB karmaDB;

    @InjectMock
    RandomProvider randomProvider;

    @InjectMock
    TimeService timeService;

    @Test
    public void testGameNoAnswer()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomElement( anyList() ) ).thenReturn( "test" );

        // test -> estt
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 1 ).thenReturn( 0 ).thenReturn( 0 );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!omgword" );
        assertThat( omgwordService.startGame( channelMessageEvent ) ).isEqualTo( "Welcome to omgword!\n"
            + "For 2 karma:\n" + "estt\n" + "30 seconds on the clock." );

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( Mockito.eq( Duration.ofSeconds( 30 ) ), runnableArgumentCaptor.capture() );
        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        runnableArgumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Game over! The correct answer was: test" );
        verifyNoInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void testGameWrongAnswer()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( randomProvider.getRandomElement( anyList() ) ).thenReturn( "test" );
        when( timeService.onTimeout( Mockito.eq( Duration.ofSeconds( 30 ) ), any() ) ).thenReturn( future );

        // test -> estt
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 1 ).thenReturn( 0 ).thenReturn( 0 );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!omgword" );
        assertThat( omgwordService.startGame( channelMessageEvent ) ).isEqualTo( "Welcome to omgword!\n"
            + "For 2 karma:\n" + "estt\n" + "30 seconds on the clock." );
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( Mockito.eq( Duration.ofSeconds( 30 ) ), runnableArgumentCaptor.capture() );

        assertThat( omgwordService.onAnswer( user,
                                             "dog" ) ).isEqualTo( "Sorry bob! That is incorrect, you lose 2 karma." );
        verify( karmaDB ).modNounKarma( "bob", -2 );
        verifyNoInteractions( future );

        // Cancel the game so other tests work
        runnableArgumentCaptor.getValue().run();
    }

    @Test
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void testGameRightAnswer()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( randomProvider.getRandomElement( anyList() ) ).thenReturn( "test" );
        when( timeService.onTimeout( Mockito.eq( Duration.ofSeconds( 30 ) ), any() ) ).thenReturn( future );

        // test -> estt
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 1 ).thenReturn( 0 ).thenReturn( 0 );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!omgword" );
        assertThat( omgwordService.startGame( channelMessageEvent ) ).isEqualTo( "Welcome to omgword!\n"
            + "For 2 karma:\n" + "estt\n" + "30 seconds on the clock." );

        assertThat( omgwordService.onAnswer( user, "test" ) ).isEqualTo( "Congrats bob, you win 2 karma!" );
        verify( karmaDB ).modNounKarma( "bob", 2 );
        verify( future ).cancel( true );
    }

    @Test
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void testGameAmbigiousAnswers()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( randomProvider.getRandomElement( anyList() ) ).thenReturn( "dog" );
        when( timeService.onTimeout( Mockito.eq( Duration.ofSeconds( 30 ) ), any() ) ).thenReturn( future );

        // dog -> odg
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 0 ).thenReturn( 0 );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!omgword" );
        assertThat( omgwordService.startGame( channelMessageEvent ) ).isEqualTo( "Welcome to omgword!\n"
            + "For 1 karma:\n" + "odg\n" + "30 seconds on the clock." );

        assertThat( omgwordService.onAnswer( user, "god" ) ).isEqualTo( "Congrats bob, you win 1 karma!" );
        verify( karmaDB ).modNounKarma( "bob", 1 );
        verify( future ).cancel( true );

        // dog -> odg
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 0 ).thenReturn( 0 );
        assertThat( omgwordService.startGame( channelMessageEvent ) ).isEqualTo( "Welcome to omgword!\n"
            + "For 1 karma:\n" + "odg\n" + "30 seconds on the clock." );

        assertThat( omgwordService.onAnswer( user, "dog" ) ).isEqualTo( "Congrats bob, you win 1 karma!" );
        verify( karmaDB, times( 2 ) ).modNounKarma( "bob", 1 );
        verify( future, times( 2 ) ).cancel( true );
    }
}
