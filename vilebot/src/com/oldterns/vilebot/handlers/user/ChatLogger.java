package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.db.LogDB;
import net.engio.mbassy.listener.Handler;

/**
 * Created by eunderhi on 18/08/15.
 */

@HandlerContainer
public class ChatLogger {
    @Handler
    protected void logMessage(ReceivePrivmsg event) {
        if(event.getChannel().equals("#thefoobar")) {
            String text = event.getText() + "\n";
            LogDB.addItem(text);
        }
    }
}
