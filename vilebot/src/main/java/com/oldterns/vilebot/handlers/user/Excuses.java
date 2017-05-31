/**
 * Copyright (C) 2013 Oldterns
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

import com.oldterns.vilebot.db.ExcuseDB;

@HandlerContainer
public class Excuses
{
    private static final Pattern excusePattern = Pattern.compile( "!excuse" );

    @Handler
    private void excusesQuery( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = excusePattern.matcher( text );

        if ( matcher.matches() )
        {
            String excuse = ExcuseDB.getRandExcuse();
            if ( excuse != null )
            {
                event.reply( excuse );
            }
            else
            {
                event.reply( "No excuses available" );
            }
        }
    }

    // TODO method to add excuses
}
