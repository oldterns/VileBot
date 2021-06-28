package com.oldterns.vilebot.util;

import java.net.MalformedURLException;
import java.net.URL;

public interface URLFactory
{
    public URL build( String url )
        throws MalformedURLException;
}
