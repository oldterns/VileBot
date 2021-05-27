package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserPingService
{
    @OnChannelMessage( "!ping" )
    public String ping( ChannelMessageEvent ircMessage )
    {
        return Nick.getUser( ircMessage ).getBaseNick() + ": pong";
    }
}
