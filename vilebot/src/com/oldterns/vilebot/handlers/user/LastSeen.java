/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.oldterns.vilebot.db.LastSeenDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Ignore;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceiveJoin;
import ca.szc.keratin.core.event.message.recieve.ReceivePart;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import ca.szc.keratin.core.event.message.recieve.ReceiveQuit;

@HandlerContainer
public class LastSeen
{
    @AssignedBot
    private KeratinBot bot;

    private static TimeZone timeZone = TimeZone.getTimeZone( "America/Toronto" );

    private static DateFormat dateFormat = makeDateFormat();

    @Handler
    private void longTimeNoSee( ReceiveJoin event )
    {
        String joiner = BaseNick.toBaseNick( event.getJoiner() );

        if ( !bot.getNick().equals( joiner ) && !Ignore.getOnJoin().contains( joiner ) )
        {
            long lastSeen = LastSeenDB.getLastSeenTime( joiner );
            long now = System.currentTimeMillis();
            long timeAgo = now - lastSeen;

            long daysAgo = TimeUnit.MILLISECONDS.toDays( timeAgo );

            if ( daysAgo > 30 )
            {
                StringBuilder sb = new StringBuilder();

                sb.append( "Hi " );
                sb.append( event.getJoiner() );
                sb.append( "! I last saw you " );
                sb.append( daysAgo );
                sb.append( " days ago at " );
                sb.append( dateFormat.format( new Date( lastSeen ) ) );
                sb.append( ". Long time, no see." );

                event.reply( sb.toString() );
            }

            LastSeenDB.updateLastSeenTime( joiner );
        }
    }

    @Handler
    private void updateLastSeenOnPrivmsg( ReceivePrivmsg event )
    {
        String nick = BaseNick.toBaseNick( event.getSender() );

        if ( !bot.getNick().equals( nick ) )
            LastSeenDB.updateLastSeenTime( nick );
    }

    @Handler
    private void updateLastSeenOnPart( ReceivePart event )
    {
        String nick = BaseNick.toBaseNick( event.getParter() );

        if ( !bot.getNick().equals( nick ) )
            LastSeenDB.updateLastSeenTime( nick );
    }

    @Handler
    private void updateLastSeenOnQuit( ReceiveQuit event )
    {
        String nick = BaseNick.toBaseNick( event.getQuitter() );

        if ( !bot.getNick().equals( nick ) )
            LastSeenDB.updateLastSeenTime( nick );
    }

    private static DateFormat makeDateFormat()
    {
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mmX" );
        df.setTimeZone( timeZone );
        return df;
    }
}
