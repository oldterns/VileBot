package com.oldterns.vilebot.util;

import com.oldterns.vilebot.Nick;
import org.kitteh.irc.client.library.element.User;

import javax.naming.LimitExceededException;
import java.time.Duration;

public interface LimitService {
    void setLimit( int maxUsesPerPeriod, Duration timePeriod );

    void addUse(String noun) throws LimitExceededException;

    default void addUse(Nick nick) throws LimitExceededException {
        addUse(nick.getBaseNick());
    }

    default void addUse(User user) throws LimitExceededException {
        addUse(Nick.getNick(user));
    }

    boolean isAtLimit(String noun);

    default boolean isAtLimit(Nick nick) {
        return isAtLimit(nick.getBaseNick());
    }

    default boolean isAtLimit(User user) {
        return isAtLimit(Nick.getNick(user));
    }
}
