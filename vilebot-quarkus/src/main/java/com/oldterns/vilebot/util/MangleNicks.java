package com.oldterns.vilebot.util;

import com.oldterns.vilebot.Nick;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class MangleNicks
{

    public static String mangleNicks( ChannelEvent event, String message )
    {
        return mangleNicks( event.getChannel().getUsers().stream().map( Nick::getNick ).map( Nick::getFullNick ).collect( Collectors.toList() ),
                            message );
    }

    public static String mangleNicks( ActorEvent<User> event, String message )
    {
        // Delegate to Channel if possible
        if ( event instanceof ChannelEvent )
        {
            return mangleNicks( (ChannelEvent) event, message );
        }
        return mangleNicks( Collections.singleton( Objects.requireNonNull( event.getActor() ).getNick() ), message );
    }

    private static String mangleNicks( Collection<String> nicks, String message )
    {

        if ( nicks.isEmpty() )
        {
            return message;
        }

        StringBuilder reply = new StringBuilder();
        for ( String word : message.split( " " ) )
        {
            reply.append( " " );
            reply.append( inside( nicks, word ) ? mangled( word ) : word );
        }
        return reply.toString().trim();
    }

    private static String mangled( String word )
    {
        return new StringBuilder( word ).reverse().toString();
    }

    private static boolean inside( Collection<String> nicks, String word )
    {
        for ( String nick : nicks )
        {
            if ( word.contains( nick ) )
            {
                return true;
            }
        }
        return false;
    }
}
