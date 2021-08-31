package com.oldterns.vilebot.services;

import java.util.List;

import javax.inject.Inject;
import javax.naming.LimitExceededException;

import com.oldterns.vilebot.util.LimitService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
public class SummonServiceTest
{

    @Inject
    SummonService summonService;

    @InjectMock
    LimitService limitService;

    @Test
    public void testSummon()
    {
        Client client = Mockito.mock( Client.class );
        User user = Mockito.mock( User.class );
        Channel channel = Mockito.mock( Channel.class );
        ServerMessage serverMessage = Mockito.mock( ServerMessage.class );
        Mockito.when( channel.getClient() ).thenReturn( client );
        Mockito.when( channel.getMessagingName() ).thenReturn( "#channel" );
        Mockito.when( channel.getUsers() ).thenReturn( List.of( user ) );
        Mockito.when( user.getNick() ).thenReturn( "alice" );
        Mockito.when( user.getClient() ).thenReturn( client );
        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, serverMessage, user, channel, "!summon bob" );
        assertThat( summonService.summonNick( channelMessageEvent, "bob" ) ).isEqualTo( "bob was invited" );
        verify( client ).sendMessage( "bob", "You are summoned by alice to join #channel" );
    }

    @Test
    public void testUserInChannelSummon()
    {
        Client client = Mockito.mock( Client.class );
        User user = Mockito.mock( User.class );
        User targetUser = Mockito.mock( User.class );
        Channel channel = Mockito.mock( Channel.class );
        ServerMessage serverMessage = Mockito.mock( ServerMessage.class );
        Mockito.when( channel.getClient() ).thenReturn( client );
        Mockito.when( channel.getMessagingName() ).thenReturn( "#channel" );
        Mockito.when( user.getNick() ).thenReturn( "alice" );
        Mockito.when( user.getClient() ).thenReturn( client );
        Mockito.when( targetUser.getNick() ).thenReturn( "bob" );
        Mockito.when( targetUser.getClient() ).thenReturn( client );
        Mockito.when( channel.getUsers() ).thenReturn( List.of( user, targetUser ) );
        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, serverMessage, user, channel, "!summon bob" );
        assertThat( summonService.summonNick( channelMessageEvent,
                                              "bob" ) ).isEqualTo( "bob is already in the channel." );
        verifyNoInteractions( client );
    }

    @Test
    public void testOverLimit()
        throws LimitExceededException
    {
        Client client = Mockito.mock( Client.class );
        User user = Mockito.mock( User.class );
        Channel channel = Mockito.mock( Channel.class );
        ServerMessage serverMessage = Mockito.mock( ServerMessage.class );
        Mockito.when( channel.getClient() ).thenReturn( client );
        Mockito.when( channel.getMessagingName() ).thenReturn( "#channel" );
        Mockito.when( user.getNick() ).thenReturn( "alice" );
        Mockito.when( user.getClient() ).thenReturn( client );
        Mockito.when( channel.getUsers() ).thenReturn( List.of( user ) );
        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, serverMessage, user, channel, "!summon bob" );
        Mockito.doThrow( new LimitExceededException( "alice has the maximum uses" ) ).when( limitService ).addUse( user );
        assertThat( summonService.summonNick( channelMessageEvent, "bob" ) ).isEqualTo( "alice has the maximum uses" );
        verifyNoInteractions( client );
    }
}
