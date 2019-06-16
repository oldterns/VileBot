/**
 * Copyright (C) 2019 Newterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.oldterns.vilebot.handlers.user;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class News
    extends ListenerAdapter
{
    private static final Pattern NEWS_PATTERN = Pattern.compile( "^!news(.*)" );

    public static final String CBC_URL = "https://rss.cbc.ca/lineup/canada-toronto.xml";

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher match = NEWS_PATTERN.matcher( text );

        if ( match.matches() )
        {
            try
            {
                printHeadlines( event );
            }
            catch ( Exception e )
            {
                event.respondWith( "Unable to retrieve headlines.");
                e.printStackTrace();
            }
        }
    }

    private void printHeadlines( GenericMessageEvent event )
        throws Exception
    {
        URL feedSource = new URL( CBC_URL );

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build( new XmlReader( feedSource ) );

        List<SyndEntry> entries = feed.getEntries();

        for ( int i = 0; i < 5; i++ )
        {
            event.respondWith( entries.get( i ).getTitle() + " -> " + entries.get( i ).getLink() );
        }
    }

}
