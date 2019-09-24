/**
 * Copyright (C) 2019 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.oldterns.vilebot.handlers.user;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.util.LimitCommand;
import com.oldterns.vilebot.util.NewsParser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reddit
    extends NewsParser
{
    private static final Logger logger = LoggerFactory.getLogger( Reddit.class );

    private static final Pattern REDDIT_PATTERN = Pattern.compile( "^!reddit(?: ([a-zA-Z]+)|)" );

    private static final Pattern REDDIT_HELP_PATTERN = Pattern.compile( "^!reddit help" );

    private final String HELP_MESSAGE = generateHelpMessage();

    private final String HELP_COMMAND = "'!reddit help'";

    public static LimitCommand limitCommand = new LimitCommand();

    private static final String RESTRICTED_CHANNEL = Vilebot.getConfig().get( "ircChannel1" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = REDDIT_PATTERN.matcher( text );
        Matcher helpMatcher = REDDIT_HELP_PATTERN.matcher( text );

        if ( helpMatcher.matches() )
        {
            for ( String line : HELP_MESSAGE.split( "\n" ) )
            {
                event.respondPrivateMessage( line );
            }
        }
        else if ( matcher.matches() )
        {
            Map<String, ImmutablePair<String, URL>> redditRSSMap = new HashMap<>();
            String subreddit = matcher.group( 1 ) != null ? matcher.group( 1 ) : "";
            try
            {
                redditRSSMap.put( subreddit,
                                  new ImmutablePair<String, URL>( "Subredit", new URL( new StringBuilder(
                                                                                                          "https://reddit.com/" ).append( subreddit.isEmpty() ? subreddit : "r/" + subreddit + "/" ).append( ".rss" ).toString() ) ) );
                currentNews( event, matcher, redditRSSMap, subreddit, HELP_COMMAND, limitCommand, RESTRICTED_CHANNEL,
                             logger );
            }
            catch ( MalformedURLException e )
            {
                event.respond( subreddit + " is not a subreddit." );
            }

        }
    }

    @Override
    protected String generateHelpMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Reddit (example: !reddit toronto)\n" );

        return sb.toString();
    }
}
