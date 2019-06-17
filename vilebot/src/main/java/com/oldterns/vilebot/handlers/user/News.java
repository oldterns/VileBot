/**
 * Copyright (C) 2019 Newterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.oldterns.vilebot.handlers.user;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.apache.log4j.Logger;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class News
    extends ListenerAdapter
{
    public static final int NUM_HEADLINES = 5;

    private static Logger logger = Logger.getLogger( News.class );

    private static final String defaultCategory = "toronto";

    private static final HashMap<String, URL> newsFeedsByCategory = new LinkedHashMap<>();
    static
    {
        try
        {
            newsFeedsByCategory.put( "top", new URL( "https://rss.cbc.ca/lineup/topstories.xml" ) );
            newsFeedsByCategory.put( "world", new URL( "https://rss.cbc.ca/lineup/world.xml" ) );
            newsFeedsByCategory.put( "canada", new URL( "https://rss.cbc.ca/lineup/canada.xml" ) );
            newsFeedsByCategory.put( "politics", new URL( "https://rss.cbc.ca/lineup/politics.xml" ) );
            newsFeedsByCategory.put( "business", new URL( "https://rss.cbc.ca/lineup/business.xml" ) );
            newsFeedsByCategory.put( "health", new URL( "https://rss.cbc.ca/lineup/health.xml" ) );
            newsFeedsByCategory.put( "arts", new URL( "https://rss.cbc.ca/lineup/arts.xml" ) );
            newsFeedsByCategory.put( "tech", new URL( "https://rss.cbc.ca/lineup/technology.xml" ) );
            newsFeedsByCategory.put( "indigenous", new URL( "https://www.cbc.ca/cmlink/rss-cbcaboriginal" ) );
            newsFeedsByCategory.put( "sports", new URL( "https://rss.cbc.ca/lineup/sports.xml" ) );
            newsFeedsByCategory.put( "mlb", new URL( "https://rss.cbc.ca/lineup/sports-mlb.xml" ) );
            newsFeedsByCategory.put( "nba", new URL( "https://rss.cbc.ca/lineup/sports-nba.xml" ) );
            newsFeedsByCategory.put( "cfl", new URL( "https://rss.cbc.ca/lineup/sports-cfl.xml" ) );
            newsFeedsByCategory.put( "nfl", new URL( "https://rss.cbc.ca/lineup/sports-nfl.xml" ) );
            newsFeedsByCategory.put( "nhl", new URL( "https://rss.cbc.ca/lineup/sports-nhl.xml" ) );
            newsFeedsByCategory.put( "soccer", new URL( "https://rss.cbc.ca/lineup/sports-soccer.xml" ) );
            newsFeedsByCategory.put( "bc", new URL( "https://rss.cbc.ca/lineup/canada-britishcolumbia.xml" ) );
            newsFeedsByCategory.put( "kamloops", new URL( "https://rss.cbc.ca/lineup/canada-kamloops.xml" ) );
            newsFeedsByCategory.put( "calgary", new URL( "https://rss.cbc.ca/lineup/canada-calgary.xml" ) );
            newsFeedsByCategory.put( "edmonton", new URL( "https://rss.cbc.ca/lineup/canada-edmonton.xml" ) );
            newsFeedsByCategory.put( "saskatchewan", new URL( "https://rss.cbc.ca/lineup/canada-saskatchewan.xml" ) );
            newsFeedsByCategory.put( "saskatoon", new URL( "https://rss.cbc.ca/lineup/canada-saskatoon.xml" ) );
            newsFeedsByCategory.put( "manitoba", new URL( "https://rss.cbc.ca/lineup/canada-manitoba.xml" ) );
            newsFeedsByCategory.put( "thunderbay", new URL( "https://rss.cbc.ca/lineup/canada-thunderbay.xml" ) );
            newsFeedsByCategory.put( "sudbury", new URL( "https://rss.cbc.ca/lineup/canada-sudbury.xml" ) );
            newsFeedsByCategory.put( "windsor", new URL( "https://rss.cbc.ca/lineup/canada-windsor.xml" ) );
            newsFeedsByCategory.put( "london", new URL( "https://www.cbc.ca/cmlink/rss-canada-london" ) );
            newsFeedsByCategory.put( "waterloo", new URL( "https://rss.cbc.ca/lineup/canada-kitchenerwaterloo.xml" ) );
            newsFeedsByCategory.put( "toronto", new URL( "https://rss.cbc.ca/lineup/canada-toronto.xml" ) );
            newsFeedsByCategory.put( "hamilton", new URL( "https://rss.cbc.ca/lineup/canada-hamiltonnews.xml" ) );
            newsFeedsByCategory.put( "montreal", new URL( "http://rss.cbc.ca/lineup/canada-montreal.xml" ) );
            newsFeedsByCategory.put( "nb", new URL( "https://rss.cbc.ca/lineup/canada-newbrunswick.xml" ) );
            newsFeedsByCategory.put( "pei", new URL( "https://rss.cbc.ca/lineup/canada-pei.xml" ) );
            newsFeedsByCategory.put( "ns", new URL( "https://rss.cbc.ca/lineup/canada-novascotia.xml" ) );
            newsFeedsByCategory.put( "newfoundland", new URL( "https://rss.cbc.ca/lineup/canada-newfoundland.xml" ) );
            newsFeedsByCategory.put( "north", new URL( "https://rss.cbc.ca/lineup/canada-north.xml" ) );
        }
        catch ( MalformedURLException e )
        {
            logger.error( "Error loading news URLs" );
            throw new RuntimeException( e );
        }
    }

    private static final Pattern NEWS_PATTERN = Pattern.compile( "^!news(?: ([a-zA-Z]{2,})|)" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = NEWS_PATTERN.matcher( text );

        if ( matcher.matches() )
        {
            currentNews( event, matcher );
        }
    }

    private void currentNews( GenericMessageEvent event, Matcher matcher )
    {
        String category = matcher.group( 1 ); // The news category
        if ( category == null )
        {
            category = defaultCategory;
        }
        category = category.toLowerCase();

        if ( newsFeedsByCategory.containsKey( category ) )
        {
            printHeadlines( event, category );
        }
        else
        {
            event.respondWith( "No news feed available for " + category );
        }
    }

    private void printHeadlines( GenericMessageEvent event, String category )
    {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        try
        {
            feed = input.build( new XmlReader( newsFeedsByCategory.get( category ) ) );
        }
        catch ( FeedException | IOException e )
        {
            logger.error( e.getMessage() );
            logger.error( "Error opening RSS feed" );
        }

        List<SyndEntry> entries = feed.getEntries();

        for ( int i = 0; i < NUM_HEADLINES; i++ )
        {
            event.respondWith( entries.get( i ).getTitle() + " -> " + entries.get( i ).getLink() );
        }
    }
}
