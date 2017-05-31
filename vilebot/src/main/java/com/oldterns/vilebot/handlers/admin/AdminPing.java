/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.Sessions;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class AdminPing
{

    /**
     * Do I have admin access?
     */
    @Handler
    private void ping( ReceivePrivmsg event )
    {
        String text = event.getText();
        String sender = event.getSender();

        if ( "!admin ping".equals( text ) )
        {
            String username = Sessions.getSession( sender );
            if ( GroupDB.isAdmin( username ) )
            {
                event.replyDirectly( "You have an active admin session" );
            }
            else
            {
                event.replyDirectly( "You do not have an active admin session" );
            }
        }
    }
}
