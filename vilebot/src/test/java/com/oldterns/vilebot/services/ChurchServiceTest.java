package com.oldterns.vilebot.services;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.vilebot.database.ChurchDB;
import com.oldterns.vilebot.database.KarmaDB;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ChurchServiceTest
{

    @Inject
    ChurchService churchService;

    @InjectMock
    TimeService timeService;

    @InjectMock
    KarmaDB karmaDB;

    @InjectMock
    ChurchDB churchDB;

    @Test
    public void testDonateNoKarma()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.empty() );
        assertThat( churchService.donate( user, 10 ) ).isEqualTo( "You have insufficient karma to donate." );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
        verifyNoInteractions( churchDB );
    }

    @Test
    public void testDonateInsufficientKarma()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 5L ) );
        assertThat( churchService.donate( user, 10 ) ).isEqualTo( "You have insufficient karma to donate." );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
        verifyNoInteractions( churchDB );
    }

    @Test
    public void testDonateNegativeKarma()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 10L ) );
        assertThat( churchService.donate( user, -5 ) ).isEqualTo( "You cannot donate a non-positive number." );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
        verifyNoInteractions( churchDB );
    }

    @Test
    public void testDonate()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 10L ) );
        when( churchDB.getDonorKarma( "bob" ) ).thenReturn( Optional.of( 10L ) );
        assertThat( churchService.donate( user, 10 ) ).isEqualTo( "Thank you for your donation of 10 bob!" );
        verify( karmaDB ).getNounKarma( "bob" );
        verify( karmaDB ).modNounKarma( "bob", -10 );
        verifyNoMoreInteractions( karmaDB );
        verify( churchDB ).getDonorKarma( "bob" );
        verify( churchDB ).modDonorKarma( "bob", 10 );
        verify( churchDB ).modDonorTitle( "bob", " " );
        verifyNoMoreInteractions( churchDB );
    }

    @Test
    public void testDonorDonate()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( churchDB.getDonorTitle( "bob" ) ).thenReturn( "bob the king" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 10L ) );
        when( churchDB.getDonorKarma( "bob" ) ).thenReturn( Optional.of( 20L ) );
        assertThat( churchService.donate( user, 10 ) ).isEqualTo( "Thank you for your donation of 10 bob!" );
        verify( karmaDB ).getNounKarma( "bob" );
        verify( karmaDB ).modNounKarma( "bob", -10 );
        verifyNoMoreInteractions( karmaDB );
        verify( churchDB ).getDonorKarma( "bob" );
        verify( churchDB ).modDonorKarma( "bob", 10 );
        verifyNoMoreInteractions( churchDB );
    }

    @Test
    public void testDonateDuringAVote()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorTitle( "bob" ) ).thenReturn( "bob the king" );
        when( churchDB.getDonorKarma( "bob" ) ).thenReturn( Optional.of( 20L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( Set.of( "alice", "bob", "charlie", "david" ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!inquisit" );
        churchService.inquisit( channelMessageEvent, user, Nick.valueOf( "bob" ) );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( any(), endVote.capture() );
        // Don't care about interactions from inquisit
        Mockito.reset( karmaDB, churchDB );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 10L ) );
        assertThat( churchService.donate( user,
                                          10 ) ).isEqualTo( "There is an ongoing vote, you cannot donate at this time." );
        verifyNoInteractions( karmaDB );
        verifyNoInteractions( churchDB );

        // end the vote so state goes back to normal for other tests
        endVote.getValue().run();
    }

    @Test
    public void testChurchTotal()
    {
        when( churchDB.getTotalDonations() ).thenReturn( 100L );
        when( churchDB.getTotalNonDonations() ).thenReturn( 50L );
        assertThat( churchService.churchTotal() ).isEqualTo( "The church coffers contains 150, of which 100 was contributed by its loyal believers." );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testTopDonorsNoDonors()
    {
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( null );
        assertThat( churchService.topDonors() ).isEqualTo( "The church does not have enough followers yet." );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( Collections.emptySet() );
        assertThat( churchService.topDonors() ).isEqualTo( "The church does not have enough followers yet." );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testTopDonors()
    {
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob",
                                                                                              "charlie" ) ) );
        when( churchDB.getDonorTitle( "alice" ) ).thenReturn( " " );
        when( churchDB.getDonorKarma( "alice" ) ).thenReturn( Optional.of( 20L ) );
        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorKarma( "bob" ) ).thenReturn( Optional.of( 10L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorTitle( "bob" ) ).thenReturn( "bob the king" );
        when( churchDB.getDonorKarma( "charlie" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorTitle( "charlie" ) ).thenReturn( "charlie the joker" );
        assertThat( churchService.topDonors() ).isEqualTo( "NICK           AMOUNT    TITLE\n"
            + "ecila          20         \n" + "bob            10        bob the king\n"
            + "eilrahc        5         charlie the joker" );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testSetTitleNonDonor()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.empty() );
        assertThat( churchService.setTitle( user,
                                            "bob the king" ) ).isEqualTo( "You must be a top donor to set your title." );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testSetTitleNonTopDonor()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 5L ) );
        assertThat( churchService.setTitle( user,
                                            "bob the king" ) ).isEqualTo( "You must be a top donor to set your title." );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testSetTitle()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 4L ) );
        when( churchDB.getDonorTitle( "bob" ) ).thenReturn( "bob the knight" );
        assertThat( churchService.setTitle( user,
                                            "bob the king" ) ).isEqualTo( "bob is now to be referred to as bob the king instead of bob the knight." );
        verify( churchDB ).modDonorTitle( "bob", "bob the king" );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testSetTooLongTitle()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 4L ) );
        when( churchDB.getDonorTitle( "bob" ) ).thenReturn( "bob the knight" );
        assertThat( churchService.setTitle( user,
                                            "bob the super amazing kingly heavenly king" ) ).isEqualTo( "bob is now to be referred to as bob the super amazing kingly heavenly k instead of bob the knight." );
        verify( churchDB ).modDonorTitle( "bob", "bob the super amazing kingly heavenly k" );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testInquisitionNonDonor()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!inquisit" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.empty() );
        assertThat( churchService.inquisit( channelMessageEvent, user,
                                            Nick.valueOf( "bob" ) ) ).isEqualTo( "You must be a top donor to start an inquisition." );
        verifyNoInteractions( timeService );
    }

    @Test
    public void testInquisitionNonTopDonor()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!inquisit" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 5L ) );
        assertThat( churchService.inquisit( channelMessageEvent, user,
                                            Nick.valueOf( "bob" ) ) ).isEqualTo( "You must be a top donor to start an inquisition." );
        verifyNoInteractions( timeService );
    }

    @Test
    public void testInquisitionNotEnoughMembers()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( user.getNick() ).thenReturn( "bob" );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!inquisit" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( Set.of( "alice", "bob" ) );

        assertThat( churchService.inquisit( channelMessageEvent, user,
                                            Nick.valueOf( "bob" ) ) ).isEqualTo( "There are not enough members of the church to invoke a vote" );
        verifyNoInteractions( timeService );
    }

    @Test
    public void testInquisitionAgainstSomeoneWithNoDonations()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User bob = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( bob.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, bob, channel, "!inquisit" );

        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorKarma( "charlie" ) ).thenReturn( Optional.empty() );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( Set.of( "alice", "bob", "member2", "member3" ) );

        assertThat( churchService.inquisit( channelMessageEvent, bob,
                                            Nick.valueOf( "charlie" ) ) ).isEqualTo( "You cannot start an inquisition against someone who has no donation value." );
        verifyNoInteractions( timeService );
    }

    @Test
    public void testInquisitionNoVotes()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        User david = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( bob.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );

        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorRank( "david" ) ).thenReturn( Optional.of( 4L ) );

        when( churchDB.getDonorKarma( "user" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob", "charlie",
                                                                                              "david" ) ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, bob, channel, "!inquisit user" );
        assertThat( churchService.inquisit( channelMessageEvent, bob,
                                            Nick.valueOf( "user" ) ) ).isEqualTo( "An inquisition has started against user. Please cast your votes with !aye or !nay\n"
                                                + "alice bob charlie david " );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), endVote.capture() );

        endVote.getValue().run();

        verify( channel ).sendMessage( "Voting is now finished" );
        verify( channel ).sendMessage( "The vote to inquisit user has failed. Nothing will happen." );
        verify( churchDB, never() ).modDonorKarma( any(), any() );
        verify( churchDB, never() ).modNonDonorKarma( any() );
        verify( churchDB, never() ).removeDonor( any() );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testInquisitionNotEnoughVotes()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        User david = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );

        when( alice.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( bob.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( charlie.getClient() ).thenReturn( client );
        when( charlie.getNick() ).thenReturn( "charlie" );
        when( david.getClient() ).thenReturn( client );
        when( david.getNick() ).thenReturn( "david" );

        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorRank( "david" ) ).thenReturn( Optional.of( 4L ) );

        when( churchDB.getDonorKarma( "user" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob", "charlie",
                                                                                              "david" ) ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, bob, channel, "!inquisit user" );
        assertThat( churchService.inquisit( channelMessageEvent, bob,
                                            Nick.valueOf( "user" ) ) ).isEqualTo( "An inquisition has started against user. Please cast your votes with !aye or !nay\n"
                                                + "alice bob charlie david " );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), endVote.capture() );

        assertThat( churchService.voteAye( charlie ) ).isEqualTo( "alice (false) bob (true) charlie (true) david (false) " );

        endVote.getValue().run();

        verify( channel ).sendMessage( "Voting is now finished" );
        verify( channel ).sendMessage( "The vote to inquisit user has failed. Nothing will happen." );
        verify( churchDB, never() ).modDonorKarma( any(), any() );
        verify( churchDB, never() ).modNonDonorKarma( any() );
        verify( churchDB, never() ).removeDonor( any() );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testInquisitionBottomThreeCanInquisit()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        User david = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );

        when( alice.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( bob.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( charlie.getClient() ).thenReturn( client );
        when( charlie.getNick() ).thenReturn( "charlie" );
        when( david.getClient() ).thenReturn( client );
        when( david.getNick() ).thenReturn( "david" );

        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorRank( "david" ) ).thenReturn( Optional.of( 4L ) );

        when( churchDB.getDonorKarma( "user" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob", "charlie",
                                                                                              "david" ) ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, bob, channel, "!inquisit user" );
        assertThat( churchService.inquisit( channelMessageEvent, bob,
                                            Nick.valueOf( "user" ) ) ).isEqualTo( "An inquisition has started against user. Please cast your votes with !aye or !nay\n"
                                                + "alice bob charlie david " );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), endVote.capture() );

        assertThat( churchService.voteAye( charlie ) ).isEqualTo( "alice (false) bob (true) charlie (true) david (false) " );
        assertThat( churchService.voteAye( david ) ).isEqualTo( "alice (false) bob (true) charlie (true) david (true) " );

        endVote.getValue().run();

        verify( channel ).sendMessage( "Voting is now finished" );
        verify( channel ).sendMessage( "The vote to inquisit user has passed. user will be stripped of their karma." );
        verify( churchDB, never() ).modDonorKarma( any(), any() );
        verify( churchDB ).modNonDonorKarma( 5 );
        verify( churchDB ).removeDonor( "user" );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testTopCannotInquisitAlone()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        User david = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );

        when( alice.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( bob.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( charlie.getClient() ).thenReturn( client );
        when( charlie.getNick() ).thenReturn( "charlie" );
        when( david.getClient() ).thenReturn( client );
        when( david.getNick() ).thenReturn( "david" );

        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorRank( "david" ) ).thenReturn( Optional.of( 4L ) );

        when( churchDB.getDonorKarma( "user" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob", "charlie",
                                                                                              "david" ) ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!inquisit user" );
        assertThat( churchService.inquisit( channelMessageEvent, bob,
                                            Nick.valueOf( "user" ) ) ).isEqualTo( "An inquisition has started against user. Please cast your votes with !aye or !nay\n"
                                                + "alice bob charlie david " );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), endVote.capture() );

        endVote.getValue().run();

        verify( channel ).sendMessage( "Voting is now finished" );
        verify( channel ).sendMessage( "The vote to inquisit user has failed. Nothing will happen." );
        verify( churchDB, never() ).modDonorKarma( any(), any() );
        verify( churchDB, never() ).modNonDonorKarma( any() );
        verify( churchDB, never() ).removeDonor( any() );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testTopCannotInquisitWithBottomDonor()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        User david = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );

        when( alice.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( bob.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( charlie.getClient() ).thenReturn( client );
        when( charlie.getNick() ).thenReturn( "charlie" );
        when( david.getClient() ).thenReturn( client );
        when( david.getNick() ).thenReturn( "david" );

        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorRank( "david" ) ).thenReturn( Optional.of( 4L ) );

        when( churchDB.getDonorKarma( "user" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob", "charlie",
                                                                                              "david" ) ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!inquisit user" );
        assertThat( churchService.inquisit( channelMessageEvent, alice,
                                            Nick.valueOf( "user" ) ) ).isEqualTo( "An inquisition has started against user. Please cast your votes with !aye or !nay\n"
                                                + "alice bob charlie david " );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), endVote.capture() );

        assertThat( churchService.voteAye( david ) ).isEqualTo( "alice (true) bob (false) charlie (false) david (true) " );

        endVote.getValue().run();

        verify( channel ).sendMessage( "Voting is now finished" );
        verify( channel ).sendMessage( "The vote to inquisit user has failed. Nothing will happen." );
        verify( churchDB, never() ).modDonorKarma( any(), any() );
        verify( churchDB, never() ).modNonDonorKarma( any() );
        verify( churchDB, never() ).removeDonor( any() );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testTopCanInquisitWithSecondLowestTopDonor()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        User david = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );

        when( alice.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( bob.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( charlie.getClient() ).thenReturn( client );
        when( charlie.getNick() ).thenReturn( "charlie" );
        when( david.getClient() ).thenReturn( client );
        when( david.getNick() ).thenReturn( "david" );

        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorRank( "david" ) ).thenReturn( Optional.of( 4L ) );

        when( churchDB.getDonorKarma( "user" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob", "charlie",
                                                                                              "david" ) ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!inquisit user" );
        assertThat( churchService.inquisit( channelMessageEvent, alice,
                                            Nick.valueOf( "user" ) ) ).isEqualTo( "An inquisition has started against user. Please cast your votes with !aye or !nay\n"
                                                + "alice bob charlie david " );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), endVote.capture() );

        assertThat( churchService.voteAye( charlie ) ).isEqualTo( "alice (true) bob (false) charlie (true) david (false) " );

        endVote.getValue().run();

        verify( channel ).sendMessage( "Voting is now finished" );
        verify( channel ).sendMessage( "The vote to inquisit user has passed. user will be stripped of their karma." );
        verify( churchDB, never() ).modDonorKarma( any(), any() );
        verify( churchDB ).modNonDonorKarma( 5 );
        verify( churchDB ).removeDonor( "user" );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testStarterCanNayToChangeTheirVote()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        User david = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );

        when( alice.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( bob.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( charlie.getClient() ).thenReturn( client );
        when( charlie.getNick() ).thenReturn( "charlie" );
        when( david.getClient() ).thenReturn( client );
        when( david.getNick() ).thenReturn( "david" );

        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorRank( "david" ) ).thenReturn( Optional.of( 4L ) );

        when( churchDB.getDonorKarma( "user" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob", "charlie",
                                                                                              "david" ) ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!inquisit user" );
        assertThat( churchService.inquisit( channelMessageEvent, alice,
                                            Nick.valueOf( "user" ) ) ).isEqualTo( "An inquisition has started against user. Please cast your votes with !aye or !nay\n"
                                                + "alice bob charlie david " );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), endVote.capture() );

        assertThat( churchService.voteAye( charlie ) ).isEqualTo( "alice (true) bob (false) charlie (true) david (false) " );
        assertThat( churchService.voteNay( alice ) ).isEqualTo( "alice (false) bob (false) charlie (true) david (false) " );

        endVote.getValue().run();

        verify( channel ).sendMessage( "Voting is now finished" );
        verify( channel ).sendMessage( "The vote to inquisit user has failed. Nothing will happen." );
        verify( churchDB, never() ).modDonorKarma( any(), any() );
        verify( churchDB, never() ).modNonDonorKarma( any() );
        verify( churchDB, never() ).removeDonor( any() );
        verifyNoInteractions( karmaDB );
    }

    @Test
    public void testDonorCanNayToChangeTheirVote()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User alice = mock( User.class );
        User bob = mock( User.class );
        User charlie = mock( User.class );
        User david = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );

        when( alice.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( bob.getClient() ).thenReturn( client );
        when( bob.getNick() ).thenReturn( "bob" );
        when( charlie.getClient() ).thenReturn( client );
        when( charlie.getNick() ).thenReturn( "charlie" );
        when( david.getClient() ).thenReturn( client );
        when( david.getNick() ).thenReturn( "david" );

        when( churchDB.getDonorRank( "alice" ) ).thenReturn( Optional.of( 1L ) );
        when( churchDB.getDonorRank( "bob" ) ).thenReturn( Optional.of( 2L ) );
        when( churchDB.getDonorRank( "charlie" ) ).thenReturn( Optional.of( 3L ) );
        when( churchDB.getDonorRank( "david" ) ).thenReturn( Optional.of( 4L ) );

        when( churchDB.getDonorKarma( "user" ) ).thenReturn( Optional.of( 5L ) );
        when( churchDB.getDonorsByRanks( 0L, 3L ) ).thenReturn( new LinkedHashSet<>( List.of( "alice", "bob", "charlie",
                                                                                              "david" ) ) );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!inquisit user" );
        assertThat( churchService.inquisit( channelMessageEvent, alice,
                                            Nick.valueOf( "user" ) ) ).isEqualTo( "An inquisition has started against user. Please cast your votes with !aye or !nay\n"
                                                + "alice bob charlie david " );
        ArgumentCaptor<Runnable> endVote = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), endVote.capture() );

        assertThat( churchService.voteAye( charlie ) ).isEqualTo( "alice (true) bob (false) charlie (true) david (false) " );
        assertThat( churchService.voteNay( charlie ) ).isEqualTo( "alice (true) bob (false) charlie (false) david (false) " );

        endVote.getValue().run();

        verify( channel ).sendMessage( "Voting is now finished" );
        verify( channel ).sendMessage( "The vote to inquisit user has failed. Nothing will happen." );
        verify( churchDB, never() ).modDonorKarma( any(), any() );
        verify( churchDB, never() ).modNonDonorKarma( any() );
        verify( churchDB, never() ).removeDonor( any() );
        verifyNoInteractions( karmaDB );
    }
}
