package com.oldterns.vilebot.util;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

public interface TwitterService
{
    Status getStatus( long tweetId )
        throws TwitterException;

    User getUser( String userId )
        throws TwitterException;
}
