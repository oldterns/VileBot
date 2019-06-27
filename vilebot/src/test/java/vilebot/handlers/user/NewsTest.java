/**
 * Copyright (C) 2019 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package vilebot.handlers.user;

import com.oldterns.vilebot.handlers.user.News;

import com.oldterns.vilebot.util.LimitCommand;
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
        newsClass.limitCommand = new LimitCommand();
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
        String ircmsg = "!news help";

        StringBuilder sb = new StringBuilder();
        sb.append( "News Categories (example: !news toronto):" );
        sb.append( "\n" );
        sb.append( " { top }" );
        sb.append( " { world }" );
        sb.append( " { canada }" );
        sb.append( " { usa }" );
        sb.append( " { britain }" );
        sb.append( " { politics }" );
        sb.append( " { business }" );
        sb.append( " { health }" );
        sb.append( " { arts }" );
        sb.append( " { tech }" );
        sb.append( " { indigenous }" );
        sb.append( " { sports }" );
        sb.append( " { mlb }" );
        sb.append( " { nba }" );
        sb.append( " { cfl }" );
        sb.append( " { nfl }" );
        sb.append( " { nhl }" );
        sb.append( " { soccer }" );
        sb.append( " { bc }" );
        sb.append( " { kamloops }" );
        sb.append( " { calgary }" );
        sb.append( " { edmonton }" );
        sb.append( " { saskatchewan }" );
        sb.append( " { saskatoon }" );
        sb.append( " { manitoba }" );
        sb.append( " { thunderbay }" );
        sb.append( " { sudbury }" );
        sb.append( " { windsor }" );
        sb.append( " { london }" );
        sb.append( " { waterloo }" );
        sb.append( " { toronto }" );
        sb.append( " { hamilton }" );
        sb.append( " { montreal }" );
        sb.append( " { nb }" );
        sb.append( " { pei }" );
        sb.append( " { ns }" );
        sb.append( " { newfoundland }" );
        sb.append( " { north }" );

        String expectedReply = sb.toString();
        when( event.getMessage() ).thenReturn( ircmsg );
        newsClass.onGenericMessage( event );

        for ( String line : expectedReply.split( "\n" ) )
        {
            verify( event, times( 1 ) ).respondPrivateMessage( line );
        }
    }

    @Test
    public void invalidCategoryTest()
    {
        String ircmsg = "!news invalid";

        String expectedReply = "No news feed available for invalid. Try '!news help' for available news categories.";
        when( event.getMessage() ).thenReturn( ircmsg );
        newsClass.onGenericMessage( event );

        verify( event, times( 1 ) ).respondWith( expectedReply );
    }

    @Test
    public void defaultCommand()
    {
        String ircmsg = "!news";

        when( event.getMessage() ).thenReturn( ircmsg );
        newsClass.onGenericMessage( event );

        verify( event, times( 3 ) ).respondWith( notNull() );
    }

    @Test
    public void validCommand()
    {
        String ircmsg = "!news business";

        when( event.getMessage() ).thenReturn( ircmsg );
        newsClass.onGenericMessage( event );

        verify( event, times( 3 ) ).respondWith( notNull() );
    }
}
