/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.db.PasswordDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Sessions;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import net.engio.mbassy.listener.Handler;

@HandlerContainer
public class Auth
{
    private static final Pattern authPattern = Pattern.compile( "!admin auth (\\S+) (\\S+)" );

    private static final long sessionLength = TimeUnit.MINUTES.toMillis( 5 );

    @Handler
    private void auth( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = authPattern.matcher( text );
        String sender = event.getSender();

        if ( matcher.matches() )
        {
            String username = BaseNick.toBaseNick( matcher.group( 1 ) );
            String password = matcher.group( 2 );

            if ( GroupDB.isAdmin( username ) && PasswordDB.isValidPassword( username, password ) )
            {
                Sessions.addSession( sender, username, sessionLength );
                event.reply( "Authentication successful. Session active for "
                    + TimeUnit.MILLISECONDS.toMinutes( sessionLength ) + " minutes." );
            }
            else
            {
                event.reply( "Authentication failed" );
            }
        }
    }
}
