package vilebot.handlers.user;

import com.oldterns.vilebot.handlers.user.TwitterCorrection;
import org.junit.Before;
import org.junit.Test;
import org.pircbotx.hooks.events.MessageEvent;

import static org.mockito.Mockito.*;

public class TwitterCorrectionTest
{

    private String correctResponse =
        "You seem to be using twitter addressing syntax. On IRC you would say this instead: ipun: message";

    private TwitterCorrection tester = new TwitterCorrection();

    private MessageEvent event;

    @Before
    public void setup()
    {
        event = mock( MessageEvent.class );
    }

    @Test
    public void validTwitterCorrectionTest1()
    {
        when( event.getMessage() ).thenReturn( "@ipun hello world" );
        tester.onGenericMessage( event );
        verify( event, times( 1 ) ).respond( correctResponse );
    }

    @Test
    public void validTwitterCorrectionTest2()
    {
        when( event.getMessage() ).thenReturn( " Hello world @ipun hello world" );
        tester.onGenericMessage( event );
        verify( event, times( 1 ) ).respond( correctResponse );
    }

    @Test
    public void invalidTwitterCorrectionTest()
    {
        when( event.getMessage() ).thenReturn( "Hello world" );
        tester.onGenericMessage( event );
        verify( event, never() ).respond( anyString() );
    }

    @Test
    public void additionalPostfixTwitterCorrectionTest1()
    {
        when( event.getMessage() ).thenReturn( "@ipun: hello world" );
        tester.onGenericMessage( event );
        verify( event, times( 1 ) ).respond( correctResponse );
    }

    @Test
    public void additionalPostfixTwitterCorrectionTest2()
    {
        when( event.getMessage() ).thenReturn( "@ipun, hello world" );
        tester.onGenericMessage( event );
        verify( event, times( 1 ) ).respond( correctResponse );
    }

}
