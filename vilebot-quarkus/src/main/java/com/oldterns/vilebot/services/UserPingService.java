package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.OnMessage;
import org.kitteh.irc.client.library.element.User;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserPingService
{
    @OnMessage( "!ping" )
    public String ping( User user )
    {
        return Nick.getNick( user ).getFullNick() + ": pong";
    }
}
