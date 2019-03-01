/**
 * Copyright (C) 2013-2014 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@HandlerContainer
public class TwitterCorrection
    extends ListenerAdapter
{
    private static final Pattern twitterSyntaxUsePattern = Pattern.compile( "(?:^|\\s+)[@](\\S+)(?:\\s|:|)" );

    // @Handler
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = twitterSyntaxUsePattern.matcher( text );

        if ( matcher.find() )
        {
            String word = matcher.group( 1 );

            String sb = "You seem to be using twitter addressing syntax. On IRC you would say this instead: "
                + word.replaceAll( "[^A-Za-z0-9]$", "" ) + ": message";
            event.respond( sb );
        }
    }
}
