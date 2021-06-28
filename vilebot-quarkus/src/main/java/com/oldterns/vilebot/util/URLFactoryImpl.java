package com.oldterns.vilebot.util;

import javax.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;
import java.net.URL;

@ApplicationScoped
public class URLFactoryImpl
    implements URLFactory
{
    public URL build( String url )
        throws MalformedURLException
    {
        return new URL( url );
    }
}
