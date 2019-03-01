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
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

//@HandlerContainer
public class Quit
    extends ListenerAdapter
{
    // @AssignedBot
    // private KeratinBot bot;

    // @Handler
    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        String nick = event.getUser().getNick();

        if ( "!admin quit".equals( text ) )
        {
            String username = Sessions.getSession( nick );
            if ( GroupDB.isAdmin( username ) )
            {
                event.getBot().send().quitServer();
                // bot.disconnect();

                System.exit( 0 );
            }
        }
    }
}
