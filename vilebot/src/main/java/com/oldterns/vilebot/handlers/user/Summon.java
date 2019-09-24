package com.oldterns.vilebot.handlers.user;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.LimitCommand;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class Summon
    extends ListenerAdapter
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );

    private static final Pattern summonPattern = Pattern.compile( "^!summon (" + nounPattern + ")\\s*$" );

    public static LimitCommand limitCommand = new LimitCommand();

    private static final String RESTRICTED_CHANNEL = Vilebot.getConfig().get( "ircChannel1" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {

        if ( event instanceof MessageEvent
            && ( (MessageEvent) event ).getChannel().getName().equals( RESTRICTED_CHANNEL ) )
        {
            String text = event.getMessage();

            Matcher summonMatcher = summonPattern.matcher( text );

            if ( summonMatcher.matches() )
            {
                String toSummon = summonMatcher.group( 1 ).trim();
                if ( ( (MessageEvent) event ).getChannel().getUsers().stream().filter( u -> u.getNick().equals( toSummon ) ).findAny().isPresent() )
                {
                    event.respondWith( toSummon + " is already in the channel." );
                }
                else
                {
                    String isLimit = limitCommand.addUse( event.getUser().getNick() );
                    if ( isLimit.isEmpty() )
                        summonNick( event, toSummon );
                    else
                        event.respondWith( isLimit );
                }
            }
        }
    }

    private void summonNick( GenericMessageEvent event, String nick )
    {
        event.getBot().sendIRC().invite( nick, RESTRICTED_CHANNEL );
        event.getBot().sendIRC().message( nick, "You are summoned by "
            + BaseNick.toBaseNick( event.getUser().getNick() ) + " to join " + RESTRICTED_CHANNEL );
        event.respond( nick + " was invited." );

    }

}
