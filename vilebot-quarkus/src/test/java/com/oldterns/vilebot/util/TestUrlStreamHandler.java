package com.oldterns.vilebot.util;

import edu.emory.mathcs.util.classloader.jar.JarProxy;
import edu.emory.mathcs.util.classloader.jar.JarURLConnection;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link URLStreamHandler} that allows us to control the {@link URLConnection URLConnections} that are returned by
 * {@link URL URLs} in the code under test. Based on https://claritysoftware.co.uk/mocking-javas-url-with-mockito/
 */
public class TestUrlStreamHandler
    extends URLStreamHandler
{

    private Map<URL, URLConnection> urlToConnectionMap = new HashMap<>();

    private static TestUrlStreamHandler handlerInstance;

    /**
     * Should be called in the @BeforeAll of a test class, replaces the URLStreamHandlerFactory with a mock that returns
     * an instance of TestUrl
     *
     * @return An instance of TestUrlStreamHandler to be used in tests
     */
    public static TestUrlStreamHandler setup()
    {
        if ( handlerInstance != null )
        {
            return handlerInstance;
        }
        try
        {
            TestUrlStreamHandler out = new TestUrlStreamHandler();
            // URL provide no way to get the default factory :(
            // (And reflection on internal classes will be disabled by default)
            URLStreamHandler jarHandler = getJarURLStreamHandler();
            URLStreamHandlerFactory urlStreamHandlerFactory = mock( URLStreamHandlerFactory.class );
            URL.setURLStreamHandlerFactory( urlStreamHandlerFactory );

            when( urlStreamHandlerFactory.createURLStreamHandler( any() ) ).thenAnswer( new Answer<URLStreamHandler>()
            {

                @Override
                public URLStreamHandler answer( InvocationOnMock invocationOnMock )
                    throws Throwable
                {
                    // Quarkus need to open Jars during tests, so we can't use a mock
                    // for jars
                    if ( invocationOnMock.getArgument( 0 ).toString().startsWith( "jar" ) )
                    {
                        return jarHandler;
                    }
                    else
                    {
                        return handlerInstance;
                    }
                }
            } );
            handlerInstance = out;
            return out;
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "Error when mocking URL Factory: " + e );
        }
    }

    @Override
    protected URLConnection openConnection( URL url )
        throws IOException
    {
        if ( urlToConnectionMap.containsKey( url ) )
        {
            return urlToConnectionMap.get( url );
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
    public TestUrlStreamHandler mockConnection( URL url, URLConnection urlConnection )
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
    public TestUrlStreamHandler mockConnection( URL url, String text )
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
    public TestUrlStreamHandler mockConnection( URL url, InputStream resource )
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

    public TestUrlStreamHandler mockHttpConnection( URL url, InputStream resource )
    {
        return mockHttpConnection( "GET", url, resource, 200 );
    }

    public TestUrlStreamHandler mockHttpConnection( String method, URL url, InputStream resource, int status )
    {
        HttpURLConnection urlConnection = mock( HttpURLConnection.class );
        try
        {
            when( urlConnection.getInputStream() ).thenReturn( resource );
            when( urlConnection.getResponseCode() ).thenReturn( status );
            when( urlConnection.getRequestMethod() ).thenReturn( method );
            when( urlConnection.getURL() ).thenReturn( url );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Impossible state: exception thrown when mocking" );
        }
        return mockConnection( url, urlConnection );
    }

    private static URLStreamHandler getJarURLStreamHandler()
    {
        return new URLStreamHandler()
        {
            @Override
            protected URLConnection openConnection( URL url )
                throws IOException
            {
                return new JarURLConnection( url, new JarProxy() );
            }
        };
    }
}