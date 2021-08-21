package com.oldterns.vilebot.services.admin;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.database.PasswordDB;
import com.oldterns.vilebot.util.SessionService;
import org.kitteh.irc.client.library.element.User;

@ApplicationScoped
public class AdminManagementService
{
    @Inject
    PasswordDB passwordDB;

    @Inject
    GroupDB groupDB;

    @Inject
    SessionService sessionService;

    @OnMessage( "!setadmin @editedAdminNick @password" )
    public String setAdmin( User user, Nick editedAdminNick, String password )
    {
        // TODO: The way this is written, one admin can change another admin password
        String username = sessionService.getSession( Nick.getNick( user ) );
        if ( groupDB.noAdmins() || groupDB.isAdmin( username ) )
        {
            if ( passwordDB.setUserPassword( editedAdminNick.getBaseNick(), password ) )
            {
                groupDB.addAdmin( editedAdminNick.getBaseNick() );
            }
            return "Added/modified admin " + editedAdminNick.getBaseNick();
        }
        return null;
    }

    @OnMessage( "!remadmin @editedAdminNick" )
    public String removeAdmin( User user, Nick editedAdminNick )
    {
        // One admin can remove other admins, which is needed if they ever leaves
        // However, there is nothing that can be done if the only admin leaves
        String username = sessionService.getSession( Nick.getNick( user ) );
        if ( groupDB.isAdmin( username ) )
        {
            if ( groupDB.remAdmin( editedAdminNick.getBaseNick() ) )
            {
                passwordDB.removeUserPassword( editedAdminNick.getBaseNick() );
            }
            return "Removed admin " + editedAdminNick;
        }
        return null;
    }

}
