/**
 * Copyright (C) 2019 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package vilebot.handlers.user;

import com.oldterns.vilebot.handlers.user.FakeNews;

import org.junit.Before;
import org.junit.Test;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

import static org.mockito.Mockito.*;

public class FakeNewsTest
{
    private FakeNews fakeNewsClass = new FakeNews();

    private MessageEvent event;

    @Before
    public void setup()
    {
        event = mock( MessageEvent.class );
        User user = mock( User.class );
        Channel channel = mock( Channel.class );
        when( event.getUser() ).thenReturn( user );
        when( event.getChannel() ).thenReturn( channel );
        when( channel.getName() ).thenReturn( "#thefoobar" );
    }

    @Test
    public void helpMsgTest()
    {
        String ircmsg = "!fakenews help";

        StringBuilder sb = new StringBuilder();

        sb.append( "Fake News Categories (example: !fakenews canada):" );
        sb.append( "\n" );
        sb.append( " { canada }" );
        sb.append( " { usa }" );
        sb.append( " { belgium }" );
        sb.append( " { france }" );
        sb.append( " { india }" );
        sb.append( " { russia }" );
        sb.append( " { serbia }" );
        sb.append( " { venezuela }" );
        sb.append( " { newzealand }" );

        String expectedReply = sb.toString();
        when( event.getMessage() ).thenReturn( ircmsg );
        fakeNewsClass.onGenericMessage( event );

        for ( String line : expectedReply.split( "\n" ) )
        {
            verify( event, times( 1 ) ).respondPrivateMessage( line );
        }
    }

    @Test
    public void invalidCategoryTest()
    {
        String ircmsg = "!fakenews invalid";

        String expectedReply = "No news feed available for invalid";
        when( event.getMessage() ).thenReturn( ircmsg );
        fakeNewsClass.onGenericMessage( event );

        verify( event, times( 1 ) ).respondWith( expectedReply );
    }

    @Test
    public void defaultCommand()
    {
        String ircmsg = "!fakenews";

        when( event.getMessage() ).thenReturn( ircmsg );
        fakeNewsClass.onGenericMessage( event );

        verify( event, times( 3 ) ).respondWith( notNull() );
    }

    @Test
    public void validCommand()
    {
        String ircmsg = "!fakenews russia";

        when( event.getMessage() ).thenReturn( ircmsg );
        fakeNewsClass.onGenericMessage( event );

        verify( event, times( 3 ) ).respondWith( notNull() );
    }
}
