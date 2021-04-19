package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnChannelMessage;
import org.apache.camel.component.irc.IrcMessage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
public class UserPing
{

    @OnChannelMessage( "!ping" )
    public String ping( ChannelMessage ircMessage )
    {
        return ircMessage.getNick().getBaseNick() + ": pong";
    }

    // @Override
    // public void configure() throws Exception {
    // fromMainChannel()
    // .pipeline()
    // .filter(messageTextMatches(text -> text.equals("!ping")))
    // .process(reply(ircMessage -> ircMessage.getUser().getNick() + ": pong"))
    // .to(getMainChannel());
    // }
}
