package com.oldterns.vilebot.util;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MangleNicksTest
{
    Channel channel;

    ChannelEvent channelEvent;

    ActorEvent<User> actorEvent;

    @BeforeEach
    @SuppressWarnings( "unchecked" )
    public void setup()
    {
        channel = mock( Channel.class );
        User salman = mock( User.class );
        User sasiddiq = mock( User.class );

        when( salman.getNick() ).thenReturn( "salman" );
        when( sasiddiq.getNick() ).thenReturn( "sasiddiq" );

        channelEvent = mock( ChannelEvent.class );
        actorEvent = mock( ActorEvent.class );
        when( channelEvent.getChannel() ).thenReturn( channel );
        when( actorEvent.getActor() ).thenReturn( salman );
        when( channel.getUsers() ).thenReturn( List.of( salman, sasiddiq ) );
    }

    @Test
    public void noNicks()
    {
        String messageText = "i am the karma police";
        String returnText1 = MangleNicks.mangleNicks( channelEvent, messageText );
        String returnText2 = MangleNicks.mangleNicks( actorEvent, messageText );

        assertThat( returnText1 ).isEqualTo( messageText );
        assertThat( returnText2 ).isEqualTo( messageText );
    }

    @Test
    public void oneNick()
    {
        String messageText = "salman is a man of many bots";
        String returnText1 = MangleNicks.mangleNicks( channelEvent, messageText );
        String returnText2 = MangleNicks.mangleNicks( actorEvent, messageText );
        String expectedReturn = "namlas is a man of many bots";

        assertThat( returnText1 ).isEqualTo( expectedReturn );
        assertThat( returnText2 ).isEqualTo( expectedReturn );
    }

    @Test
    public void multipleNicks()
    {
        Client client = mock( Client.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );

        when( channel.getClient() ).thenReturn( client );
        when( actorEvent.getActor().getClient() ).thenReturn( client );

        ChannelMessageEvent channelMessageEvent = new ChannelMessageEvent( client, sourceMessage, actorEvent.getActor(),
                                                                           channel, "salman is actually sasiddiq" );
        String messageText = "salman is actually sasiddiq";
        String returnText1 = MangleNicks.mangleNicks( channelEvent, messageText );
        String returnText2 = MangleNicks.mangleNicks( (ActorEvent<User>) channelMessageEvent, messageText );
        String expectedReturn = "namlas is actually qiddisas";

        assertThat( returnText1 ).isEqualTo( expectedReturn );
        assertThat( returnText2 ).isEqualTo( expectedReturn );
    }
}
