package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.db.LogDB;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.regex.Pattern;

public class GetLog
    extends ListenerAdapter
{

    private static final Pattern showLog = Pattern.compile( "!admin showLog$" );

    private static final Pattern deleteLog = Pattern.compile( "!admin deleteLog$" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        String sender = event.getUser().getNick();
        String username = Sessions.getSession( sender );

        boolean showLogMatches = showLog.matcher( text ).matches();
        boolean deleteLogMatches = deleteLog.matcher( text ).matches();

        if ( GroupDB.isAdmin( username ) )
        {
            if ( showLogMatches )
            {
                event.respondWith( "Getting log..." );
                event.respondWith( LogDB.getLog() );
            }
            else if ( deleteLogMatches )
            {
                LogDB.deleteLog();
                event.respondWith( "Log deleted" );
            }
        }
    }
}
