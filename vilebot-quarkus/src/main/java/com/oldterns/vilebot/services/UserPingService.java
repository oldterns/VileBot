package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import org.kitteh.irc.client.library.element.User;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserPingService
{
    @OnChannelMessage( "!ping" )
    public String ping( User user )
    {
        return Nick.getUser( user ).getBaseNick() + ": pong";
    }
}
