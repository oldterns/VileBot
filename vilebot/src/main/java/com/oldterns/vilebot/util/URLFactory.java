package com.oldterns.vilebot.util;

import java.net.MalformedURLException;
import java.net.URL;

public interface URLFactory
{
    URL build( String url )
        throws MalformedURLException;

    URL build( String protocol, String host, int port, String file )
        throws MalformedURLException;
}
