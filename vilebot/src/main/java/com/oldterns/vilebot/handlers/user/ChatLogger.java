package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.LogDB;
import net.engio.mbassy.listener.Handler;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

/**
 * Created by eunderhi on 18/08/15.
 */

// @HandlerContainer
public class ChatLogger
    extends ListenerAdapter
{
    // @Handler
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
