package vilebot.handlers.user;

import org.junit.Before;
import org.junit.Test;

import com.oldterns.vilebot.handlers.user.Trivia;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import org.pircbotx.Channel;
import org.pircbotx.hooks.events.MessageEvent;

public class TriviaTest
{

    private MessageEvent event;

    private Channel channel;

    private static final String VILEBOT_CONFIG_FILE = "cfg/vilebot.conf";

    private static final String VILEBOT_CONFIG_FILE_SECONDARY = "cfg/vilebot.conf.example";

    @Before
    public void setup()
    {
        event = mock( MessageEvent.class );
        channel = mock( Channel.class );
        when( event.getChannel() ).thenReturn( channel );
        when( channel.getName() ).thenReturn( getJeopardyChannel() );
    }

    @Test
    public void verifyNotInChannelErrorTest()
    {
        when( event.getMessage() ).thenReturn( "!jeopardy" );
        when( channel.getName() ).thenReturn( getJeopardyChannel() + "blah" );

        Trivia trivia = new Trivia();
        trivia.onGenericMessage( event );

        verify( event, times( 1 ) ).respondWith( "To play jeopardy join: " + getJeopardyChannel() );
    }

    @Test
    public void verifyRightChannelStartsGameTest()
    {
        String correctReply = "Welcome to Bot Jeopardy!";
        Channel channel = mock( Channel.class );
        when( event.getMessage() ).thenReturn( "!jeopardy" );
        when( event.getChannel() ).thenReturn( channel );
        when( channel.getName() ).thenReturn( getJeopardyChannel() );

        Trivia trivia = new Trivia();
        trivia.onGenericMessage( event );

        verify( event, times( 1 ) ).respondWith( correctReply );
    }

    private String getJeopardyChannel()
    {
        String configLine = "";
        String jeopardyChannel = "";

        BufferedReader configFile;

        try
        {
            if ( new File( VILEBOT_CONFIG_FILE ).exists() )
            {
                configFile = new BufferedReader( new FileReader( VILEBOT_CONFIG_FILE ) );
            }
            else
            {
                configFile = new BufferedReader( new FileReader( VILEBOT_CONFIG_FILE_SECONDARY ) );
            }
            while ( ( configLine = configFile.readLine() ) != null )
            {
                if ( configLine.length() > 16 )
                {
                    jeopardyChannel = configLine.substring( 0, 15 );
                }
                if ( "jeopardyChannel".equals( jeopardyChannel ) )
                {
                    break;
                }
            }
        }
        catch ( Exception e )
        {
            System.out.println( "Error reading config file: " + e.getMessage() );
        }

        return configLine.substring( 16, configLine.length() );
    }
}
