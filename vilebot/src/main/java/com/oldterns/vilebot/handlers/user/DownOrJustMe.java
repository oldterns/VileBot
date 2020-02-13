package com.oldterns.vilebot.handlers.user;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownOrJustMe
    extends ListenerAdapter
{
    private static final Pattern DOWN_OR_JUST_ME_PATTERN = Pattern.compile( "^!(downorjustme)\\s(.+)$" );

    private static final int PING_TIMEOUT = 3000;

    private static final int HTTP_TIMEOUT = 5000;

    private static final String[] DISALLOWED_SUBNETS = new String[] { "10.0.0.0/8" };

    // Some sites check for this and returns non-2xx status. Ridiculous!
    private static final String HTTP_UA =
        "Mozilla/5.0 (X11; Fedora; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = DOWN_OR_JUST_ME_PATTERN.matcher( text );

        if ( matcher.matches() )
        {
            check( event, matcher );
        }
    }

    protected static void check( GenericMessageEvent event, Matcher matcher )
    {
        String hostnameOrUrl = matcher.group( 2 );
        InetAddress destination;
        try
        {
            destination = getHostNetAddress( hostnameOrUrl );
        }
        catch ( UnknownHostException e )
        {
            event.respondWith( "Nope, it's not just you. The hostname is not resolvable from here, too!" );
            return;
        }

        try
        {
            validateDestination( destination );
        }
        catch ( IllegalArgumentException e )
        {
            event.respondWith( e.getMessage() );
            return;
        }

        URL url;
        try
        {
            url = new URL( hostnameOrUrl );
        }
        catch ( MalformedURLException e )
        {
            try
            {
                url = new URL( "http", hostnameOrUrl, 80, "/" );
            }
            catch ( MalformedURLException ex )
            {
                throw new RuntimeException( ex ); // This should not happen
            }
        }

        int status = httpGet( url );
        if ( status >= 400 )
        {
            event.respondWith( "It responded with a non-2xx status code (" + status + "), but the host is up." );
            return;
        }
        else if ( status != -1 )
        {
            event.respondWith( "LGTM! It's just you." );
            return;
        }

        if ( icmpPing( destination ) )
        {
            event.respondWith( "Not sure about web services, but the host is definitely up." );
        }
        else
        {
            event.respondWith( "Nope, it's not just you. The host looks down from here, too!" );
        }
    }

    private static InetAddress getHostNetAddress( String hostnameOrUrl )
        throws UnknownHostException
    {
        try
        {
            return InetAddress.getByName( new URL( hostnameOrUrl ).getHost() );
        }
        catch ( MalformedURLException e )
        {
            // noop
        }

        return InetAddress.getByName( hostnameOrUrl );
    }

    private static void validateDestination( InetAddress destination )
    {
        if ( destination.isMulticastAddress() )
        {
            throw new IllegalArgumentException( "A multicast address? Seriously? Stop flooding the network already." );
        }

        if ( destination.isLoopbackAddress() )
        {
            throw new IllegalArgumentException( "You wouldn't think I was that vulnerable, right?" );
        }

        for ( String subnet : DISALLOWED_SUBNETS )
        {
            InetAddress id;
            try
            {
                id = InetAddress.getByName( subnet.split( "/" )[0] );
            }
            catch ( UnknownHostException e )
            {
                throw new RuntimeException( "Check DISALLOWED_SUBNETS constant validity!" );
            }
            int maskLength = Integer.parseInt( subnet.split( "/" )[1] );

            byte[] idBytes = id.getAddress();
            byte[] destinationBytes = destination.getAddress();
            if ( idBytes.length != destinationBytes.length )
            {
                continue; // Don't match between IPv4 and IPv6
            }

            if ( new BigInteger( idBytes ).shiftRight( idBytes.length
                * 8 - maskLength ).equals(
                                           new BigInteger( destinationBytes ).shiftRight( destinationBytes.length * 8
                                               - maskLength ) ) )
            {
                throw new IllegalArgumentException( "Ah-ha! Destinations in subnet " + subnet + " are not allowed." );
            }
        }
    }

    private static boolean icmpPing( InetAddress destination )
    {
        try
        {
            return destination.isReachable( PING_TIMEOUT );
        }
        catch ( IOException e )
        {
            return false;
        }
    }

    private static int httpGet( URL url )
    {
        try
        {
            HttpURLConnection httpClient = (HttpURLConnection) url.openConnection();
            httpClient.setConnectTimeout( HTTP_TIMEOUT );
            httpClient.setReadTimeout( HTTP_TIMEOUT );
            httpClient.setRequestMethod( "GET" );
            httpClient.setRequestProperty( "User-Agent", HTTP_UA );
            return httpClient.getResponseCode();
        }
        catch ( IOException e )
        {
            return -1;
        }
    }
}
