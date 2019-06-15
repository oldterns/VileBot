/**
 * Copyright (C) 2019 Newterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.oldterns.vilebot.handlers.user;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

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
            printHeadlines( event );
        }
    }

    private void printHeadlines( GenericMessageEvent event )
    {
        event.respondWith("Get headlines");
    }

}
