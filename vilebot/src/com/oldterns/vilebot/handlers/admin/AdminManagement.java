package com.oldterns.vilebot.handlers.admin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.db.PasswordDB;
import com.oldterns.vilebot.util.Sessions;

@HandlerContainer
public class AdminManagement
{
    private static final Pattern adminSetPattern = Pattern.compile( "!admin setadmin (\\S+) (\\S+)" );

    private static final Pattern adminRemPattern = Pattern.compile( "!admin remadmin (\\S+)" );

    @Handler
    private void set( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = adminSetPattern.matcher( text );
        String sender = event.getSender();

        if ( matcher.matches() )
        {
            String editedAdminNick = matcher.group( 1 );
            String password = matcher.group( 2 );

            // Note that an empty admin group will allow a setadmin command to succeed (self-bootstrapping)

            String username = Sessions.getSession( sender );
            if ( GroupDB.noAdmins() || GroupDB.isAdmin( username ) )
            {
                if ( PasswordDB.setUserPassword( editedAdminNick, password ) )
                {
                    GroupDB.addAdmin( editedAdminNick );
                }
                event.reply( "Added/modified admin " + editedAdminNick );
            }
        }
    }

    @Handler
    private void rem( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = adminRemPattern.matcher( text );
        String sender = event.getSender();

        if ( matcher.matches() )
        {
            String editedAdminNick = matcher.group( 1 );

            String username = Sessions.getSession( sender );
            if ( GroupDB.isAdmin( username ) )
            {
                if ( GroupDB.remAdmin( editedAdminNick ) )
                {
                    PasswordDB.remUserPassword( editedAdminNick );
                }
                event.reply( "Removed admin " + editedAdminNick );
            }
        }
    }
}
