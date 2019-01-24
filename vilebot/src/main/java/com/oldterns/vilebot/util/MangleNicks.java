package com.oldterns.vilebot.util;

import java.util.Collection;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.core.event.message.recieve.ReceiveJoin;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

/**
 * Reverse all nicks in messages
 */
public class MangleNicks()
{

    public static String mangleNicks(KeratinBot bot, ReceivePrivmsg event, String message) {
        return mangleNicks(bot, event.getChannel(), message);
    }

    public static String mangleNicks(KeratinBot bot, ReceiveJoin event, String message) {
        return mangleNicks(bot, event.getChannel(), message);
    }

    private static String mangleNicks(KeratinBot bot, String channel, String message) {
        List<String> nicks;
        try {
            nicks = bot.getChannel( channel ).getNicks();
        } catch (Exception e) {
            // nicks list is empty
            return message;
        }
        StringBuilder reply = new StringBuilder();
        for ( String word : message.split( " " ) )
        {
            reply.append( " " );
            reply.append( inside( nicks, word ) ? mangled( word ) : word );
        }
        return reply.toString().trim();
    }

    private static String mangled(String word) {
		return new StringBuilder( word ).reverse().toString();
	}
}