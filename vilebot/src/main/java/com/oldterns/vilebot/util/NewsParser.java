/**
 * Copyright (C) 2019 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.oldterns.vilebot.util;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NewsParser
    extends ListenerAdapter
{
    private static final Logger logger = LoggerFactory.getLogger( NewsParser.class );

    protected static final int NUM_HEADLINES = 3;

    protected void currentNews( GenericMessageEvent event, Matcher matcher,
                                Map<String, ImmutablePair<String, URL>> newsFeedsByCategory, String defaultCategory,
                                String helpCommand, LimitCommand limitCommand, String restrictedChannel, Logger logger )
    {
        String category = matcher.group( 1 ); // The news category

        category = ( category != null ) ? category.toLowerCase() : defaultCategory;

        if ( newsFeedsByCategory.containsKey( category ) )
        {
            newsLimit( event, newsFeedsByCategory, category, logger, limitCommand, restrictedChannel );
        }
        else
        {
            event.respondWith( "No news feed available for " + category + ". Try " + helpCommand
                + " for available news categories." );
        }
    }

    protected void newsLimit( GenericMessageEvent event, Map<String, ImmutablePair<String, URL>> newsFeedsByCategory,
                              String category, Logger logger, LimitCommand limitCommand, String restrictedChannel )
    {
        if ( event instanceof MessageEvent
            && ( (MessageEvent) event ).getChannel().getName().equals( restrictedChannel ) )
        {
            String isLimit = limitCommand.addUse( event.getUser().getNick() );
            if ( isLimit.isEmpty() )
            {
                printHeadlines( event, newsFeedsByCategory, category, logger );
            }
            else
            {
                event.respondWith( isLimit );
            }
        }
        else
        {
            printHeadlines( event, newsFeedsByCategory, category, logger );
        }
    }

    protected void printHeadlines( GenericMessageEvent event,
                                   Map<String, ImmutablePair<String, URL>> newsFeedsByCategory, String category,
                                   Logger logger )
    {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;

        try
        {
            feed = input.build( new XmlReader( newsFeedsByCategory.get( category ).getRight() ) );
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
