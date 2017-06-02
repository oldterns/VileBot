/**
 * Copyright (C) 2013-2014 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class TwitterCorrection
{
    private static final Pattern twitterSyntaxUsePattern = Pattern.compile( "(?:^|\\s+)[@](\\S+)(?:\\s|:|)" );

    @Handler
    private void twitterBeGone( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = twitterSyntaxUsePattern.matcher( text );

        if ( matcher.find() )
        {
            String word = matcher.group( 1 );

            StringBuilder sb = new StringBuilder();

            sb.append( "You seem to be using twitter addressing syntax. On IRC you would say this instead: " );
            sb.append( word.replaceAll( "[^A-Za-z0-9]$", "" ) );
            sb.append( ": message" );

            event.replyDirectly( sb.toString() );
        }
    }
}
