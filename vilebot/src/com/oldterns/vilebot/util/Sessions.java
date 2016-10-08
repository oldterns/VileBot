package com.oldterns.vilebot.util;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides record-keeping for time limited sessions.
 */
public class Sessions {
    private static class SessionEntry {
        private final long expiryMillis;

        private final String username;

        public SessionEntry(String username, long expiryMillis) {
            this.username = username;
            this.expiryMillis = expiryMillis;
        }

        public long getExpiryMillis() {
            return expiryMillis;
        }

        public String getUsername() {
            return username;
        }

        public boolean isExpired() {
            return getExpiryMillis() < System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "SessionEntry [expiryMillis=" + expiryMillis + ", username=" + username + "]";
        }
    }

    private static final int MASS_CLEANUP_THRESHOLD = 10;

    private static int queryCount = 0;

    private static final ConcurrentHashMap<String, SessionEntry> sessions = new ConcurrentHashMap<String, SessionEntry>();

    /**
     * Create a session mapping a ircNick to an internal username, with a certain expiry time.
     * 
     * @param ircNick The active identification of the user on IRC
     * @param username The user name to permit them to use
     * @param sessionlength The time in milliseconds until the session expires
     */
    public static void addSession(String ircNick, String username, long sessionlength) {
        long nowMillis = System.currentTimeMillis();
        sessions.put(ircNick, new SessionEntry(username, nowMillis + sessionlength));
    }

    private static void cleanup() {
        if (queryCount > MASS_CLEANUP_THRESHOLD) {
            queryCount = 0;
            for (Entry<String, SessionEntry> sessionEntry : sessions.entrySet()) {
                String nick = sessionEntry.getKey();
                SessionEntry session = sessionEntry.getValue();
                if (session.isExpired()) {
                    sessions.remove(nick);
                }
            }
        } else {
            queryCount++;
        }
    }

    /**
     * Check for a valid session registered to the given nick.
     * 
     * @param ircNick The active identification of the user on IRC
     * @return If a valid session exists, the username the session is for. Otherwise, null.
     */
    public static String getSession(String ircNick) {
        String username = null;
        SessionEntry session = sessions.get(ircNick);
        if (session != null) {
            if (!session.isExpired()) {
                username = session.getUsername();
            } else {
                sessions.remove(ircNick);
            }
        }
        cleanup();
        return username;
    }
}
