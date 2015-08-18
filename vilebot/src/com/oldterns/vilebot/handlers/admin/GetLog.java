package com.oldterns.vilebot.handlers.admin;

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

    private static final Pattern showLog = Pattern.compile("^!showLog$");
    private static final Pattern deleteLog = Pattern.compile("^!deleteLog$");

    @Handler
    private void getLog(ReceivePrivmsg event) {
        String text = event.getText();
        boolean showLogMatches = showLog.matcher(text).matches();
        boolean deleteLogMatches = deleteLog.matcher(text).matches();

        if (showLogMatches) {
            event.reply(LogDB.getLog());
        }
        else if(deleteLogMatches) {
            LogDB.deleteLog();
        }
    }
}
