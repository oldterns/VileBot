package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.vilebot.util.LimitService;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.LimitExceededException;

@ApplicationScoped
public class SummonService
{

    @Inject
    LimitService limitService;

    @OnChannelMessage( "!summon @nick" )
    public String summonNick( ChannelMessageEvent event, String nick )
    {
        Channel channel = event.getChannel();
        if ( channel.getUsers().stream().anyMatch( user -> Nick.valueOf( nick ).getBaseNick().equals( Nick.getNick( user ).getBaseNick() ) ) )
        {
            return nick + " is already in the channel.";
        }
        try
        {
            limitService.addUse( event.getActor() );
            event.getClient().sendMessage( nick, "You are summoned by " + Nick.getNick( event.getActor() ).getBaseNick()
                + " to join " + event.getChannel().getMessagingName() );
            return nick + " was invited";
        }
        catch ( LimitExceededException e )
        {
            return e.getMessage();
        }
    }
}
