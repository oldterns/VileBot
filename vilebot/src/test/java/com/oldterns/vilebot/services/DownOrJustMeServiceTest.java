package com.oldterns.vilebot.services;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.inject.Inject;

import com.oldterns.vilebot.util.InetAddressProvider;
import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class DownOrJustMeServiceTest
{

    @Inject
    DownOrJustMeService downOrJustMeService;

    @Inject
    TestUrlStreamHandler urlMocker;

    @InjectMock
    InetAddressProvider inetAddressProvider;

    @BeforeEach
    public void setupForbiddenInetAddress()
        throws UnknownHostException
    {
        InetAddress inetAddress = getInetAddress( false, false, new byte[] { 10, 0, 0, 0 } );
        when( inetAddressProvider.getByName( "10.0.0.0" ) ).thenReturn( inetAddress );
    }

    private InetAddress getInetAddress()
    {
        return getInetAddress( false, false );
    }

    private InetAddress getInetAddress( boolean isMulticast, boolean isLoopback )
    {
        return getInetAddress( isMulticast, isLoopback, new byte[] { 127, 0, 0, 1 } );
    }

    private InetAddress getInetAddress( boolean isMulticast, boolean isLoopback, byte[] address )
    {
        InetAddress inetAddress = mock( InetAddress.class );
        when( inetAddress.isMulticastAddress() ).thenReturn( isMulticast );
        when( inetAddress.isLoopbackAddress() ).thenReturn( isLoopback );
        when( inetAddress.getAddress() ).thenReturn( address );
        return inetAddress;
    }

    @Test
    public void test200Url()
        throws UnknownHostException
    {
        InetAddress inetAddress = getInetAddress();
        when( inetAddressProvider.getHostNetAddress( "https://www.google.com/" ) ).thenReturn( inetAddress );
        urlMocker.mockHttpConnection( "GET", "https://www.google.com/",
                                      DownOrJustMeServiceTest.class.getResourceAsStream( "example-webpage.html" ),
                                      200 );
        assertThat( downOrJustMeService.check( "https://www.google.com/" ) ).isEqualTo( "LGTM! It's just you." );
    }

    @Test
    public void test404Url()
        throws UnknownHostException
    {
        InetAddress inetAddress = getInetAddress();
        when( inetAddressProvider.getHostNetAddress( "https://www.google.com/404" ) ).thenReturn( inetAddress );
        urlMocker.mockHttpConnection( "GET", "https://www.google.com/404",
                                      DownOrJustMeServiceTest.class.getResourceAsStream( "example-webpage.html" ),
                                      404 );
        assertThat( downOrJustMeService.check( "https://www.google.com/404" ) ).isEqualTo( "It responded with a non-2xx status code (404), but the host is up." );
    }

    @Test
    public void testNonResolvableHostname()
        throws UnknownHostException
    {
        when( inetAddressProvider.getHostNetAddress( "secret-host-name-thats-not-gonna-be-resolved-anyhow" ) ).thenThrow( new UnknownHostException() );
        assertThat( downOrJustMeService.check( "secret-host-name-thats-not-gonna-be-resolved-anyhow" ) ).isEqualTo( "Nope, it's not just you. The hostname is not resolvable from here, too!" );
    }

    @Test
    public void testReachableHostWithNoHttpService()
        throws IOException
    {
        InetAddress inetAddress = getInetAddress();
        when( inetAddressProvider.getHostNetAddress( "129.146.47.116" ) ).thenReturn( inetAddress );
        urlMocker.mockHttpConnection( "GET", "http://129.146.47.116:80/",
                                      DownOrJustMeServiceTest.class.getResourceAsStream( "example-webpage.html" ),
                                      500 );
        HttpURLConnection connection = urlMocker.getConnection( "http://129.146.47.116:80/" );
        when( connection.getResponseCode() ).thenThrow( new IOException() );
        when( inetAddress.isReachable( anyInt() ) ).thenReturn( true );
        assertThat( downOrJustMeService.check( "129.146.47.116" ) ).isEqualTo( "Not sure about web services, but the host is definitely up." );
    }

    @Test
    public void testUnreachableHost()
        throws IOException
    {
        InetAddress inetAddress = getInetAddress();
        when( inetAddressProvider.getHostNetAddress( "1.2.3.4" ) ).thenReturn( inetAddress );
        urlMocker.mockHttpConnection( "GET", "http://1.2.3.4:80/",
                                      DownOrJustMeServiceTest.class.getResourceAsStream( "example-webpage.html" ),
                                      500 );
        HttpURLConnection connection = urlMocker.getConnection( "http://1.2.3.4:80/" );
        when( connection.getResponseCode() ).thenThrow( new IOException() );
        when( inetAddress.isReachable( anyInt() ) ).thenReturn( false );
        assertThat( downOrJustMeService.check( "1.2.3.4" ) ).isEqualTo( "Nope, it's not just you. The host looks down from here, too!" );
    }

    @Test
    public void testMulticast()
        throws UnknownHostException
    {
        InetAddress inetAddress = getInetAddress( true, false );
        when( inetAddressProvider.getHostNetAddress( "224.0.0.1" ) ).thenReturn( inetAddress );
        assertThat( downOrJustMeService.check( "224.0.0.1" ) ).isEqualTo( "A multicast address? Seriously? Stop flooding the network already." );
    }

    @Test
    public void testLocalHost()
        throws UnknownHostException
    {
        InetAddress inetAddress = getInetAddress( false, true );
        when( inetAddressProvider.getHostNetAddress( "127.0.0.1" ) ).thenReturn( inetAddress );
        assertThat( downOrJustMeService.check( "127.0.0.1" ) ).isEqualTo( "You wouldn't think I was that vulnerable, right?" );
    }

    @Test
    public void testDisallowedSubnet()
        throws UnknownHostException
    {
        InetAddress inetAddress = getInetAddress( false, false, new byte[] { 10, 0, 0, 1 } );
        ;
        when( inetAddressProvider.getHostNetAddress( "10.0.0.1" ) ).thenReturn( inetAddress );
        assertThat( downOrJustMeService.check( "10.0.0.1" ) ).isEqualTo( "Ah-ha! Destinations in subnet 10.0.0.0/8 are not allowed." );
    }
}
