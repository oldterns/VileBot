package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.Regex;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kitteh.irc.client.library.Client;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

import javax.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

@ApplicationScoped
public class UrlTweetAnnouncerService
{
    @ConfigProperty( name = "vilebot.twitter.consumer-key" )
    Optional<String> consumerKey; // may be known as 'API key'

    @ConfigProperty( name = "vilebot.twitter.consumer-secret" )
    Optional<String> consumerSecret; // may be known as 'API secret'

    @ConfigProperty( name = "vilebot.twitter.access-token" )
    Optional<String> accessToken; // may be known as 'Access token'

    @ConfigProperty( name = "vilebot.twitter.access-token-secret" )
    Optional<String> accessTokenSecret; // may be known as 'Access token secret'

    @OnChannelMessage( "@message" )
    public String onTwitterUrlMessage( Client client,
                                       @Regex( "((?:http|https)://(?:www.|)(?:(?:twitter)\\.com)[^ ]*)" ) String message )
    {
        if ( consumerKey.isEmpty() || consumerSecret.isEmpty() || accessToken.isEmpty() || accessTokenSecret.isEmpty() )
        {
            return "Sorry, I can't read that tweet because my maintainer is a moron. And I wouldn't want to read it, anyway.";
        }

        String title = scrapeURLHTMLTitle( client, message );
        return "' " + title + " '";
    }

    /**
     * Accesses the source of a HTML page and looks for a title element
     *
     * @param url http tweet String
     * @return String of text which represents the tweet. Empty if error.
     */
    private String scrapeURLHTMLTitle( Client client, String url )
    {
        String text = "";

        try
        {
            new URL( url );
        }
        catch ( MalformedURLException x )
        {
            // System.err.format("scrapeURLHTMLTitle new URL error: %s%n", x);
            return text;
        }

        // split the url into pieces, change the request based on what we have
        String[] parts = url.split( "/" );
        int userPosition = 0;
        long tweetID = 0;
        for ( int i = 0; i < parts.length; i++ )
        {
            if ( parts[i].equals( "twitter.com" ) )
                userPosition = i + 1;
            if ( parts[i].equals( "status" ) || parts[i].equals( "statuses" ) )
                tweetID = Long.valueOf( parts[i + 1] );
        }
        if ( userPosition == 0 )
            return text;
        else
        {
            try
            {
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setDebugEnabled( true ).setOAuthConsumerKey( consumerKey.get() ).setOAuthConsumerSecret( consumerSecret.get() ).setOAuthAccessToken( accessToken.get() ).setOAuthAccessTokenSecret( accessTokenSecret.get() );
                TwitterFactory tf = new TwitterFactory( cb.build() );
                Twitter twitter = tf.getInstance();
                if ( tweetID != 0 ) // tweet of the twitter.com/USERID/status/TWEETID variety
                {
                    Status status = twitter.showStatus( tweetID );
                    return ( status.getUser().getName() + ": " + status.getText() );
                }
                else // just the user is given, ie, twitter.com/USERID
                {
                    User user = twitter.showUser( parts[userPosition].toString() );
                    if ( !user.getDescription().isEmpty() ) // the user has a description
                        return ( "Name: " + user.getName() + " | " + user.getDescription() + "\'\nLast Tweet: \'"
                            + user.getStatus().getText() );
                    else // the user doesn't have a description, don't print it
                        return ( "Name: " + user.getName() + "\'\nLast Tweet: \'" + user.getStatus().getText() );

                }
            }
            catch ( TwitterException x )
            {
                return client.getNick()
                    + ": Until my maintainer fixes the API Key, this is the only tweet you're gonna see. U mad, bro?";
            }
        }
    }
}
