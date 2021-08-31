package com.oldterns.vilebot.services.admin;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.database.LogDB;
import com.oldterns.vilebot.util.SessionService;
import org.kitteh.irc.client.library.element.User;

@ApplicationScoped
public class GetLogService
{

    @Inject
    SessionService sessionService;

    @Inject
    GroupDB groupDB;

    @Inject
    LogDB logDB;

    @OnMessage( "!admin showLog" )
    public String showLog( User sender )
    {
        String username = sessionService.getSession( Nick.getNick( sender ) );
        if ( groupDB.isAdmin( username ) )
        {
            return "Getting log...\n" + logDB.getLog();
        }
        return null;
    }

    @OnMessage( "!admin deleteLog" )
    public String deleteLog( User sender )
    {
        String username = sessionService.getSession( Nick.getNick( sender ) );
        if ( groupDB.isAdmin( username ) )
        {
            logDB.deleteLog();
            return "Log deleted.";
        }
        return null;
    }

}
