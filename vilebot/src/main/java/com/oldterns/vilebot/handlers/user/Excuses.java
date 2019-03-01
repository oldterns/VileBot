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
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

//@HandlerContainer
public class Excuses
    extends ListenerAdapter
{
    private static final Pattern excusePattern = Pattern.compile( "!excuse" );

    // @Handler
    @Override
    public void onGenericMessage( GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = excusePattern.matcher( text );

        if ( matcher.matches() )
        {
            String excuse = ExcuseDB.getRandExcuse();
            if ( excuse != null )
            {
                event.respondWith( excuse );
            }
            else
            {
                event.respondWith( "No excuses available" );
            }
        }
    }

    // TODO method to add excuses
}
