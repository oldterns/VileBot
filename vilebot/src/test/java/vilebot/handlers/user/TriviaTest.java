package vilebot.handlers.user;

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

public class TriviaTest
{

    ReceivePrivmsg event = mock( ReceivePrivmsg.class );

    private static final String VILEBOT_CONFIG_FILE = "cfg/vilebot.conf";

    private static final String VILEBOT_CONFIG_FILE_SECONDARY = "cfg/vilebot.conf.example";

    @Test
    public void verifyNotInChannelErrorTest()
    {
        StringBuilder correctReply = new StringBuilder( "To play jeopardy join: " );
        correctReply.append( getJeopardyChannel() );

        when( event.getText() ).thenReturn( "!jeopardy" );

        Trivia trivia = new Trivia();
        trivia.doTrivia( event );

        verify( event, times( 1 ) ).reply( correctReply.toString() );
    }

    @Test
    public void verifyRightChannelStartsGameTest()
    {
        String correctReply = "Welcome to Bot Jeopardy!";

        when( event.getText() ).thenReturn( "!jeopardy" );
        when( event.getChannel() ).thenReturn( getJeopardyChannel() );

        Trivia trivia = new Trivia();
        trivia.doTrivia( event );

        verify( event, times( 1 ) ).reply( correctReply );
    }

    private String getJeopardyChannel()
    {
        String configLine = "";
        String jeopardyChannel = "";

        BufferedReader configFile = null;

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
