package com.oldterns.vilebot.util;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link URLStreamHandler} that allows us to control the {@link URLConnection URLConnections} that are returned by
 * {@link URL URLs} in the code under test. Based on https://claritysoftware.co.uk/mocking-javas-url-with-mockito/
 */
@ApplicationScoped
public class TestUrlStreamHandler
    extends URLStreamHandler
{
    private Map<String, URLConnection> urlToConnectionMap = new HashMap<>();

    @Inject
    TestURLFactory testURLFactory;

    @Override
    protected URLConnection openConnection( URL url )
    {
        if ( urlToConnectionMap.containsKey( url.toString() ) )
        {
            return urlToConnectionMap.get( url.toString() );
        }
        else
        {
            throw new IllegalStateException( "An attempted access was made on url (" + url
                + ") but no mocked connection was supplied. Maybe add a connection using mockConnection(url, data)?" );
        }
    }

    /**
     * Clears the connection map; should be done in @BeforeEach or @AfterEach (unless the same connections are reused)
     */
    public void reset()
    {
        urlToConnectionMap.clear();
        testURLFactory.reset();
    }

    /**
     * Return urlConnection when url connection is open. The urlConnection should be mocked. Ex: {@code
     * URLConnection urlConnection = mock(URLConnection.class);
     * InputStream connectionInputStream = new ByteArrayInputStream("Hello".getBytes());
     * when(urlConnection.getInputStream()).thenReturn(connectionInputStream);
     * }
     *
     * @param url The URL to set the connection of
     * @param urlConnection The mocked url connection
     * @return this for chaining
     */
    public TestUrlStreamHandler mockConnection( String url, URLConnection urlConnection )
    {
        urlToConnectionMap.put( url, urlConnection );
        return this;
    }

    /**
     * Return a mocked URLConnection whose input stream contains text when url connection is open.
     *
     * @param url The URL to set the connection of
     * @param text The text the url connection input stream should match
     * @return this for chaining
     */
    public TestUrlStreamHandler mockConnection( String url, String text )
    {
        URLConnection urlConnection = mock( URLConnection.class );
        InputStream connectionInputStream = new ByteArrayInputStream( text.getBytes() );
        try
        {
            when( urlConnection.getInputStream() ).thenReturn( connectionInputStream );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Impossible state: exception thrown when mocking" );
        }
        return mockConnection( url, urlConnection );
    }

    /**
     * Return a mocked URLConnection with the supplied input stream when url connection is open.
     *
     * @param url The URL to set the connection of
     * @param resource The input stream the url connection should use
     * @return this for chaining
     */
    public TestUrlStreamHandler mockConnection( String url, InputStream resource )
    {
        URLConnection urlConnection = mock( URLConnection.class );
        try
        {
            when( urlConnection.getInputStream() ).thenReturn( resource );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Impossible state: exception thrown when mocking" );
        }
        return mockConnection( url, urlConnection );
    }

    public TestUrlStreamHandler mockHttpConnection( String url, InputStream resource )
    {
        return mockHttpConnection( "GET", url, resource, 200 );
    }

    public TestUrlStreamHandler mockHttpConnection( String method, String url, InputStream resource, int status )
    {
        HttpURLConnection urlConnection = mock( HttpURLConnection.class );
        try
        {
            when( urlConnection.getInputStream() ).thenReturn( resource );
            when( urlConnection.getResponseCode() ).thenReturn( status );
            when( urlConnection.getRequestMethod() ).thenReturn( method );
            when( urlConnection.getURL() ).thenAnswer( invocationOnMock -> testURLFactory.getURL( url ) );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Impossible state: exception thrown when mocking" );
        }
        return mockConnection( url, urlConnection );
    }

    protected void parseURL( URL u, String spec, int start, int limit )
    {
    }

    protected int getDefaultPort()
    {
        return -1;
    }

    protected boolean equals( URL u1, URL u2 )
    {
        return Objects.equals( u1.getRef(), u2.getRef() );
    }

    protected int hashCode( URL u )
    {
        return Objects.hashCode( u.getRef().hashCode() );
    }

    protected boolean sameFile( URL u1, URL u2 )
    {
        return true;
    }

    protected synchronized InetAddress getHostAddress( URL u )
    {
        return null;
    }

    protected boolean hostsEqual( URL u1, URL u2 )
    {
        return true;
    }

    protected String toExternalForm( URL u )
    {
        String s;
        return u.getProtocol() + ':' + ( ( s = u.getAuthority() ) != null && !s.isEmpty() ? "//" + s : "" )
            + ( ( s = u.getPath() ) != null ? s : "" ) + ( ( s = u.getQuery() ) != null ? '?' + s : "" )
            + ( ( s = u.getRef() ) != null ? '#' + s : "" );
    }

    protected void setURL( URL u, String protocol, String host, int port, String authority, String userInfo,
                           String path, String query, String ref )
    {
    }

    @Deprecated
    protected void setURL( URL u, String protocol, String host, int port, String file, String ref )
    {
    }

}