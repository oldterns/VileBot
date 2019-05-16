package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.db.PasswordDB;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminManagement
    extends ListenerAdapter
{
    private static final Pattern adminSetPattern = Pattern.compile( "!admin setadmin (\\S+) (\\S+)" );

    private static final Pattern adminRemPattern = Pattern.compile( "!admin remadmin (\\S+)" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher setMatcher = adminSetPattern.matcher( text );
        Matcher remMatcher = adminRemPattern.matcher( text );
        if ( setMatcher.matches() )
            set( event, setMatcher );
        if ( remMatcher.matches() )
            rem( event, remMatcher );
    }

    private void set( GenericMessageEvent event, Matcher matcher )
    {
        String editedAdminNick = matcher.group( 1 );
        String password = matcher.group( 2 );

        // Note that an empty admin group will allow a setadmin command to succeed (self-bootstrapping)

        String username = Sessions.getSession( event.getUser().getNick() );
        if ( GroupDB.noAdmins() || GroupDB.isAdmin( username ) )
        {
            if ( PasswordDB.setUserPassword( editedAdminNick, password ) )
            {
                GroupDB.addAdmin( editedAdminNick );
            }
            event.respondWith( "Added/modified admin " + editedAdminNick );
        }
        // }
    }

    // @Handler
    private void rem( GenericMessageEvent event, Matcher matcher )
    {
        String editedAdminNick = matcher.group( 1 );

        String username = Sessions.getSession( event.getUser().getNick() );
        if ( GroupDB.isAdmin( username ) )
        {
            if ( GroupDB.remAdmin( editedAdminNick ) )
            {
                PasswordDB.remUserPassword( editedAdminNick );
            }
            event.respondWith( "Removed admin " + editedAdminNick );
        }
    }
    // }
}
