package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.db.LogDB;
import net.engio.mbassy.listener.Handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eunderhi on 18/08/15.
 */
@HandlerContainer
public class GetLog {

    private static final Pattern command = Pattern.compile("^!log$");

    @Handler
    private void getLog(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher matcher = command.matcher(text);

        if (matcher.matches()) {
            event.reply(LogDB.getLog());
        }
    }
}
