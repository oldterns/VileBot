package com.oldterns.vilebot.util;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InetAddressProvider
{

    public InetAddress getHostNetAddress( String hostnameOrUrl )
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

    public InetAddress getByName( String name )
        throws UnknownHostException
    {
        return InetAddress.getByName( name );
    }

}
