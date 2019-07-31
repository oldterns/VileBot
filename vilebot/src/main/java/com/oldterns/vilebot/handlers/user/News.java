/**
 * Copyright (C) 2019 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.util.LimitCommand;
import com.oldterns.vilebot.util.NewsParser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class News
    extends NewsParser
{
    private static final Logger logger = LoggerFactory.getLogger( News.class );

    private static final String DEFAULT_CATEGORY = "toronto";

    private static final LinkedHashMap<String, ImmutablePair<String, URL>> newsFeedsByCategory = new LinkedHashMap<>();
    static
    {
        try
        {
            newsFeedsByCategory.put( "top",
                                     new ImmutablePair<>( "General",
                                                          new URL( "https://rss.cbc.ca/lineup/topstories.xml" ) ) );
            newsFeedsByCategory.put( "world",
                                     new ImmutablePair<>( "General",
                                                          new URL( "https://news.google.com/news/rss/headlines/section/topic/WORLD?ned=us&hl=en" ) ) );
            newsFeedsByCategory.put( "canada",
                                     new ImmutablePair<>( "General",
                                                          new URL( "https://rss.cbc.ca/lineup/canada.xml" ) ) );
            newsFeedsByCategory.put( "usa",
                                     new ImmutablePair<>( "General",
                                                          new URL( "http://feeds.reuters.com/Reuters/domesticNews" ) ) );
            newsFeedsByCategory.put( "britain",
                                     new ImmutablePair<>( "General",
                                                          new URL( "http://feeds.bbci.co.uk/news/uk/rss.xml" ) ) );
            newsFeedsByCategory.put( "redhat",
                                     new ImmutablePair<>( "Open Source",
                                                          new URL( "https://www.redhat.com/en/rss/blog/channel/red-hat-news" ) ) );
            newsFeedsByCategory.put( "fedora", new ImmutablePair<>( "Open Source",
                                                                    new URL( "http://fedoraplanet.org/rss20.xml" ) ) );
            newsFeedsByCategory.put( "openshift",
                                     new ImmutablePair<>( "Open Source",
                                                          new URL( "https://blog.openshift.com/category/news/rss" ) ) );
            newsFeedsByCategory.put( "opensource",
                                     new ImmutablePair<>( "Open Source", new URL( "https://opensource.com/feed" ) ) );
            newsFeedsByCategory.put( "politics",
                                     new ImmutablePair<>( "Topics",
                                                          new URL( "https://rss.cbc.ca/lineup/politics.xml" ) ) );
            newsFeedsByCategory.put( "business",
                                     new ImmutablePair<>( "Topics",
                                                          new URL( "https://rss.cbc.ca/lineup/business.xml" ) ) );
            newsFeedsByCategory.put( "health",
                                     new ImmutablePair<>( "Topics",
                                                          new URL( "https://rss.cbc.ca/lineup/health.xml" ) ) );
            newsFeedsByCategory.put( "arts",
                                     new ImmutablePair<>( "Topics", new URL( "https://rss.cbc.ca/lineup/arts.xml" ) ) );
            newsFeedsByCategory.put( "tech",
                                     new ImmutablePair<>( "Topics",
                                                          new URL( "https://rss.cbc.ca/lineup/technology.xml" ) ) );
            newsFeedsByCategory.put( "offbeat",
                                     new ImmutablePair<>( "Topics",
                                                          new URL( "https://rss.cbc.ca/lineup/offbeat.xml" ) ) );
            newsFeedsByCategory.put( "indigenous",
                                     new ImmutablePair<>( "Topics",
                                                          new URL( "https://www.cbc.ca/cmlink/rss-cbcaboriginal" ) ) );
            newsFeedsByCategory.put( "sports",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports.xml" ) ) );
            newsFeedsByCategory.put( "mlb",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports-mlb.xml" ) ) );
            newsFeedsByCategory.put( "nba",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports-nba.xml" ) ) );
            newsFeedsByCategory.put( "cfl",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports-cfl.xml" ) ) );
            newsFeedsByCategory.put( "nfl",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports-nfl.xml" ) ) );
            newsFeedsByCategory.put( "nhl",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports-nhl.xml" ) ) );
            newsFeedsByCategory.put( "soccer",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports-soccer.xml" ) ) );
            newsFeedsByCategory.put( "curling",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports-curling.xml" ) ) );
            newsFeedsByCategory.put( "skating",
                                     new ImmutablePair<>( "Sports",
                                                          new URL( "https://rss.cbc.ca/lineup/sports-figureskating.xml" ) ) );
            newsFeedsByCategory.put( "bc",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-britishcolumbia.xml" ) ) );
            newsFeedsByCategory.put( "kamloops",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-kamloops.xml" ) ) );
            newsFeedsByCategory.put( "calgary",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-calgary.xml" ) ) );
            newsFeedsByCategory.put( "edmonton",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-edmonton.xml" ) ) );
            newsFeedsByCategory.put( "saskatchewan",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-saskatchewan.xml" ) ) );
            newsFeedsByCategory.put( "saskatoon",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-saskatoon.xml" ) ) );
            newsFeedsByCategory.put( "manitoba",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-manitoba.xml" ) ) );
            newsFeedsByCategory.put( "thunderbay",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-thunderbay.xml" ) ) );
            newsFeedsByCategory.put( "sudbury",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-sudbury.xml" ) ) );
            newsFeedsByCategory.put( "windsor",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-windsor.xml" ) ) );
            newsFeedsByCategory.put( "london",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://www.cbc.ca/cmlink/rss-canada-london" ) ) );
            newsFeedsByCategory.put( "waterloo",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-kitchenerwaterloo.xml" ) ) );
            newsFeedsByCategory.put( "toronto",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-toronto.xml" ) ) );
            newsFeedsByCategory.put( "hamilton",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-hamiltonnews.xml" ) ) );
            newsFeedsByCategory.put( "montreal",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-montreal.xml" ) ) );
            newsFeedsByCategory.put( "newbrunswick",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-newbrunswick.xml" ) ) );
            newsFeedsByCategory.put( "pei",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-pei.xml" ) ) );
            newsFeedsByCategory.put( "novascotia",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-novascotia.xml" ) ) );
            newsFeedsByCategory.put( "newfoundland",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-newfoundland.xml" ) ) );
            newsFeedsByCategory.put( "north",
                                     new ImmutablePair<>( "Regional",
                                                          new URL( "https://rss.cbc.ca/lineup/canada-north.xml" ) ) );
        }
        catch ( MalformedURLException e )
        {
            logger.error( "Error loading news URLs" );
            throw new RuntimeException( e );
        }
    }

    private static final Pattern NEWS_PATTERN = Pattern.compile( "^!news(?: ([a-zA-Z]+)|)" );

    private static final Pattern NEWS_HELP_PATTERN = Pattern.compile( "^!news help" );

    private final String HELP_MESSAGE = generateHelpMessage();

    private final String HELP_COMMAND = "'!news help'";

    public static LimitCommand limitCommand = new LimitCommand();

    private static final String RESTRICTED_CHANNEL = Vilebot.getConfig().get( "ircChannel1" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = NEWS_PATTERN.matcher( text );
        Matcher helpMatcher = NEWS_HELP_PATTERN.matcher( text );

        if ( helpMatcher.matches() )
        {
            for ( String line : HELP_MESSAGE.split( "\n" ) )
            {
                event.respondPrivateMessage( line );
            }
        }
        else if ( matcher.matches() )
        {
            currentNews( event, matcher, newsFeedsByCategory, DEFAULT_CATEGORY, HELP_COMMAND, limitCommand,
                         RESTRICTED_CHANNEL, logger );
        }
    }

    @Override
    protected String generateHelpMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "News Categories (example: !news toronto):" );

        String prevGenre = null;

        for ( String category : newsFeedsByCategory.keySet() )
        {
            String currentGenre = newsFeedsByCategory.get( category ).getLeft();

            if ( !currentGenre.equals( prevGenre ) )
            {
                sb.append( "\n" );
                sb.append( "  " + currentGenre + ":" );
                prevGenre = currentGenre;
            }

            sb.append( " { " + category + " }" );
        }

        return sb.toString();
    }
}
