package com.oldterns.vilebot;

import org.apache.camel.component.irc.IrcMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Nick {
    public static final String regex =  "(?:x|)([a-zA-Z0-9]+)(?:\\p{Punct}+[a-zA-Z0-9]*|)";
    final String nick;
    private static final Pattern nickPattern = Pattern.compile( regex );

    public Nick(String nick) {
        this.nick = nick;
    }

    public String getBaseNick() {
        Matcher nickMatcher = nickPattern.matcher( nick );
        if ( nickMatcher.find() )
        {
            return nickMatcher.group( 1 );
        }
        return nick;
    }

    public String getFullNick() {
        return nick;
    }

    public static Nick getUser(IrcMessage ircMessage) {
        return new Nick((String) ircMessage.getHeader("irc.user.nick"));
    }

    public static Nick valueOf(String nick) {
        return new Nick(nick);
    }
}
