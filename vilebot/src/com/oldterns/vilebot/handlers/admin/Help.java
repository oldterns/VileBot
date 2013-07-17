/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class Help
{
    private static final Pattern helpPattern = Pattern.compile( "!admin help" );

    private static final String helpMessage = generateHelpMessage();

    @Handler
    private void adminHelp( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = helpPattern.matcher( text );

        if ( matcher.matches() )
        {
            event.replyPrivately( helpMessage );
        }
    }

    private static String generateHelpMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Available Commands:" );

        sb.append( " { !admin help }" );
        sb.append( " { !admin ping }" );
        sb.append( " { !admin auth <user> <pass> }" );
        sb.append( " { !admin quit }" );
        sb.append( " { !admin nick <nick> }" );
        sb.append( " { !admin op <nick> }" );
        sb.append( " { !admin unop <nick> }" );
        sb.append( " { !admin setadmin <nick> <pass> }" );
        sb.append( " { !admin remadmin <nick> }" );

        return sb.toString();
    }
}
