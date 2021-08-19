package com.oldterns.vilebot.services;

import java.time.Duration;
import java.util.Optional;

import javax.inject.Inject;

import com.oldterns.vilebot.database.LastSeenDB;
import com.oldterns.vilebot.util.TimeService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class LastSeenServiceTest
{

    @Inject
    LastSeenService lastSeenService;

    @InjectMock
    LastSeenDB lastSeenDB;

    @InjectMock
    TimeService timeService;

    @Test
    @SuppressWarnings( "unchecked" )
    public void testUpdateTimeWhenUserActs()
    {
        ActorEvent<User> actorEvent = mock( ActorEvent.class );
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( actorEvent.getActor() ).thenReturn( user );

        lastSeenService.updateLastSeenTime( actorEvent );

        verify( lastSeenDB ).updateLastSeenTime( "bob" );
        verifyNoMoreInteractions( lastSeenDB );
    }

    @Test
    public void testNoCommentWhenUserJoinedThatWasNeverSeen()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( channel.getClient() ).thenReturn( client );
        when( user.getClient() ).thenReturn( client );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );

        when( lastSeenDB.getLastSeenTime( "bob" ) ).thenReturn( Optional.empty() );

        assertThat( lastSeenService.onJoin( channelJoinEvent ) ).isNull();

        verify( lastSeenDB ).getLastSeenTime( "bob" );
        verify( lastSeenDB ).updateLastSeenTime( "bob" );
        verifyNoMoreInteractions( lastSeenDB );
    }

    @Test
    public void testNoCommentWhenUserJoinedThatWasRecentlySeen()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( channel.getClient() ).thenReturn( client );
        when( user.getClient() ).thenReturn( client );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );

        when( timeService.getCurrentTimeMills() ).thenReturn( 10000L );
        when( lastSeenDB.getLastSeenTime( "bob" ) ).thenReturn( Optional.of( 5000L ) );

        assertThat( lastSeenService.onJoin( channelJoinEvent ) ).isNull();

        verify( lastSeenDB ).getLastSeenTime( "bob" );
        verify( lastSeenDB ).updateLastSeenTime( "bob" );
        verifyNoMoreInteractions( lastSeenDB );
    }

    @Test
    public void testCommentWhenUserJoinedThatWasLastSeenLongAgo()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );
        ServerMessage sourceMessage = mock( ServerMessage.class );
        User user = mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( channel.getClient() ).thenReturn( client );
        when( user.getClient() ).thenReturn( client );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );

        when( timeService.getCurrentTimeMills() ).thenReturn( Duration.ofDays( 40 ).toMillis() );
        when( lastSeenDB.getLastSeenTime( "bob" ) ).thenReturn( Optional.of( 5000L ) );

        assertThat( lastSeenService.onJoin( channelJoinEvent ) ).isEqualTo( "Hi bob! I last saw you 39 days ago at 1969-12-31T19:00-05. Long time, no see." );

        verify( lastSeenDB ).getLastSeenTime( "bob" );
        verify( lastSeenDB ).updateLastSeenTime( "bob" );
        verifyNoMoreInteractions( lastSeenDB );
    }
}
