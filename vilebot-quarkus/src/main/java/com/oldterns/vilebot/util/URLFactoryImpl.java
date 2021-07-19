package com.oldterns.vilebot.util;

import javax.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;
import java.net.URL;

@ApplicationScoped
public class URLFactoryImpl
    implements URLFactory
{
    public URL build( String url ) throws MalformedURLException
    {
        return new URL( url );
    }

    @Override
    public URL build(String protocol, String host, int port, String file) throws MalformedURLException {
        return new URL(protocol, host, port, file);
    }
}
