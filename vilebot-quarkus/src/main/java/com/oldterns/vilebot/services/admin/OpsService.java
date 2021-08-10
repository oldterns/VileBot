package com.oldterns.vilebot.services.admin;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.Delimiter;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.util.SessionService;
import org.kitteh.irc.client.library.element.User;

@ApplicationScoped
public class OpsService
{

    @Inject
    SessionService sessionService;

    @Inject
    GroupDB groupDB;

    @OnMessage( "!admin op @nickList" )
    public String autoOpNicks( User sender, @Delimiter( "\\s+" ) List<Nick> nickList )
    {
        String username = sessionService.getSession( Nick.getNick( sender ) );
        if ( groupDB.isAdmin( username ) )
        {
            StringBuilder successNicks = new StringBuilder();
            StringBuilder failureNicks = new StringBuilder();

            for ( Nick nick : nickList )
            {
                StringBuilder selectedSB;
                if ( groupDB.addOp( nick.getBaseNick() ) )
                    selectedSB = successNicks;
                else
                    selectedSB = failureNicks;

                selectedSB.append( nick );
                selectedSB.append( " " );
            }

            StringBuilder out = new StringBuilder();
            if ( successNicks.length() > 0 )
                out.append( "Added " + successNicks.toString() + "to operator group\n" );
            if ( failureNicks.length() > 0 )
                out.append( failureNicks.toString() + "was/were already in the operator group\n" );
            return out.toString();
        }
        return null;
    }

    @OnMessage( "!admin unop @nickList" )
    public String removeAutoOpNicks( User sender, @Delimiter( "\\s+" ) List<Nick> nickList )
    {
        String username = sessionService.getSession( Nick.getNick( sender ) );
        if ( groupDB.isAdmin( username ) )
        {
            StringBuilder successNicks = new StringBuilder();
            StringBuilder failureNicks = new StringBuilder();

            for ( Nick nick : nickList )
            {
                StringBuilder selectedSB;
                if ( groupDB.remOp( nick.getBaseNick() ) )
                    selectedSB = successNicks;
                else
                    selectedSB = failureNicks;

                selectedSB.append( nick );
                selectedSB.append( " " );
            }

            StringBuilder out = new StringBuilder();
            if ( successNicks.length() > 0 )
                out.append( "Removed " + successNicks.toString() + "from operator group\n" );
            if ( failureNicks.length() > 0 )
                out.append( failureNicks.toString() + "was/were not in the operator group" );
            return out.toString();
        }
        return null;
    }
}
