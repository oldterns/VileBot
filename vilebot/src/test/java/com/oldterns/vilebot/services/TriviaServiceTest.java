package com.oldterns.vilebot.services;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.TestUrlStreamHandler;
import com.oldterns.vilebot.util.TimeService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class TriviaServiceTest
{

    @Inject
    TriviaService triviaService;

    @Inject
    TestUrlStreamHandler urlStreamHandler;

    @InjectMock
    KarmaDB karmaDB;

    @InjectMock
    TimeService timeService;

    static PrintStream realErrorStream = System.err;

    @BeforeAll
    public static void setupAll()
    {
        // Hide printed stack trace for the unit test that test the case the API fails
        realErrorStream = System.err;
        System.setErr( new PrintStream( new OutputStream()
        {
            public void write( int i )
            {
            }
        } ) );
    }

    @AfterAll
    public static void teardownAll()
    {
        // Restore the error stream
        System.setErr( realErrorStream );
    }

    @Test
    public void testTriviaGame()
    {
        urlStreamHandler.mockConnection( "http://jservice.io/api/random?count=100",
                                         TriviaServiceTest.class.getResourceAsStream( "trivia-example.json" ) );
        Client client = Mockito.mock( Client.class );
        User botUser = Mockito.mock( User.class );
        Channel channel = Mockito.mock( Channel.class );
        ServerMessage serverMessage = Mockito.mock( ServerMessage.class );
        Mockito.when( channel.getClient() ).thenReturn( client );
        Mockito.when( channel.getMessagingName() ).thenReturn( "#trivia" );
        Mockito.when( botUser.getClient() ).thenReturn( client );
        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, serverMessage, botUser, channel, "!jeopardy" );

        final AtomicReference<Runnable> timeoutCommand = new AtomicReference<>();
        final AtomicReference<Future<?>> timeoutFuture = new AtomicReference<>();
        Mockito.when( timeService.onTimeout( Mockito.eq( Duration.ofSeconds( 30 ) ),
                                             Mockito.any() ) ).thenAnswer( invocation -> {
                                                 timeoutCommand.set( invocation.getArgument( 1 ) );
                                                 Future<?> future = Mockito.mock( Future.class );
                                                 timeoutFuture.set( future );
                                                 return future;
                                             } );

        // Test a simple game
        assertThat( triviaService.startGame( channelMessageEvent ) ).isEqualTo( "Your category is: " + TriviaService.RED
            + "Math" + TriviaService.RESET + "\n" + "For " + TriviaService.GREEN + "5" + TriviaService.RESET
            + " karma:\n" + TriviaService.BLUE + "This number is the base used in binary" + TriviaService.RESET + "\n"
            + "30 seconds on the clock." );
        Mockito.verify( timeService ).onTimeout( Mockito.eq( Duration.ofSeconds( 30 ) ), Mockito.any() );

        assertThat( triviaService.startGame( channelMessageEvent ) ).isEqualTo( "A game is already in session!\n"
            + "Your category is: " + TriviaService.RED + "Math" + TriviaService.RESET + "\n" + "For "
            + TriviaService.GREEN + "5" + TriviaService.RESET + " karma:\n" + TriviaService.BLUE
            + "This number is the base used in binary" + TriviaService.RESET );

        User user = Mockito.mock( User.class );
        Mockito.when( user.getNick() ).thenReturn( "bob" );
        assertThat( triviaService.tryAnswer( user,
                                             "4" ) ).isEqualTo( "Sorry bob! That is incorrect, you lose 5 karma." );
        Mockito.verify( karmaDB ).modNounKarma( "bob", -5 );
        Mockito.verifyNoInteractions( timeoutFuture.get() );
        assertThat( triviaService.tryAnswer( user, "2" ) ).isEqualTo( "Congrats bob, you win 5 karma!" );
        Mockito.verify( karmaDB ).modNounKarma( "bob", 5 );
        Mockito.verify( timeoutFuture.get() ).cancel( true );

        // Test it uses the next valid question from the response
        assertThat( triviaService.startGame( channelMessageEvent ) ).isEqualTo( "Your category is: " + TriviaService.RED
            + "VileBot1" + TriviaService.RESET + "\n" + "For " + TriviaService.GREEN + "2" + TriviaService.RESET
            + " karma:\n" + TriviaService.BLUE + "This command starts a trivia game" + TriviaService.RESET + "\n"
            + "30 seconds on the clock." );

        assertThat( triviaService.tryAnswer( user,
                                             "!omgword" ) ).isEqualTo( "Sorry bob! That is incorrect, you lose 2 karma." );
        Mockito.verify( karmaDB ).modNounKarma( "bob", -2 );
        Mockito.verifyNoInteractions( timeoutFuture.get() );
        timeoutCommand.get().run();
        Mockito.verify( channel ).getClient();
        Mockito.verify( channel ).sendMessage( "Your 30 seconds is up! The answer we were looking for was:" );
        Mockito.verify( channel ).sendMessage( TriviaService.BLUE + "!jeopardy" + TriviaService.RESET );
        Mockito.verifyNoMoreInteractions( channel );

        // Test it fails correctly if API doesn't work
        assertThat( triviaService.startGame( channelMessageEvent ) ).isEqualTo( "I don't feel like playing." );

        // Test it refreshes correctly
        urlStreamHandler.mockConnection( "http://jservice.io/api/random?count=100", "[{\n"
            + "    \"question\": \"A final example\",\n" + "    \"invalid_count\": null,\n"
            + "    \"category\": { \"title\": \"Test\" },\n" + "    \"answer\": \"Unit Tests!\"\n" + "  }]" );
        assertThat( triviaService.startGame( channelMessageEvent ) ).isEqualTo( "Your category is: " + TriviaService.RED
            + "Test" + TriviaService.RESET + "\n" + "For " + TriviaService.GREEN + "5" + TriviaService.RESET
            + " karma:\n" + TriviaService.BLUE + "A final example" + TriviaService.RESET + "\n"
            + "30 seconds on the clock." );

    }
}
