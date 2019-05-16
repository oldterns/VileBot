/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class Quit
    extends ListenerAdapter
{
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

                System.exit( 0 );
            }
        }
    }
}
