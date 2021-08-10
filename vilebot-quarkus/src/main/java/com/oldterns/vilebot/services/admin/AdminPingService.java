package com.oldterns.vilebot.services.admin;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.util.SessionService;
import org.kitteh.irc.client.library.element.User;

@ApplicationScoped
public class AdminPingService
{
    @Inject
    SessionService sessionService;

    @Inject
    GroupDB groupDB;

    @OnMessage( "!admin ping" )
    public void onAdminPing( User user )
    {
        String username = sessionService.getSession( Nick.getNick( user ) );
        if ( groupDB.isAdmin( username ) )
        {
            user.sendMessage( "You have an active admin session" );
        }
        else
        {
            user.sendMessage( "You do not have an active admin session" );
        }
    }
}
