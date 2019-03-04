package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.output.OutputIRC;

public class AdminPing
    extends ListenerAdapter
{

    /**
     * Do I have admin access?
     */

    @Override
    public void onPrivateMessage( final PrivateMessageEvent event )
    {
        String replyTarget = event.getUser().getNick();
        OutputIRC outputQ = event.getBot().send();
        String text = event.getMessage();
        String sender = event.getUser().getNick();
        adminPing( outputQ, replyTarget, text, sender );
    }

    @Override
    public void onMessage( final MessageEvent event )
    {
        String replyTarget = event.getChannel().getName();
        OutputIRC outputQ = event.getBot().send();
        String text = event.getMessage();
        String sender = event.getUser().getNick();
        adminPing( outputQ, replyTarget, text, sender );
    }

    private void adminPing( OutputIRC outputQ, String replyTarget, String text, String sender )
    {
        if ( "!admin ping".equals( text ) )
        {
            String username = Sessions.getSession( sender );
            if ( GroupDB.isAdmin( username ) )
            {
                outputQ.message( replyTarget, "You have an active admin session" );
            }
            else
            {
                outputQ.message( replyTarget, "You do not have an active admin session" );
            }
        }
    }
}
