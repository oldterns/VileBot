package com.oldterns.vilebot.util;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Alternative
@ApplicationScoped
public class TestURLFactory
    implements URLFactory
{
    Map<String, URL> urlStringToUrlMap = new HashMap<>();

    @Inject
    TestUrlStreamHandler urlStreamHandler;

    @Override
    public URL build( String url )
        throws MalformedURLException
    {
        URL tempURL = new URL( url );
        URL testURL =
            new URL( tempURL.getProtocol(), tempURL.getHost(), tempURL.getPort(), tempURL.getFile(), urlStreamHandler );
        urlStringToUrlMap.put( url, testURL );
        return testURL;
    }

    public URL getURL( String url )
    {
        return urlStringToUrlMap.get( url );
    }

    void reset()
    {
        urlStringToUrlMap.clear();
    }
}
