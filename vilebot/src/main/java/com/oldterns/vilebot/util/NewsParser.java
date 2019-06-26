/**
 * Copyright (C) 2019 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.oldterns.vilebot.util;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

public abstract class NewsParser
    extends ListenerAdapter
{
    private static final Logger logger = LoggerFactory.getLogger( NewsParser.class );

    protected static final int NUM_HEADLINES = 3;

    protected void newsLimit( GenericMessageEvent event, Matcher matcher, HashMap<String, URL> newsFeedsByCategory,
                              String defaultCategory, Logger logger, LimitCommand limitCommand,
                              String restrictedChannel )
    {
        if ( event instanceof MessageEvent
            && ( (MessageEvent) event ).getChannel().getName().equals( restrictedChannel ) )
        {
            String isLimit = limitCommand.addUse( event.getUser().getNick() );
            if ( isLimit.isEmpty() )
            {
                currentNews( event, matcher, newsFeedsByCategory, defaultCategory, logger );
            }
            else
            {
                event.respondWith( isLimit );
            }
        }
        else
        {
            currentNews( event, matcher, newsFeedsByCategory, defaultCategory, logger );
        }
    }

    protected void currentNews( GenericMessageEvent event, Matcher matcher, HashMap<String, URL> newsFeedsByCategory,
                                String defaultCategory, Logger logger )
    {
        String category = matcher.group( 1 ); // The news category

        category = ( category != null ) ? category.toLowerCase() : defaultCategory;

        if ( newsFeedsByCategory.containsKey( category ) )
        {
            printHeadlines( event, newsFeedsByCategory, category, logger );
        }
        else
        {
            event.respondWith( "No news feed available for " + category );
        }
    }

    protected void printHeadlines( GenericMessageEvent event, HashMap<String, URL> newsFeedsByCategory, String category,
                                   Logger logger )
    {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        try
        {
            feed = input.build( new XmlReader( newsFeedsByCategory.get( category ) ) );
        }
        catch ( FeedException | IOException e )
        {
            String errorMsg = "Error opening RSS feed";
            logger.error( e.getMessage() );
            logger.error( errorMsg );
            event.respondWith( errorMsg );
        }

        List<SyndEntry> entries = feed.getEntries();

        for ( int i = 0; i < NUM_HEADLINES; i++ )
        {
            event.respondWith( Colors.bold( "  " + entries.get( i ).getTitle() ) + " -> "
                + entries.get( i ).getLink() );
        }
    }

    protected abstract String generateHelpMessage();
}
