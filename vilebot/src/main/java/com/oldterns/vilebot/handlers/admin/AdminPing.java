package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

//@HandlerContainer
public class AdminPing
    extends ListenerAdapter
{

    /**
     * Do I have admin access?
     */
    // @Handler
    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        String sender = event.getUser().getNick();

        if ( "!admin ping".equals( text ) )
        {
            String username = Sessions.getSession( sender );
            if ( GroupDB.isAdmin( username ) )
            {
                event.respond( "You have an active admin session" );
            }
            else
            {
                event.respond( "You do not have an active admin session" );
            }
        }
    }
}
