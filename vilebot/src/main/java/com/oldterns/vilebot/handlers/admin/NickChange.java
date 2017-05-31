/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Sessions;

import net.engio.mbassy.listener.Handler;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class NickChange
{
    private static final Pattern nickChangePattern = Pattern.compile( "!admin nick ([a-zA-Z][a-zA-Z0-9-_|]+)" );

    @AssignedBot
    private KeratinBot bot;

    @Handler
    private void changeNick( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = nickChangePattern.matcher( text );
        String sender = event.getSender();

        if ( matcher.matches() )
        {
            String username = Sessions.getSession( sender );
            if ( GroupDB.isAdmin( username ) )
            {
                String newNick = matcher.group( 1 );
                bot.setNick( newNick );
                BaseNick.addBotNick( newNick );

                event.reply( "Nick changed" );
            }
        }
    }
}
