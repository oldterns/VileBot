package com.oldterns.vilebot.util;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

@ApplicationScoped
public class TwitterServiceImpl
    implements TwitterService
{
    @ConfigProperty( name = "vilebot.twitter.consumer-key" )
    Optional<String> consumerKey; // may be known as 'API key'

    @ConfigProperty( name = "vilebot.twitter.consumer-secret" )
    Optional<String> consumerSecret; // may be known as 'API secret'

    @ConfigProperty( name = "vilebot.twitter.access-token" )
    Optional<String> accessToken; // may be known as 'Access token'

    @ConfigProperty( name = "vilebot.twitter.access-token-secret" )
    Optional<String> accessTokenSecret; // may be known as 'Access token secret'

    Twitter twitter;

    @PostConstruct
    public void setupTwitter()
    {
        if ( consumerKey.isPresent() && consumerSecret.isPresent() && accessToken.isPresent()
            && accessTokenSecret.isPresent() )
        {
            try
            {
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setDebugEnabled( true ).setOAuthConsumerKey( consumerKey.get() ).setOAuthConsumerSecret( consumerSecret.get() ).setOAuthAccessToken( accessToken.get() ).setOAuthAccessTokenSecret( accessTokenSecret.get() );
                TwitterFactory tf = new TwitterFactory( cb.build() );
                twitter = tf.getInstance();
            }
            catch ( Exception ignored )
            {

            }
        }
    }

    @Override
    public Status getStatus( long tweetId )
        throws TwitterException
    {
        if ( twitter != null )
        {
            return twitter.showStatus( tweetId );
        }
        else
        {
            throw new TwitterException( "API Keys invalid or missing" );
        }
    }

    @Override
    public User getUser( String userId )
        throws TwitterException
    {
        if ( twitter != null )
        {
            return twitter.showUser( userId );
        }
        else
        {
            throw new TwitterException( "API Keys invalid or missing" );
        }
    }
}
