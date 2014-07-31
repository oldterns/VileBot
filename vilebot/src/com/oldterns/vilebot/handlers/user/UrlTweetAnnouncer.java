/**
 * Copyright (C) 2014 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.Vilebot;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;

/**
 * Will grab the text from a tweet given the static tweet URL.
 */
@HandlerContainer
public class UrlTweetAnnouncer
{

    private static final Pattern urlPattern =
        Pattern.compile( "((?:http|https)://(?:www.|)(?:(?:twitter)\\.com)[^ ]*)" );

    private static final Pattern titlePattern = Pattern.compile( "<title>(.*)</title>" );
    private final Map<String, String> cfg = Collections.unmodifiableMap(Vilebot.getConfigMap("cfg", "twitter.conf"));
    private final String consumerKey = cfg.get("consumerKey");  //may be known as 'API key'
    private final String consumerSecret = cfg.get("consumerSecret"); //may be known as 'API secret'
    private final String accessToken = cfg.get("accessToken"); //may be known as 'Access token'
    private final String accessTokenSecret = cfg.get("accessTokenSecret"); //may be known as 'Access token secret'

    @Handler
    public void urlAnnouncer( ReceivePrivmsg event )
    {
        Matcher urlMatcher = urlPattern.matcher( event.getText() );

        if ( urlMatcher.find() )
        {
            String title = scrapeURLHTMLTitle( urlMatcher.group( 1 ) );
            event.reply( "'" + title + "'" );
        }
    }

    /**
     * Accesses the source of a HTML page and looks for a title element
     * 
     * @param url http tweet String
     * @return String of text which represents the tweet.  Empty if error.
     */
    private String scrapeURLHTMLTitle( String url )
    {
        String text = "";

        URL page;
        try
        {
            page = new URL( url );
        }
        catch ( MalformedURLException x )
        {
            // System.err.format("scrapeURLHTMLTitle new URL error: %s%n", x);
            return text;
        }

        //split the url into pieces, change the request based on what we have
        String parts[] = url.split("/");
        int userPosition = 0;
        long tweetID = 0;
        for (int i = 0; i < parts.length; i++)
        {

            if (parts[i].toString().equals("twitter.com"))
                userPosition = i+1;
            if (parts[i].toString().equals( "status" ) || parts[i].toString().equals( "statuses" ))
                tweetID = Long.valueOf(parts[i+1].toString()).longValue();
        }
        if (userPosition == 0)
            return text;
        else
        {
            try
            {
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setDebugEnabled(true)
                  .setOAuthConsumerKey(consumerKey)
                  .setOAuthConsumerSecret(consumerSecret)
                  .setOAuthAccessToken(accessToken)
                  .setOAuthAccessTokenSecret(accessTokenSecret);
                TwitterFactory tf = new TwitterFactory(cb.build());
                Twitter twitter = tf.getInstance();
                if(tweetID != 0) //tweet of the twitter.com/USERID/status/TWEETID variety
                {
                    Status status = twitter.showStatus(tweetID);
                    return (status.getUser().getName() + ": " + status.getText());
                }
                else //just the user is given, ie, twitter.com/USERID 
                {
                    User user = twitter.showUser( parts[userPosition].toString() );
                    if(!user.getDescription().isEmpty()) //the user has a description
                        return ("Name: " + user.getName() + " | " + user.getDescription() + "\'\nLast Tweet: \'" + user.getStatus().getText());
                    else //the user doesn't have a description, don't print it
                        return ("Name: " + user.getName() + "\'\nLast Tweet: \'" + user.getStatus().getText());
                    
                }
            }
            catch (TwitterException x)
            {
                return text;
            }
        }
    }
}
