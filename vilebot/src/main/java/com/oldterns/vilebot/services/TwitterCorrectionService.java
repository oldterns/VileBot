package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.Regex;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TwitterCorrectionService
{

    @OnChannelMessage( ".*@twitterCallout.*" )
    public String onTwitterSyntax( @Regex( "@(\\S+):?" ) String twitterCallout )
    {
        return "You seem to be using twitter addressing syntax. On IRC you would say this instead: "
            + twitterCallout.substring( 1 ).replaceAll( "[^A-Za-z0-9]$", "" ) + ": message";
    }
}
