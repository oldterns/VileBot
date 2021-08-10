package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnChannelMessage;
import org.kitteh.irc.client.library.element.User;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserPingService
{
    @OnChannelMessage( "!ping" )
    public String ping( User user )
    {
        return Nick.getNick( user ).getFullNick() + ": pong";
    }
}
