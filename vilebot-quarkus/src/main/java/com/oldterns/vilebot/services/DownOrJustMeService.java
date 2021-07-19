package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.util.URLFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

@ApplicationScoped
public class DownOrJustMeService
{
    @Inject
    URLFactory urlFactory;

    private static final int PING_TIMEOUT = 3000;

    private static final int HTTP_TIMEOUT = 5000;

    private static final String[] DISALLOWED_SUBNETS = new String[] { "10.0.0.0/8" };

    // Some sites check for this and returns non-2xx status. Ridiculous!
    private static final String HTTP_UA =
        "Mozilla/5.0 (X11; Fedora; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";

    @OnMessage( "!downorjustme @hostnameOrUrl" )
    public String check( String hostnameOrUrl )
    {
        InetAddress destination;
        try
        {
            destination = getHostNetAddress( hostnameOrUrl );
        }
        catch ( UnknownHostException e )
        {
            return "Nope, it's not just you. The hostname is not resolvable from here, too!";
        }

        try
        {
            validateDestination( destination );
        }
        catch ( IllegalArgumentException e )
        {
            return e.getMessage();
        }

        URL url;
        try
        {
            url = urlFactory.build( hostnameOrUrl );
        }
        catch ( MalformedURLException e )
        {
            try
            {
                url = urlFactory.build( "http", hostnameOrUrl, 80, "/" );
            }
            catch ( MalformedURLException ex )
            {
                throw new RuntimeException( ex ); // This should not happen
            }
        }

        int status = httpGet( url );
        if ( status >= 400 )
        {
            return "It responded with a non-2xx status code (" + status + "), but the host is up.";
        }
        else if ( status != -1 )
        {
            return "LGTM! It's just you.";
        }

        if ( icmpPing( destination ) )
        {
            return "Not sure about web services, but the host is definitely up.";
        }
        else
        {
            return "Nope, it's not just you. The host looks down from here, too!";
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
