/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.Sessions;

@HandlerContainer
public class Quit
{
    @AssignedBot
    private KeratinBot bot;

    @Handler
    private void quit( ReceivePrivmsg event )
    {
        String text = event.getText();
        String nick = event.getSender();

        if ( "!admin quit".equals( text ) )
        {
            String username = Sessions.getSession( nick );
            if ( GroupDB.isAdmin( username ) )
            {
                bot.disconnect();

                System.exit( 0 );
            }
        }
    }
}
