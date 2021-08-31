package com.oldterns.vilebot.services;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class KarmaTransferServiceTest
{

    @Inject
    KarmaTransferService karmaTransferService;

    @InjectMock
    TimeService timeService;

    @InjectMock
    KarmaDB karmaDB;

    @Test
    public void testTransferTimeout()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!transfer alice 1000" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 1000L ) );
        assertThat( karmaTransferService.transferKarma( channelMessageEvent, Nick.valueOf( "alice" ),
                                                        1000L ) ).isEqualTo( "bob wants to transfer 1000 karma to alice. alice, please !accept or !reject the transfer. You have 30 seconds to respond." );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), argumentCaptor.capture() );
        verify( channel ).getClient();
        verifyNoMoreInteractions( channel );
        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Transfer failed. No response was received within 30 seconds." );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    public void testStartingSecondTransferWhenATransferInProgress()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!transfer alice 1000" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 1000L ) );
        assertThat( karmaTransferService.transferKarma( channelMessageEvent, Nick.valueOf( "alice" ),
                                                        1000L ) ).isEqualTo( "bob wants to transfer 1000 karma to alice. alice, please !accept or !reject the transfer. You have 30 seconds to respond." );

        channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, user, channel, "!transfer alice 1000" );
        assertThat( karmaTransferService.transferKarma( channelMessageEvent, Nick.valueOf( "alice" ),
                                                        1000L ) ).isEqualTo( "A transfer is already active. bob wants to transfer 1000 karma to alice. alice, please !accept or !reject this transfer. You have 30 seconds to respond." );

        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass( Runnable.class );
        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), argumentCaptor.capture() );
        verify( channel, times( 2 ) ).getClient();
        verifyNoMoreInteractions( channel );
        argumentCaptor.getValue().run();
        verify( channel ).sendMessage( "Transfer failed. No response was received within 30 seconds." );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testAcceptedTransfer()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User bob = mock( User.class );
        User alice = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        Future future = mock( Future.class );

        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 30 ) ), any() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, bob, channel, "!transfer alice 1000" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 1000L ) );
        assertThat( karmaTransferService.transferKarma( channelMessageEvent, Nick.valueOf( "alice" ),
                                                        1000L ) ).isEqualTo( "bob wants to transfer 1000 karma to alice. alice, please !accept or !reject the transfer. You have 30 seconds to respond." );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), any() );

        channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, bob, channel, "!accept" );
        assertThat( karmaTransferService.acceptTransfer( channelMessageEvent ) ).isEqualTo( "Only alice may accept this transfer." );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
        verifyNoInteractions( future );

        channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, alice, channel, "!accept" );
        assertThat( karmaTransferService.acceptTransfer( channelMessageEvent ) ).isEqualTo( "Transfer success! bob has transferred 1000 karma to alice!" );
        verify( karmaDB ).getNounKarma( "bob" );
        verify( karmaDB ).modNounKarma( "bob", -1000L );
        verify( karmaDB ).modNounKarma( "alice", 1000L );
        verifyNoMoreInteractions( karmaDB );

        verify( future ).cancel( true );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testRejectedTransfer()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User bob = mock( User.class );
        User alice = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        Future future = mock( Future.class );

        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 30 ) ), any() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, bob, channel, "!transfer alice 1000" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 1000L ) );
        assertThat( karmaTransferService.transferKarma( channelMessageEvent, Nick.valueOf( "alice" ),
                                                        1000L ) ).isEqualTo( "bob wants to transfer 1000 karma to alice. alice, please !accept or !reject the transfer. You have 30 seconds to respond." );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), any() );

        channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, bob, channel, "!reject" );
        assertThat( karmaTransferService.rejectTransfer( channelMessageEvent ) ).isEqualTo( "Only alice may reject this transfer." );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
        verifyNoInteractions( future );

        channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, alice, channel, "!reject" );
        assertThat( karmaTransferService.rejectTransfer( channelMessageEvent ) ).isEqualTo( "alice has rejected bob's transfer." );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );

        verify( future ).cancel( true );
    }

    @Test
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void testCancelledTransfer()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User bob = mock( User.class );
        User alice = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        Future future = mock( Future.class );

        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );
        when( timeService.onTimeout( eq( Duration.ofSeconds( 30 ) ), any() ) ).thenReturn( future );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, bob, channel, "!transfer alice 1000" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 1000L ) );
        assertThat( karmaTransferService.transferKarma( channelMessageEvent, Nick.valueOf( "alice" ),
                                                        1000L ) ).isEqualTo( "bob wants to transfer 1000 karma to alice. alice, please !accept or !reject the transfer. You have 30 seconds to respond." );

        verify( timeService ).onTimeout( eq( Duration.ofSeconds( 30 ) ), any() );

        channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, alice, channel, "!cancel" );
        assertThat( karmaTransferService.cancelTransfer( channelMessageEvent ) ).isEqualTo( "Only bob may cancel this transfer." );
        verifyNoInteractions( future );

        channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, bob, channel, "!cancel" );
        assertThat( karmaTransferService.cancelTransfer( channelMessageEvent ) ).isEqualTo( "bob has cancelled this transfer." );
        verify( future ).cancel( true );

        channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, alice, channel, "!accept" );
        assertThat( karmaTransferService.acceptTransfer( channelMessageEvent ) ).isNull();
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    public void testNegativeTransferAmount()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!transfer alice 1000" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 1000L ) );
        assertThat( karmaTransferService.transferKarma( channelMessageEvent, Nick.valueOf( "alice" ),
                                                        -1000L ) ).isEqualTo( "-1000 isn't a valid amount. Transfer amount must be greater than 0, and you must have at least as much karma as the amount you want to transfer." );

        verifyNoInteractions( timeService );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
    }

    @Test
    public void testLackingKarmaTransferAmount()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        User user = mock( User.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, user, channel, "!transfer alice 1000" );

        when( karmaDB.getNounKarma( "bob" ) ).thenReturn( Optional.of( 10L ) );
        assertThat( karmaTransferService.transferKarma( channelMessageEvent, Nick.valueOf( "alice" ),
                                                        -1000L ) ).isEqualTo( "-1000 isn't a valid amount. Transfer amount must be greater than 0, and you must have at least as much karma as the amount you want to transfer." );

        verifyNoInteractions( timeService );
        verify( karmaDB ).getNounKarma( "bob" );
        verifyNoMoreInteractions( karmaDB );
    }
}
