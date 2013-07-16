/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import net.engio.mbassy.listener.Handler;

/**
 * Will find the HTML title of HTTP(S) pages from certain domains. Generally it's only worth adding trustworthy domains
 * that don't include titles in the URL for SEO.
 */
@HandlerContainer
public class UrlTitleAnnouncer
{

    private static final Pattern urlPattern =
        Pattern.compile( "((?:http|https)://(?:www.|)(?:(?:abstrusegoose|xkcd)\\.com|youtube\\.(?:com|ca))[^ ]*)" );

    private static final Pattern titlePattern = Pattern.compile( "<title>(.*)</title>" );

    @Handler
    public void urlAnnouncer( ReceivePrivmsg event )
    {
        Matcher urlMatcher = urlPattern.matcher( event.getText() );

        if ( urlMatcher.find() )
        {
            String title = scrapeURLHTMLTitle( urlMatcher.group( 1 ) );
            event.reply( "'" + title + "'" );
        }
    }

    /**
     * Accesses the source of a HTML page and looks for a title element
     * 
     * @param url http URI String
     * @return String of text between the first <title> tag group on the page, empty if error.
     */
    private String scrapeURLHTMLTitle( String url )
    {
        String title = "";

        URL page;
        try
        {
            page = new URL( url );
        }
        catch ( MalformedURLException x )
        {
            // System.err.format("scrapeURLHTMLTitle new URL error: %s%n", x);
            return title;
        }

        URLConnection conn;
        try
        {
            conn = page.openConnection();
        }
        catch ( IOException x )
        {
            // System.err.format("scrapeURLHTMLTitle openConnection() error: %s%n", x);
            return title;
        }

        try (BufferedReader in = new BufferedReader( new InputStreamReader( conn.getInputStream() ) ))
        {
            String inputLine;
            Matcher titlePatternMatcher;

            while ( ( inputLine = in.readLine() ) != null )
            {
                titlePatternMatcher = titlePattern.matcher( inputLine );
                if ( titlePatternMatcher.find() )
                {
                    title = titlePatternMatcher.group( 1 );
                    break;
                }
            }

            title = title.replace( "&#39;", "'" );
        }
        catch ( IOException x )
        {
            System.err.format( "scrapeURLHTMLTitle BufferedReader error: %s%n", x );
        }

        return title;
    }

}
