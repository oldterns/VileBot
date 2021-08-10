package com.oldterns.irc.bot;

import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.helper.ActorEvent;

import java.util.Objects;
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

    public static Nick getNick(ActorEvent<User> event) {
        return getNick(event.getActor());
    }

    public static Nick getNick(User user) {
        return new Nick(user.getNick());
    }

    public static Nick valueOf(String nick) {
        return new Nick(nick);
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Nick nick1 = (Nick) o;
        return getBaseNick().equals(nick1.getBaseNick());
    }

    @Override public int hashCode() {
        return Objects.hash(getBaseNick());
    }

    @Override
    public String toString() {
        return getBaseNick();
    }
}
