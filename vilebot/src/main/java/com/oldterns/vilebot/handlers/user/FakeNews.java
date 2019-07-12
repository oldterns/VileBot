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

public class FakeNews
    extends NewsParser
{

    private static final Logger logger = LoggerFactory.getLogger( FakeNews.class );

    private static final String DEFAULT_CATEGORY = "canada";

    private static final LinkedHashMap<String, ImmutablePair<String, URL>> fakeNewsFeedsByCategory =
        new LinkedHashMap<>();
    static
    {
        try
        {
            fakeNewsFeedsByCategory.put( "canada",
                                         new ImmutablePair<>( "Countries",
                                                              new URL( "https://www.thebeaverton.com/rss" ) ) );
            fakeNewsFeedsByCategory.put( "usa", new ImmutablePair<>( "Countries",
                                                                     new URL( "https://www.theonion.com/rss" ) ) );
            fakeNewsFeedsByCategory.put( "belgium",
                                         new ImmutablePair<>( "Countries", new URL( "https://nordpresse.be/rss" ) ) );
            fakeNewsFeedsByCategory.put( "france",
                                         new ImmutablePair<>( "Countries", new URL( "http://www.legorafi.fr/rss" ) ) );
            fakeNewsFeedsByCategory.put( "india", new ImmutablePair<>( "Countries",
                                                                       new URL( "http://www.fakingnews.com/rss" ) ) );
            fakeNewsFeedsByCategory.put( "russia",
                                         new ImmutablePair<>( "Countries", new URL( "https://fognews.ru/rss" ) ) );
            fakeNewsFeedsByCategory.put( "serbia",
                                         new ImmutablePair<>( "Countries", new URL( "https://www.njuz.net/rss" ) ) );
            fakeNewsFeedsByCategory.put( "venezuela",
                                         new ImmutablePair<>( "Countries",
                                                              new URL( "http://feeds.feedburner.com/elchiguirebipolar" ) ) );
            fakeNewsFeedsByCategory.put( "newzealand",
                                         new ImmutablePair<>( "Countries",
                                                              new URL( "http://www.thecivilian.co.nz/rss" ) ) );
        }
        catch ( MalformedURLException e )
        {
            logger.error( "Error loading fake news URLs" );
            throw new RuntimeException( e );
        }
    }

    private static final Pattern FAKE_NEWS_PATTERN = Pattern.compile( "^!fakenews(?: ([a-zA-Z]+)|)" );

    private static final Pattern FAKE_NEWS_HELP_PATTERN = Pattern.compile( "^!fakenews help" );

    private final String HELP_MESSAGE = generateHelpMessage();

    private final String HELP_COMMAND = "'!fakenews help'";

    public static LimitCommand limitCommand = new LimitCommand();

    private static final String RESTRICTED_CHANNEL = Vilebot.getConfig().get( "ircChannel1" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = FAKE_NEWS_PATTERN.matcher( text );
        Matcher helpMatcher = FAKE_NEWS_HELP_PATTERN.matcher( text );

        if ( helpMatcher.matches() )
        {
            for ( String line : HELP_MESSAGE.split( "\n" ) )
            {
                event.respondPrivateMessage( line );
            }
        }
        else if ( matcher.matches() )
        {
            currentNews( event, matcher, fakeNewsFeedsByCategory, DEFAULT_CATEGORY, HELP_COMMAND, limitCommand,
                         RESTRICTED_CHANNEL, logger );
        }
    }

    @Override
    protected String generateHelpMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Fake News Categories (example: !fakenews canada):" );
        sb.append( "\n" );

        for ( String category : fakeNewsFeedsByCategory.keySet() )
        {
            sb.append( " { " + category + " }" );
        }

        return sb.toString();
    }
}
