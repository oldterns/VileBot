package com.oldterns.vilebot.handlers.user;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.util.BaseNick;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class Summon
    extends ListenerAdapter
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );

    private static final Pattern summonPattern = Pattern.compile( "^!summon (" + nounPattern + ")\\s*$" );

    private static ExecutorService timer = Executors.newScheduledThreadPool( 1 );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();

        Matcher summonMatcher = summonPattern.matcher( text );

        if ( summonMatcher.matches() )
            summonNick( event, summonMatcher.group( 1 ) );
    }

    private void summonNick( GenericMessageEvent event, String nick )
    {
        event.getBot().sendIRC().invite( nick, Vilebot.getConfig().get( "ircChannel1" ) );
        event.getBot().sendIRC().message( nick,
                                          "You are summoned by " + BaseNick.toBaseNick( event.getUser().getNick() )
                                              + " to join " + Vilebot.getConfig().get( "ircChannel1" ) );
        event.respond( nick + " was invited." );

    }

}
