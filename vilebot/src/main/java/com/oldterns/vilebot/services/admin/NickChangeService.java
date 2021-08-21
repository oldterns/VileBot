package com.oldterns.vilebot.services.admin;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.util.SessionService;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.User;

@ApplicationScoped
public class NickChangeService
{

    @Inject
    SessionService sessionService;

    @Inject
    GroupDB groupDB;

    @OnMessage( "!admin nick @newNick" )
    public String changeNick( Client client, User sender, Nick newNick )
    {
        String username = sessionService.getSession( Nick.getNick( sender ) );
        if ( groupDB.isAdmin( username ) )
        {
            client.setNick( newNick.getFullNick() );
            return "Nick changed";
        }
        return null;
    }
}
