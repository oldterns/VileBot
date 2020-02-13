package vilebot.handlers.user;

import com.oldterns.vilebot.handlers.user.DownOrJustMe;
import org.junit.Before;
import org.junit.Test;
import org.pircbotx.hooks.events.MessageEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DownOrJustMeTest
{
    private DownOrJustMe instance;

    private MessageEvent event;

    @Before
    public void setup()
    {
        instance = new DownOrJustMe();
        event = mock( MessageEvent.class );
    }

    @Test
    public void testUnrelatedMessages()
    {
        when( event.getMessage() ).thenReturn( "foobar" );

        verifyNoMoreInteractions( event );
    }

    @Test
    public void test200Url()
    {
        when( event.getMessage() ).thenReturn( "!downorjustme https://www.google.com/" );

        instance.onGenericMessage( event );

        verify( event ).respondWith( "LGTM! It's just you." );
    }

    @Test
    public void test404Url()
    {
        when( event.getMessage() ).thenReturn( "!downorjustme https://www.google.com/404" );

        instance.onGenericMessage( event );

        verify( event ).respondWith( "It responded with a non-2xx status code (404), but the host is up." );
    }

    @Test
    public void testNonResolvableHostname()
    {
        when( event.getMessage() ).thenReturn( "!downorjustme secret-host-name-thats-not-gonna-be-resolved-anyhow" );

        instance.onGenericMessage( event );

        verify( event ).respondWith( "Nope, it's not just you. The hostname is not resolvable from here, too!" );
    }

    @Test
    public void testReachableHostWithNoHttpService()
    {
        when( event.getMessage() ).thenReturn( "!downorjustme 129.146.47.116" );

        instance.onGenericMessage( event );

        verify( event ).respondWith( "Not sure about web services, but the host is definitely up." );
    }

    @Test
    public void testUnreachableHost()
    {
        when( event.getMessage() ).thenReturn( "!downorjustme 1.2.3.4" );

        instance.onGenericMessage( event );

        verify( event ).respondWith( "Nope, it's not just you. The host looks down from here, too!" );
    }

    @Test
    public void testMulticast()
    {
        when( event.getMessage() ).thenReturn( "!downorjustme 224.0.0.1" );

        instance.onGenericMessage( event );

        verify( event ).respondWith( "A multicast address? Seriously? Stop flooding the network already." );
    }

    @Test
    public void testLocalHost()
    {
        when( event.getMessage() ).thenReturn( "!downorjustme 127.0.0.1" );

        instance.onGenericMessage( event );

        verify( event ).respondWith( "You wouldn't think I was that vulnerable, right?" );
    }

    @Test
    public void testDisallowedSubnet()
    {
        when( event.getMessage() ).thenReturn( "!downorjustme 10.0.0.1" );

        instance.onGenericMessage( event );

        verify( event ).respondWith( "Ah-ha! Destinations in subnet 10.0.0.0/8 are not allowed." );
    }
}
