/**
 * Copyright (C) 2019 Newterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package vilebot.handlers.user;

import com.oldterns.vilebot.handlers.user.News;

import org.junit.Before;
import org.junit.Test;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

import static org.mockito.Mockito.*;

public class NewsTest
{
    private News newsClass = new News();

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
    public void defaultNewsTest()
    {
        String ircmsg = "!news";
        String expectedReply = "Get headlines";
        when( event.getMessage() ).thenReturn( ircmsg );
        newsClass.onGenericMessage( event );
        verify( event, times( 1 ) ).respondWith( expectedReply );
    }

}
