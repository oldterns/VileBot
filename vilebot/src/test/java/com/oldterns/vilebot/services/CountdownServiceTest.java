package com.oldterns.vilebot.services;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;

import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.Colors;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class CountdownServiceTest
{

    @Inject
    CountdownService countdownService;

    @InjectMock
    KarmaDB karmaDB;

    @InjectMock
    TimeService timeService;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testSolutionWhenNoGameInSession()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        assertThat( countdownService.onSolution( user,
                                                 "1 + 10 * 20" ) ).isEqualTo( "No active game. Start a new one with !countdown." );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testRules()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        countdownService.getRules( user );
        verify( user ).sendMessage( " " + Colors.RED + "COUNTDOWN RULES:" + Colors.NORMAL );
        verify( user ).sendMessage( "1) Get as close as you can to the target number using only the numbers given." );
        verify( user ).sendMessage( Colors.RED + "TIP: You do not have to use all the numbers." + Colors.NORMAL );
        verify( user ).sendMessage( "2) Answer with !solution <your answer> . Make sure to only use valid characters, such as numbers and + - * / ( ) . " );
        verify( user ).sendMessage( "Breaking Rule 2 will subject you to a loss of 10 karma." );
        verify( user ).sendMessage( "3) The closer you are to the target number, the more karma you will get (max. 10)." );
        verify( user ).sendMessage( Colors.RED + "TIP: If you are over/under 200 you will be penalized 10 karma."
            + Colors.NORMAL );
        verify( user ).sendMessage( "4) Use \" /msg CountdownB0t !solution <your answer> \" for your answers." );
        verifyNoInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameNoResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "There were no submissions for this game. Better luck next time!" );
        verifyNoInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameTooLowResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( user,
                                                 "25 + 1" ) ).isEqualTo( "You have put an answer that breaks the threshold of +-200, you lose 10 karma." );
        verify( karmaDB ).modNounKarma( "bob", -10 );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "There were no submissions for this game. Better luck next time!" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameTooHighResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( user,
                                                 "25 * 4 * 5 * 2" ) ).isEqualTo( "You have put an answer that breaks the threshold of +-200, you lose 10 karma." );
        verify( karmaDB ).modNounKarma( "bob", -10 );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "There were no submissions for this game. Better luck next time!" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameInvalidSyntaxResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( user,
                                                 "hi Countdown!" ) ).isEqualTo( "Sorry bob! You have put an invalid answer, you lose 10 karma." );
        verify( karmaDB ).modNounKarma( "bob", -10 );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "There were no submissions for this game. Better luck next time!" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameInvalidNumberResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( user,
                                                 "500" ) ).isEqualTo( "Sorry bob! You have put an invalid answer, you lose 10 karma." );
        verify( karmaDB ).modNounKarma( "bob", -10 );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "There were no submissions for this game. Better luck next time!" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameDuplicateNumberResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( user,
                                                 "25 * 4 * 4 + 25 * 4" ) ).isEqualTo( "Sorry bob! You have put an invalid answer, you lose 10 karma." );
        verify( karmaDB ).modNounKarma( "bob", -10 );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "There were no submissions for this game. Better luck next time!" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameInRangeButNotCloseEnoughResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( user,
                                                 "25 * 4 * (1 + 3)" ) ).isEqualTo( "Your submission of 25 * 4 * (1 + 3) has been received!" );
        verifyNoInteractions( karmaDB );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "The final submissions are:" );
        verify( channel ).sendMessage( "bob, with 25 * 4 * (1 + 3) = 400" );
        verify( channel ).sendMessage( "There are no winners for this game. Better luck next time!" );
        verifyNoInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameCorrectResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( user,
                                                 "25 * 4 * 5" ) ).isEqualTo( "Your submission of 25 * 4 * 5 has been received!" );
        verifyNoInteractions( karmaDB );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "The final submissions are:" );
        verify( channel ).sendMessage( "bob, with 25 * 4 * 5 = 500" );
        verify( channel ).sendMessage( "The winners are :" );
        verify( channel ).sendMessage( "bob, awarded 10 karma." );
        verify( karmaDB ).modNounKarma( "bob", 10 );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameCorrectResponseDuplicates()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 525 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 5, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 5, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "625" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( user,
                                                 "25 * 5 * 5" ) ).isEqualTo( "Your submission of 25 * 5 * 5 has been received!" );
        verifyNoInteractions( karmaDB );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "625" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "The final submissions are:" );
        verify( channel ).sendMessage( "bob, with 25 * 5 * 5 = 625" );
        verify( channel ).sendMessage( "The winners are :" );
        verify( channel ).sendMessage( "bob, awarded 10 karma." );
        verify( karmaDB ).modNounKarma( "bob", 10 );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testGameMultipleAnswersResponse()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        Future future = mock( Future.class );

        when( channel.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );
        when( charlie.getNick() ).thenReturn( "charlie" );
        when( charlie.getClient() ).thenReturn( client );
        when( randomProvider.getRandomInt( anyInt() ) ).thenReturn( 1 ).thenReturn( 400 );
        when( randomProvider.shuffleList( any() ) ).thenReturn( List.of( 25, 50, 75,
                                                                         100 ) ).thenReturn( List.of( 1, 2, 3, 4, 5, 6,
                                                                                                      7, 8, 9, 10, 1, 2,
                                                                                                      3, 4, 5, 6, 7, 8,
                                                                                                      9, 10 ) );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 45 ) ), argumentCaptor.capture() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, bob, channel, "!countdown" );
        assertThat( countdownService.startNewGame( channelMessageEvent ) ).isEqualTo( Colors.DARK_GREEN
            + "Welcome to Countdown!" + Colors.NORMAL + "\n" + "See the game rules with !countdownrules.\n"
            + "Your numbers are:\n" + Colors.RED + "[25, 1, 2, 3, 4, 5]" + Colors.NORMAL + "\n" + "Your target is:\n"
            + Colors.RED + "500" + Colors.NORMAL + "\n" + "Good luck! You have 45 seconds.\n" + Colors.RED
            + "Use \" /msg \u000FCountdownB0t1\u000304 !solution < answer > \" to submit." + Colors.NORMAL );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 45 ) ), any() );
        verifyNoInteractions( karmaDB );

        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );

        assertThat( countdownService.onSolution( bob,
                                                 "25 * 4 * 5" ) ).isEqualTo( "Your submission of 25 * 4 * 5 has been received!" );
        verifyNoInteractions( karmaDB );
        verifyNoInteractions( future );

        assertThat( countdownService.onSolution( alice,
                                                 "25 * 4 * 5 - 3" ) ).isEqualTo( "Your submission of 25 * 4 * 5 - 3 has been received!" );
        verifyNoInteractions( karmaDB );
        verifyNoInteractions( future );

        assertThat( countdownService.onSolution( charlie,
                                                 "25 * 4 * (1 + 3)" ) ).isEqualTo( "Your submission of 25 * 4 * (1 + 3) has been received!" );
        verifyNoInteractions( karmaDB );
        verifyNoInteractions( future );

        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Your time is up! The target number was " + Colors.RED + "500" + Colors.NORMAL
            + "." );
        verify( channel ).sendMessage( "The final submissions are:" );
        verify( channel ).sendMessage( "alice, with 25 * 4 * 5 - 3 = 497" );
        verify( channel ).sendMessage( "bob, with 25 * 4 * 5 = 500" );
        verify( channel ).sendMessage( "charlie, with 25 * 4 * (1 + 3) = 400" );
        verify( channel ).sendMessage( "The winners are :" );
        verify( channel ).sendMessage( "alice, awarded 7 karma." );
        verify( channel ).sendMessage( "bob, awarded 10 karma." );
        verifyNoMoreInteractions( channel );
        verify( karmaDB ).modNounKarma( "alice", 7 );
        verify( karmaDB ).modNounKarma( "bob", 10 );
        verifyNoMoreInteractions( karmaDB );
    }
}
