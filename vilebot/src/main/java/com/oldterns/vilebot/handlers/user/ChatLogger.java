package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.LogDB;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

/**
 * Created by eunderhi on 18/08/15.
 */

public class ChatLogger
    extends ListenerAdapter
{
    @Override
    public void onMessage( final MessageEvent event )
    {
        String logChannel = Vilebot.getConfig().get( "markovChannel" );
        String channel = event.getChannel().getName();
        String text = event.getMessage();

        boolean shouldLog = channel.equals( logChannel ) && !text.startsWith( "!" );

        if ( shouldLog )
        {
            LogDB.addItem( text + "\n" );
        }
    }
}
