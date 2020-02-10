package vilebot.handlers.user;

import com.oldterns.vilebot.handlers.user.Countdown;
import org.junit.Before;
import org.junit.Test;
import org.pircbotx.Channel;
import org.pircbotx.hooks.events.MessageEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CountdownTest
{
    private MessageEvent event;

    private Channel channel;

    private static final String VILEBOT_CONFIG_FILE = "cfg/vilebot.conf";

    private static final String VILEBOT_CONFIG_FILE_SECONDARY = "cfg/vilebot.conf.example";

    private static final String RED = "\u000304";

    private static final String RESET = "\u000f";

    @Before
    public void setup()
    {
        event = mock( MessageEvent.class );
        channel = mock( Channel.class );
        when( event.getChannel() ).thenReturn( channel );
        when( channel.getName() ).thenReturn( getCountdownChannel() );
    }

    @Test
    public void invalidChannelTest()
    {
        when( event.getMessage() ).thenReturn( "!countdown" );
        when( channel.getName() ).thenReturn( "INVALID CHANNEL" );

        Countdown countdown = new Countdown();
        countdown.onGenericMessage( event );

        verify( event, times( 1 ) ).respondWith( "To play Countdown join: " + getCountdownChannel() );
    }

    @Test
    public void submissionWithNoActiveGameTest()
    {
        when( event.getMessage() ).thenReturn( "!solution 0" );
        when( event.getChannel() ).thenReturn( channel );

        Countdown countdown = new Countdown();
        countdown.onGenericMessage( event );

        verify( event, times( 1 ) ).respondWith( "No active game. Start a new one with !countdown." );
    }

    @Test
    public void verifyCountdownRulesTests()
    {
        when( event.getMessage() ).thenReturn( "!countdownrules" );
        when( event.getChannel() ).thenReturn( channel );

        Countdown countdown = new Countdown();
        countdown.onGenericMessage( event );

        verify( event, times( 1 ) ).respondPrivateMessage( " " + RED + "COUNTDOWN RULES:" + RESET );
        verify( event,
                times( 1 ) ).respondPrivateMessage( "1) Get as close as you can to the target number using only the numbers given." );
        verify( event, times( 1 ) ).respondPrivateMessage( RED + "TIP: You do not have to use all the numbers."
            + RESET );
        verify( event,
                times( 1 ) ).respondPrivateMessage( "2) Answer with !solution <your answer> . Make sure to only use valid characters, such as numbers and + - * / ( ) . " );
        verify( event, times( 1 ) ).respondPrivateMessage( "Breaking Rule 2 will subject you to a loss of 10 karma." );
        verify( event,
                times( 1 ) ).respondPrivateMessage( "3) The closer you are to the target number, the more karma you will get (max. 10)." );
        verify( event, times( 1 ) ).respondPrivateMessage( RED
            + "TIP: If you are over/under 200 you will be penalized 10 karma." + RESET );
        verify( event,
                times( 1 ) ).respondPrivateMessage( "4) Use \" /msg CountdownB0t !solution <your answer> \" for your answers." );
    }

    private String getCountdownChannel()
    {
        String configLine = "";
        String countdownChannel = "";

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
                if ( configLine.length() > 17 )
                {
                    countdownChannel = configLine.substring( 0, 16 );
                }
                if ( "countdownChannel".equals( countdownChannel ) )
                {
                    break;
                }
            }
        }
        catch ( Exception e )
        {
            System.out.println( "Error reading config file: " + e.getMessage() );
        }

        return configLine.substring( 17 );
    }
}
