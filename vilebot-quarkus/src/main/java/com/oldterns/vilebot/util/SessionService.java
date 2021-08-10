package com.oldterns.vilebot.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;

/**
 * Provides record-keeping for time limited sessions.
 */
@ApplicationScoped
public class SessionService
{

    @Inject
    TimeService timeService;

    private final int MASS_CLEANUP_THRESHOLD = 10;

    private int queryCount = 0;

    private final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    /**
     * Create a session mapping a ircNick to an internal username, with a certain expiry time.
     *
     * @param ircNick The active identification of the user on IRC
     * @param username The user name to permit them to use
     * @param sessionlength The time in milliseconds until the session expires
     */
    public void addSession( Nick ircNick, String username, Duration sessionlength )
    {
        long nowMillis = timeService.getCurrentTimeMills();
        sessions.put( ircNick.getBaseNick(), new SessionEntry( username, nowMillis + sessionlength.toMillis() ) );
    }

    private void cleanup()
    {
        if ( queryCount > MASS_CLEANUP_THRESHOLD )
        {
            queryCount = 0;
            for ( Map.Entry<String, SessionEntry> sessionEntry : sessions.entrySet() )
            {
                String nick = sessionEntry.getKey();
                SessionEntry session = sessionEntry.getValue();

                if ( session.isExpired() )
                {
                    sessions.remove( nick );
                }
            }
        }
        else
        {
            queryCount++;
        }

    }

    /**
     * Check for a valid session registered to the given nick.
     *
     * @param ircNick The active identification of the user on IRC
     * @return If a valid session exists, the username the session is for. Otherwise, null.
     */
    public String getSession( Nick ircNick )
    {
        String username = null;

        SessionEntry session = sessions.get( ircNick.getBaseNick() );

        if ( session != null )
        {
            if ( !session.isExpired() )
            {
                username = session.getUsername();
            }
            else
            {
                sessions.remove( ircNick.getBaseNick() );
            }
        }

        cleanup();

        return username;
    }

    private class SessionEntry
    {
        private final long expiryMillis;

        private final String username;

        SessionEntry( String username, long expiryMillis )
        {
            this.username = username;
            this.expiryMillis = expiryMillis;
        }

        long getExpiryMillis()
        {
            return expiryMillis;
        }

        String getUsername()
        {
            return username;
        }

        boolean isExpired()
        {
            return getExpiryMillis() < timeService.getCurrentTimeMills();
        }

        @Override
        public String toString()
        {
            return "SessionEntry [expiryMillis=" + expiryMillis + ", username=" + username + "]";
        }
    }
}
