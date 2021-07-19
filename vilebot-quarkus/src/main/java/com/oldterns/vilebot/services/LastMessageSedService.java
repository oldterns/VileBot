package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.util.Colors;
import org.kitteh.irc.client.library.element.User;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class LastMessageSedService
{
    /**
     * Map with String key of IRC nick, to String value of the last line of text.
     */
    private final ConcurrentHashMap<String, String> lastMessageMapByNick = new ConcurrentHashMap<>();

    /**
     * Matches a standard sed-like replace pattern (ex: s/foo/bar/), the forward slash divisor can be replaced with any
     * punctuation character. The given separator character cannot be used elsewhere. Optionally, following the last
     * slash, a user can write another user's name in order to modify that user's message.
     */
    private static final Pattern replacePattern =
        Pattern.compile( "^s(\\p{Punct})((?!\\1).+?)\\1((?!\\1).+?)(?:\\1(g|)(?:\\s+(\\S+)\\s*|)|)$" );

    @OnChannelMessage( "@message" )
    public String onMessage( User user, String message )
    {
        Matcher sedMatcher = replacePattern.matcher( message );
        String nick = Nick.getNick( user ).getBaseNick();
        if ( sedMatcher.matches() )
        {
            String correction = "Correction: ";

            // If the last group of the regex captures a non-null string, the user is fixing another user's message.
            if ( sedMatcher.group( 5 ) != null )
            {
                nick = sedMatcher.group( 5 );
                correction = nick + ", ftfy: ";
            }

            if ( lastMessageMapByNick.containsKey( nick ) )
            {
                String regexp = sedMatcher.group( 2 );
                String replacement = sedMatcher.group( 3 );
                String endFlag = sedMatcher.group( 4 );

                String lastMessage = lastMessageMapByNick.get( nick );

                if ( !lastMessage.contains( regexp ) )
                {
                    return "Wow. Seriously? Try subbing out a string that actually occurred. Do you even sed, bro?";
                }
                else
                {
                    String replacedMsg;
                    String replacedMsgWHL;

                    String replacementWHL = Colors.bold( replacement );

                    BiFunction<String, String, String> replaceFunction =
                        ( "g".equals( endFlag ) ) ? lastMessage::replaceAll : lastMessage::replaceFirst;
                    replacedMsg = replaceFunction.apply( regexp, replacement );
                    replacedMsgWHL = replaceFunction.apply( regexp, replacementWHL );

                    lastMessageMapByNick.put( nick, replacedMsg );
                    return correction + replacedMsgWHL;
                }
            }
            return null;
        }
        else
        {
            lastMessageMapByNick.put( nick, message );
            return null;
        }
    }
}
