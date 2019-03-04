package vilebot.handlers.user;

import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.handlers.user.TwitterCorrection;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.pircbotx.hooks.events.MessageEvent;

import static org.mockito.Mockito.*;

public class TwitterCorrectionTest
{

    String correctResponse =
        "You seem to be using twitter addressing syntax. On IRC you would say this instead: ipun: message";

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass( String.class );

    TwitterCorrection tester = new TwitterCorrection();

    MessageEvent event = mock( MessageEvent.class );

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
