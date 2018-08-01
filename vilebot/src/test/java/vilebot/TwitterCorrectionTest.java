package vilebot;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.oldterns.vilebot.handlers.user.TwitterCorrection;

import static org.mockito.Mockito.*;

import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

public class TwitterCorrectionTest
{

    String correctResponse =
        "You seem to be using twitter addressing syntax. On IRC you would say this instead: ipun: message";

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass( String.class );

    TwitterCorrection tester = new TwitterCorrection();

    ReceivePrivmsg event = mock( ReceivePrivmsg.class );

    @Test
    public void validTwitterCorrectionTest1()
    {
        when( event.getText() ).thenReturn( "@ipun hello world" );
        tester.twitterBeGone( event );
        verify( event, times( 1 ) ).replyDirectly( correctResponse );
    }

    @Test
    public void validTwitterCorrectionTest2()
    {
        when( event.getText() ).thenReturn( " Hello world @ipun hello world" );
        tester.twitterBeGone( event );
        verify( event, times( 1 ) ).replyDirectly( correctResponse );
    }

    @Test
    public void invalidTwitterCorrectionTest()
    {
        when( event.getText() ).thenReturn( "Hello world" );
        tester.twitterBeGone( event );
        verify( event, never() ).replyDirectly( anyString() );
    }

    @Test
    public void additionalPostfixTwitterCorrectionTest1()
    {
        when( event.getText() ).thenReturn( "@ipun: hello world" );
        tester.twitterBeGone( event );
        verify( event, times( 1 ) ).replyDirectly( correctResponse );
    }

    @Test
    public void additionalPostfixTwitterCorrectionTest2()
    {
        when( event.getText() ).thenReturn( "@ipun, hello world" );
        tester.twitterBeGone( event );
        verify( event, times( 1 ) ).replyDirectly( correctResponse );
    }

}
