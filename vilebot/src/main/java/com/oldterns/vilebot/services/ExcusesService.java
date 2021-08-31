package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.database.ExcuseDB;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ExcusesService
{

    @Inject
    ExcuseDB excuseDB;

    @OnMessage( "!excuse" )
    public String getExcuse()
    {
        String excuse = excuseDB.getRandExcuse();
        if ( excuse != null )
        {
            return excuse;
        }
        else
        {
            return "No excuses available";
        }
    }

    @OnChannelMessage( "!excuseadd @excuse" )
    public String addExcuse( String excuse )
    {
        excuseDB.addExcuse( excuse );
        return "Excuse was added to database";
    }
}
