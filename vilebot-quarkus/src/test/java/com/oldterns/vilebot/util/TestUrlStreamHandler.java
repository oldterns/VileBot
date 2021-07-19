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
        // These fields may receive context content if this was relative URL
        String protocol = u.getProtocol();
        String authority = u.getAuthority();
        String userInfo = u.getUserInfo();
        String host = u.getHost();
        int port = u.getPort();
        String path = u.getPath();
        String query = u.getQuery();

        // This field has already been parsed
        String ref = u.getRef();

        boolean isRelPath = false;
        boolean queryOnly = false;

        // FIX: should not assume query if opaque
        // Strip off the query part
        if ( start < limit )
        {
            int queryStart = spec.indexOf( '?' );
            queryOnly = queryStart == start;
            if ( ( queryStart != -1 ) && ( queryStart < limit ) )
            {
                query = spec.substring( queryStart + 1, limit );
                if ( limit > queryStart )
                    limit = queryStart;
                spec = spec.substring( 0, queryStart );
            }
        }

        int i = 0;
        // Parse the authority part if any
        boolean isUNCName =
            ( start <= limit - 4 ) && ( spec.charAt( start ) == '/' ) && ( spec.charAt( start + 1 ) == '/' )
                && ( spec.charAt( start + 2 ) == '/' ) && ( spec.charAt( start + 3 ) == '/' );
        if ( !isUNCName && ( start <= limit - 2 ) && ( spec.charAt( start ) == '/' )
            && ( spec.charAt( start + 1 ) == '/' ) )
        {
            start += 2;
            i = spec.indexOf( '/', start );
            if ( i < 0 || i > limit )
            {
                i = spec.indexOf( '?', start );
                if ( i < 0 || i > limit )
                    i = limit;
            }

            host = authority = spec.substring( start, i );

            int ind = authority.indexOf( '@' );
            if ( ind != -1 )
            {
                if ( ind != authority.lastIndexOf( '@' ) )
                {
                    // more than one '@' in authority. This is not server based
                    userInfo = null;
                    host = null;
                }
                else
                {
                    userInfo = authority.substring( 0, ind );
                    host = authority.substring( ind + 1 );
                }
            }
            else
            {
                userInfo = null;
            }
            if ( host != null )
            {
                // If the host is surrounded by [ and ] then its an IPv6
                // literal address as specified in RFC2732
                if ( host.length() > 0 && ( host.charAt( 0 ) == '[' ) )
                {
                    if ( ( ind = host.indexOf( ']' ) ) > 2 )
                    {

                        String nhost = host;
                        host = nhost.substring( 0, ind + 1 );

                        port = -1;
                        if ( nhost.length() > ind + 1 )
                        {
                            if ( nhost.charAt( ind + 1 ) == ':' )
                            {
                                ++ind;
                                // port can be null according to RFC2396
                                if ( nhost.length() > ( ind + 1 ) )
                                {
                                    port = Integer.parseInt( nhost, ind + 1, nhost.length(), 10 );
                                }
                            }
                            else
                            {
                                throw new IllegalArgumentException( "Invalid authority field: " + authority );
                            }
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException( "Invalid authority field: " + authority );
                    }
                }
                else
                {
                    ind = host.indexOf( ':' );
                    port = -1;
                    if ( ind >= 0 )
                    {
                        // port can be null according to RFC2396
                        if ( host.length() > ( ind + 1 ) )
                        {
                            port = Integer.parseInt( host, ind + 1, host.length(), 10 );
                        }
                        host = host.substring( 0, ind );
                    }
                }
            }
            else
            {
                host = "";
            }
            if ( port < -1 )
                throw new IllegalArgumentException( "Invalid port number :" + port );
            start = i;
            // If the authority is defined then the path is defined by the
            // spec only; See RFC 2396 Section 5.2.4.
            if ( authority != null && !authority.isEmpty() )
                path = "";
        }

        if ( host == null )
        {
            host = "";
        }

        // Parse the file path if any
        if ( start < limit )
        {
            if ( spec.charAt( start ) == '/' )
            {
                path = spec.substring( start, limit );
            }
            else if ( path != null && !path.isEmpty() )
            {
                isRelPath = true;
                int ind = path.lastIndexOf( '/' );
                String separator = "";
                if ( ind == -1 && authority != null )
                    separator = "/";
                path = path.substring( 0, ind + 1 ) + separator + spec.substring( start, limit );

            }
            else
            {
                path = spec.substring( start, limit );
                path = ( authority != null ) ? "/" + path : path;
            }
        }
        else if ( queryOnly && path != null )
        {
            int ind = path.lastIndexOf( '/' );
            if ( ind < 0 )
                ind = 0;
            path = path.substring( 0, ind ) + "/";
        }
        if ( path == null )
            path = "";

        if ( isRelPath )
        {
            // Remove embedded /./
            while ( ( i = path.indexOf( "/./" ) ) >= 0 )
            {
                path = path.substring( 0, i ) + path.substring( i + 2 );
            }
            // Remove embedded /../ if possible
            i = 0;
            while ( ( i = path.indexOf( "/../", i ) ) >= 0 )
            {
                /*
                 * A "/../" will cancel the previous segment and itself, unless that segment is a "/../" itself i.e.
                 * "/a/b/../c" becomes "/a/c" but "/../../a" should stay unchanged
                 */
                if ( i > 0 && ( limit = path.lastIndexOf( '/', i - 1 ) ) >= 0
                    && ( path.indexOf( "/../", limit ) != 0 ) )
                {
                    path = path.substring( 0, limit ) + path.substring( i + 3 );
                    i = 0;
                }
                else
                {
                    i = i + 3;
                }
            }
            // Remove trailing .. if possible
            while ( path.endsWith( "/.." ) )
            {
                i = path.indexOf( "/.." );
                if ( ( limit = path.lastIndexOf( '/', i - 1 ) ) >= 0 )
                {
                    path = path.substring( 0, limit + 1 );
                }
                else
                {
                    break;
                }
            }
            // Remove starting .
            if ( path.startsWith( "./" ) && path.length() > 2 )
                path = path.substring( 2 );

            // Remove trailing .
            if ( path.endsWith( "/." ) )
                path = path.substring( 0, path.length() - 1 );
        }

        setURL( u, protocol, host, port, authority, userInfo, path, query, ref );
    }

    protected int getDefaultPort()
    {
        return -1;
    }

    protected boolean equals( URL u1, URL u2 )
    {
        return Objects.equals( u1.getRef(), u2.getRef() ) && sameFile( u1, u2 );
    }

    protected int hashCode( URL u )
    {
        int h = 0;

        // Generate the protocol part.
        String protocol = u.getProtocol();
        if ( protocol != null )
            h += protocol.hashCode();

        // Generate the host part.
        InetAddress addr = getHostAddress( u );
        if ( addr != null )
        {
            h += addr.hashCode();
        }
        else
        {
            String host = u.getHost();
            if ( host != null )
                h += host.toLowerCase().hashCode();
        }

        // Generate the file part.
        String file = u.getFile();
        if ( file != null )
            h += file.hashCode();

        // Generate the port part.
        if ( u.getPort() == -1 )
            h += getDefaultPort();
        else
            h += u.getPort();

        // Generate the ref part.
        String ref = u.getRef();
        if ( ref != null )
            h += ref.hashCode();

        return h;
    }

    protected boolean sameFile( URL u1, URL u2 )
    {
        // Compare the protocols.
        if ( !( ( u1.getProtocol() == u2.getProtocol() )
            || ( u1.getProtocol() != null && u1.getProtocol().equalsIgnoreCase( u2.getProtocol() ) ) ) )
            return false;

        // Compare the files.
        if ( !( u1.getFile() == u2.getFile() || ( u1.getFile() != null && u1.getFile().equals( u2.getFile() ) ) ) )
            return false;
        return true;
    }

    protected synchronized InetAddress getHostAddress( URL u )
    {
        return null;
    }

    protected boolean hostsEqual( URL u1, URL u2 )
    {
        InetAddress a1 = getHostAddress( u1 );
        InetAddress a2 = getHostAddress( u2 );
        // if we have internet address for both, compare them
        if ( a1 != null && a2 != null )
        {
            return a1.equals( a2 );
            // else, if both have host names, compare them
        }
        else if ( u1.getHost() != null && u2.getHost() != null )
            return u1.getHost().equalsIgnoreCase( u2.getHost() );
        else
            return u1.getHost() == null && u2.getHost() == null;
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