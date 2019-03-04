package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.misc.Colors;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LastMessageSed
    extends ListenerAdapter
{

    /**
     * Map with String key of IRC nick, to String value of the last line of text.
     */
    private final Map<String, String> lastMessageMapByNick = new HashMap<>();

    /**
     * Syncronise access to lastMessageMapByNick on this.
     */
    private final Object lastMessageMapByNickMutex = new Object();

    /**
     * Matches a standard sed-like replace pattern (ex: s/foo/bar/), the forward slash divisor can be replaced with any
     * punctuation character. The given separator character cannot be used elsewhere. Optionally, following the last
     * slash, a user can write another user's name in order to modify that user's message.
     */
    private static final Pattern replacePattern =
        Pattern.compile( "^s(\\p{Punct})((?!\\1).+?)\\1((?!\\1).+?)(?:\\1(g|)(?:\\s+(\\S+)\\s*|)|)$" );

    /**
     * Say the last thing the person said, replaced as specified. Otherwise just record the line as the last thing the
     * person said.
     */
    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher sedMatcher = replacePattern.matcher( text );

        String nick = event.getUser().getNick();

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

                synchronized ( lastMessageMapByNickMutex )
                {
                    String lastMessage = lastMessageMapByNick.get( nick );

                    if ( !lastMessage.contains( regexp ) )
                    {
                        event.respondWith( "Wow. Seriously? Try subbing out a string that actually occurred. Do you even sed, bro?" );
                    }
                    else
                    {
                        String replacedMsg;
                        String replacedMsgWHL;

                        String replacementWHL = Colors.bold( replacement );

                        // TODO: Probably can be simplified via method reference in Java 8
                        if ( "g".equals( endFlag ) )
                        {
                            replacedMsg = lastMessage.replaceAll( regexp, replacement );
                            replacedMsgWHL = lastMessage.replaceAll( regexp, replacementWHL );
                        }
                        else
                        {
                            replacedMsg = lastMessage.replaceFirst( regexp, replacement );
                            replacedMsgWHL = lastMessage.replaceFirst( regexp, replacementWHL );
                        }

                        event.respondWith( correction + replacedMsgWHL );
                        lastMessageMapByNick.put( nick, replacedMsg );
                    }
                }
            }
        }
        else
        {
            synchronized ( lastMessageMapByNickMutex )
            {
                lastMessageMapByNick.put( nick, text );
            }
        }
    }
}
