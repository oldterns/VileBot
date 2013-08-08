/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.bot.misc.Colors;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import net.engio.mbassy.listener.Handler;

@HandlerContainer
public class LastMessageSed
{
    @AssignedBot
    private KeratinBot bot;

    /**
     * Map with String key of IRC nick, to String value of the last line of text.
     */
    private final Map<String, String> lastMessageMapByNick = new HashMap<String, String>();

    /**
     * Syncronise access to lastMessageMapByNick on this.
     */
    private final Object lastMessageMapByNickMutex = new Object();

    /**
     * Matches a standard sed-like replace pattern (ex: s/foo/bar/), the forward slash divisor can be replaced with any
     * punctuation character. The given seperator character cannot be used elsewhere.
     */
    private static final Pattern replacePattern = Pattern.compile( "^s(\\p{Punct})((?!\\1).+?)\\1((?!\\1).+?)(?:\\1(g|)|)$" );

    /**
     * Say the last thing the person said, replaced as specified. Otherwise just record the line as the last thing the
     * person said.
     */
    @Handler
    private void replace( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher sedMatcher = replacePattern.matcher( text );

        String nick = event.getSender();

        if ( sedMatcher.matches() )
        {
            if ( lastMessageMapByNick.containsKey( nick ) )
            {
                String regexp = sedMatcher.group( 2 );
                String replacement = sedMatcher.group( 3 );
                String endFlag = sedMatcher.group( 4 );

                synchronized ( lastMessageMapByNickMutex )
                {
                    String lastMessage = lastMessageMapByNick.get( nick );

                    if ( !lastMessage.contains( regexp ) )
                    {
                        event.reply( "Wow. Seriously? Try subbing out a string that actually occurred. Do you even sed, bro?" );
                    }
                    else
                    {
                        String replacedMsg;
                        String replacedMsgWHL;

                        String replacementWHL = Colors.bold( replacement );

                        // TODO: Probably can be simplified via method reference in Java 8
                        if ( "g".equals( endFlag ) )
                        {
                            replacedMsg = lastMessage.replaceAll( regexp, replacement );
                            replacedMsgWHL = lastMessage.replaceAll( regexp, replacementWHL );
                        }
                        else
                        {
                            replacedMsg = lastMessage.replaceFirst( regexp, replacement );
                            replacedMsgWHL = lastMessage.replaceFirst( regexp, replacementWHL );
                        }

                        event.reply( "Correction: " + replacedMsgWHL );
                        lastMessageMapByNick.put( nick, replacedMsg );
                    }
                }
            }
        }
        else
        {
            synchronized ( lastMessageMapByNickMutex )
            {
                lastMessageMapByNick.put( nick, text );
            }
        }
    }
}
