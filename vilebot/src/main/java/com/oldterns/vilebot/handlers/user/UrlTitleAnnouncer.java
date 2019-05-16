/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Will find the HTML title of HTTP(S) pages from certain domains. Generally it's only worth adding trustworthy domains
 * that don't include titles in the URL for SEO.
 */

public class UrlTitleAnnouncer
    extends ListenerAdapter
{

    private static final Pattern urlPattern =
        Pattern.compile( "((?:http|https)://(?:www.|)(?:(?:abstrusegoose|xkcd)\\.com|youtube\\.(?:com|ca)|youtu\\.be)[^ ]*)" );

    private static final Pattern titlePattern = Pattern.compile( "<title>(.*)</title>" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        Matcher urlMatcher = urlPattern.matcher( event.getMessage() );

        if ( urlMatcher.find() )
        {
            String title = scrapeURLHTMLTitle( urlMatcher.group( 1 ) );
            event.respondWith( "'" + title + "'" );
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

        try
        {
            Document doc = Jsoup.connect( url ).get();
            title = doc.title();
        }
        catch ( IOException x )
        {
            System.err.format( "scrapeURLHTMLTitle BufferedReader error: %s%n", x );
        }

        return title;
    }

}
