package vilebot.util;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.handlers.user.Ascii;
import com.oldterns.vilebot.util.LimitCommand;
import org.junit.Before;
import org.junit.Test;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

import static org.mockito.Mockito.*;

public class LimitCommandTest
{

    private Ascii asciiClass = new Ascii();

    private MessageEvent event;

    private String expectedReply = "    _  _     _     _               __                   _                    \n"
        + "  _| || |_  | |_  | |__     ___   / _|   ___     ___   | |__     __ _   _ __ \n"
        + " |_  ..  _| | __| | '_ \\   / _ \\ | |_   / _ \\   / _ \\  | '_ \\   / _` | | '__|\n"
        + " |_      _| | |_  | | | | |  __/ |  _| | (_) | | (_) | | |_) | | (_| | | |   \n"
        + "   |_||_|    \\__| |_| |_|  \\___| |_|    \\___/   \\___/  |_.__/   \\__,_| |_|   \n"
        + "                                                                             \n";

    @Before
    public void setSenderAndChannel()
    {
        asciiClass.limitCommand = new LimitCommand( 2, 1 );
        event = mock( MessageEvent.class );
        User user = mock( User.class );
        Channel channel = mock( Channel.class );
        when( ( event.getUser() ) ).thenReturn( user );
        when( user.getNick() ).thenReturn( "jucook" );
        when( event.getChannel() ).thenReturn( channel );
        when( channel.getName() ).thenReturn( Vilebot.getConfig().get( "ircChannel1" ) );
    }

    @Test
    public void testWithAscii()
    {
        String ircmsg = "!ascii #thefoobar";
        when( event.getMessage() ).thenReturn( ircmsg );
        int maxUses = asciiClass.limitCommand.getMaxUses();
        for ( int i = 0; i < maxUses + 1; i++ )
        {
            asciiClass.onGenericMessage( event );
        }
        for ( String line : expectedReply.split( "\n" ) )
        {
            verify( event, times( maxUses ) ).respondWith( line );
        }
        verify( event, times( 1 ) ).respondWith( "jucook has the maximum uses" );
    }
}
