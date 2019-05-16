/*
  Copyright (C) 2013 Oldterns

  This file may be modified and distributed under the terms
  of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickChange
    extends ListenerAdapter
{
    private static final Pattern nickChangePattern = Pattern.compile( "!admin nick ([a-zA-Z][a-zA-Z0-9-_|]+)" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = nickChangePattern.matcher( text );
        String sender = event.getUser().getNick();

        if ( matcher.matches() )
        {
            String username = Sessions.getSession( sender );
            if ( GroupDB.isAdmin( username ) )
            {
                String newNick = matcher.group( 1 );
                event.getBot().send().changeNick( newNick );
                BaseNick.addBotNick( newNick );

                event.respondWith( "Nick changed" );
            }
        }
    }
}
