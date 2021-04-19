package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import org.apache.camel.component.irc.IrcMessage;

public class ChannelMessage {
    final Nick nick;
    final String channel;
    final String message;

    public ChannelMessage(IrcMessage ircMessage) {
        nick = Nick.valueOf((String) ircMessage.getHeader("irc.user.nick"));
        channel = (String) ircMessage.getHeader("irc.target");
        message = ircMessage.getBody(String.class);
    }

    public Nick getNick() {
        return nick;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }
}
