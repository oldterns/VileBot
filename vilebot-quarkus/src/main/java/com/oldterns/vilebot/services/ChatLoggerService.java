package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.Regex;
import com.oldterns.vilebot.database.LogDB;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by eunderhi on 18/08/15.
 */
@ApplicationScoped
public class ChatLoggerService
{

    @Inject
    LogDB logDB;

    @OnChannelMessage( value = "@message", channel = "${vilebot.markov.channel}" )
    public void logMessage( @Regex( "[^!].*" ) String message )
    {
        logDB.addItem( message + "\n" );
    }
}
