package vilebot.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.oldterns.vilebot.handlers.user.Ascii;
import com.oldterns.vilebot.util.LimitCommand;

import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

public class LimitCommandTest
{

    Ascii asciiClass = new Ascii();

    ReceivePrivmsg event;

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
        event = mock( ReceivePrivmsg.class );
        when( event.getSender() ).thenReturn( "jucook" );
        when( event.getChannel() ).thenReturn( "#thefoobar" );
    }

    @Test
    public void testWithAscii()
        throws Exception
    {
        for ( int i = 0; i < 3; i++ )
        {
            runEvent();
        }
        TimeUnit.SECONDS.sleep( 1 );
        runEvent();
        verify( event, times( 1 ) ).reply( "jucook has the maximum uses" );
        verify( event, times( 3 ) ).reply( expectedReply );
    }

    private void runEvent()
    {
        String ircmsg = "!ascii #thefoobar";
        when( event.getText() ).thenReturn( ircmsg );
        asciiClass.ascii( event );
    }
}
