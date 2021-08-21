package com.oldterns.vilebot.services;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.vilebot.util.RandomProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class RockPaperScissorsServiceTest
{

    @Inject
    RockPaperScissorsService rockPaperScissorsService;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testPlayerGame()
    {
        Client client = Mockito.mock( Client.class );
        User alice = Mockito.mock( User.class );
        User bob = Mockito.mock( User.class );
        Channel channel = Mockito.mock( Channel.class );
        ServerMessage serverMessage = Mockito.mock( ServerMessage.class );
        when( client.getNick() ).thenReturn( "Bot" );
        when( channel.getClient() ).thenReturn( client );
        when( channel.getMessagingName() ).thenReturn( "#channel" );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );
        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, serverMessage, bob, channel, "!rps bob" );

        // verify you cannot challenge yourself
        assertThat( rockPaperScissorsService.startRpsGame( channelMessageEvent, Nick.valueOf( "bob" ),
                                                           Optional.empty() ) ).isEqualTo( "You cannot challenge yourself to a Rock Paper Scissors game!" );

        assertThat( rockPaperScissorsService.startRpsGame( channelMessageEvent, Nick.valueOf( "alice" ),
                                                           Optional.empty() ) ).isEqualTo( RockPaperScissorsService.RED
                                                               + "bob dared alice to a Rock Paper Scissors match!"
                                                               + RockPaperScissorsService.RESET + "\n"
                                                               + RockPaperScissorsService.RED + "Use \" /msg "
                                                               + RockPaperScissorsService.RESET + "Bot"
                                                               + RockPaperScissorsService.RED
                                                               + " !(rock|paper|scissors) \" to submit."
                                                               + RockPaperScissorsService.RESET );

        // verify starting a game while a game in progress gives an error message
        assertThat( rockPaperScissorsService.startRpsGame( channelMessageEvent, Nick.valueOf( "alice" ),
                                                           Optional.empty() ) ).isEqualTo( "A current game is already in progress.\n"
                                                               + RockPaperScissorsService.RED
                                                               + "bob dared alice to a Rock Paper Scissors match!"
                                                               + RockPaperScissorsService.RESET + "\n" );

        assertThat( rockPaperScissorsService.onContestantAnswer( alice,
                                                                 "rock" ) ).isEqualTo( "Your submission of rock has been received!" );

        // verify you can't change your answer once your submitted
        assertThat( rockPaperScissorsService.onContestantAnswer( alice,
                                                                 "scissors" ) ).isEqualTo( "You have already submitted your answer!\n" );

        assertThat( rockPaperScissorsService.onContestantAnswer( bob,
                                                                 "paper" ) ).isEqualTo( "Your submission of paper has been received!" );

        // paper beats rock
        Mockito.verify( channel ).sendMessage( RockPaperScissorsService.RED
            + "bob used paper, alice used rock, bob wins!" );

        // test all combinations
        List<Pair<String, String>> leftWinsPairList =
            List.of( Pair.of( "rock", "scissors" ), Pair.of( "paper", "rock" ), Pair.of( "scissors", "paper" ) );

        Mockito.reset( channel );
        for ( Pair<String, String> leftWinsPair : leftWinsPairList )
        {
            assertThat( rockPaperScissorsService.startRpsGame( channelMessageEvent, Nick.valueOf( "alice" ),
                                                               Optional.empty() ) ).isEqualTo( RockPaperScissorsService.RED
                                                                   + "bob dared alice to a Rock Paper Scissors match!"
                                                                   + RockPaperScissorsService.RESET + "\n"
                                                                   + RockPaperScissorsService.RED + "Use \" /msg "
                                                                   + RockPaperScissorsService.RESET + "Bot"
                                                                   + RockPaperScissorsService.RED
                                                                   + " !(rock|paper|scissors) \" to submit."
                                                                   + RockPaperScissorsService.RESET );
            assertThat( rockPaperScissorsService.onContestantAnswer( bob,
                                                                     leftWinsPair.getLeft() ) ).isEqualTo( "Your submission of "
                                                                         + leftWinsPair.getLeft()
                                                                         + " has been received!" );
            assertThat( rockPaperScissorsService.onContestantAnswer( alice,
                                                                     leftWinsPair.getRight() ) ).isEqualTo( "Your submission of "
                                                                         + leftWinsPair.getRight()
                                                                         + " has been received!" );
            Mockito.verify( channel ).sendMessage( RockPaperScissorsService.RED + "bob used " + leftWinsPair.getLeft()
                + ", alice used " + leftWinsPair.getRight() + ", bob wins!" );

            Mockito.reset( channel );

            assertThat( rockPaperScissorsService.startRpsGame( channelMessageEvent, Nick.valueOf( "alice" ),
                                                               Optional.empty() ) ).isEqualTo( RockPaperScissorsService.RED
                                                                   + "bob dared alice to a Rock Paper Scissors match!"
                                                                   + RockPaperScissorsService.RESET + "\n"
                                                                   + RockPaperScissorsService.RED + "Use \" /msg "
                                                                   + RockPaperScissorsService.RESET + "Bot"
                                                                   + RockPaperScissorsService.RED
                                                                   + " !(rock|paper|scissors) \" to submit."
                                                                   + RockPaperScissorsService.RESET );
            assertThat( rockPaperScissorsService.onContestantAnswer( bob,
                                                                     leftWinsPair.getRight() ) ).isEqualTo( "Your submission of "
                                                                         + leftWinsPair.getRight()
                                                                         + " has been received!" );
            assertThat( rockPaperScissorsService.onContestantAnswer( alice,
                                                                     leftWinsPair.getLeft() ) ).isEqualTo( "Your submission of "
                                                                         + leftWinsPair.getLeft()
                                                                         + " has been received!" );
            Mockito.verify( channel ).sendMessage( RockPaperScissorsService.RED + "bob used " + leftWinsPair.getRight()
                + ", alice used " + leftWinsPair.getLeft() + ", alice wins!" );

            Mockito.reset( channel );

            assertThat( rockPaperScissorsService.startRpsGame( channelMessageEvent, Nick.valueOf( "alice" ),
                                                               Optional.empty() ) ).isEqualTo( RockPaperScissorsService.RED
                                                                   + "bob dared alice to a Rock Paper Scissors match!"
                                                                   + RockPaperScissorsService.RESET + "\n"
                                                                   + RockPaperScissorsService.RED + "Use \" /msg "
                                                                   + RockPaperScissorsService.RESET + "Bot"
                                                                   + RockPaperScissorsService.RED
                                                                   + " !(rock|paper|scissors) \" to submit."
                                                                   + RockPaperScissorsService.RESET );
            assertThat( rockPaperScissorsService.onContestantAnswer( bob,
                                                                     leftWinsPair.getLeft() ) ).isEqualTo( "Your submission of "
                                                                         + leftWinsPair.getLeft()
                                                                         + " has been received!" );
            assertThat( rockPaperScissorsService.onContestantAnswer( alice,
                                                                     leftWinsPair.getLeft() ) ).isEqualTo( "Your submission of "
                                                                         + leftWinsPair.getLeft()
                                                                         + " has been received!" );
            Mockito.verify( channel ).sendMessage( RockPaperScissorsService.RED + "bob used " + leftWinsPair.getLeft()
                + ", alice used " + leftWinsPair.getLeft() + ", no one wins!" );
        }

    }

    @Test
    public void testBotGame()
    {
        Client client = Mockito.mock( Client.class );
        User bot = Mockito.mock( User.class );
        User bob = Mockito.mock( User.class );
        Channel channel = Mockito.mock( Channel.class );
        ServerMessage serverMessage = Mockito.mock( ServerMessage.class );
        when( client.getNick() ).thenReturn( "Bot" );
        when( channel.getClient() ).thenReturn( client );
        when( channel.getMessagingName() ).thenReturn( "#channel" );
        when( bot.getNick() ).thenReturn( "Bot" );
        when( bot.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );
        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, serverMessage, bob, channel, "!rps Bot" );

        when( randomProvider.getRandomElement( Mockito.any( String[].class ) ) ).thenReturn( "paper" );
        assertThat( rockPaperScissorsService.startRpsGame( channelMessageEvent, Nick.valueOf( "Bot" ),
                                                           Optional.empty() ) ).isEqualTo( RockPaperScissorsService.RED
                                                               + "bob dared Bot to a Rock Paper Scissors match!"
                                                               + RockPaperScissorsService.RESET + "\n"
                                                               + RockPaperScissorsService.RED + "Use \" /msg "
                                                               + RockPaperScissorsService.RESET + "Bot"
                                                               + RockPaperScissorsService.RED
                                                               + " !(rock|paper|scissors) \" to submit."
                                                               + RockPaperScissorsService.RESET );

        assertThat( rockPaperScissorsService.onContestantAnswer( bob,
                                                                 "rock" ) ).isEqualTo( "Your submission of rock has been received!" );

        Mockito.verify( channel ).sendMessage( RockPaperScissorsService.RED
            + "bob used rock, Bot used paper, Bot wins!" );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void testGetRPSRules()
    {
        Client client = Mockito.mock( Client.class );
        when( client.getNick() ).thenReturn( "Bot" );
        User bob = Mockito.mock( User.class );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );
        ActorEvent<User> event = Mockito.mock( ActorEvent.class );
        when( event.getClient() ).thenReturn( client );
        when( event.getActor() ).thenReturn( bob );
        rockPaperScissorsService.requestRules( event );
        verify( bob ).sendMessage( RockPaperScissorsService.RED + "RPS RULES: " );
        verify( bob ).sendMessage( RockPaperScissorsService.RESET
            + "1) Dare someone to play rock paper scissors with you! " );
        verify( bob ).sendMessage( RockPaperScissorsService.RESET
            + "2) Rock beats scissors, paper beats rocks, and scissors beat paper " );
        verify( bob ).sendMessage( "3) Use /msg Bot !(rock|paper|scissors) to set your action. Cannot be undone." );
        verifyNoMoreInteractions( bob );
    }

}
