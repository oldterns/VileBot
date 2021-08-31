package com.oldterns.vilebot.services;

import java.io.IOException;

import javax.inject.Inject;

import com.oldterns.irc.bot.services.ClientCreator;
import com.oldterns.vilebot.util.TestUrlStreamHandler;
import com.oldterns.vilebot.util.TwitterService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.mockito.Mockito;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class UrlTweetAnnouncerServiceTest
{

    @Inject
    UrlTweetAnnouncerService urlTweetAnnouncerService;

    @InjectMock
    TwitterService twitterService;

    @Inject
    ClientCreator clientCreator;

    @Test
    public void testGetTweet()
        throws IOException, TwitterException
    {
        Client client = clientCreator.createClient( "MyClient" );
        Status status = Mockito.mock( Status.class );
        User user = Mockito.mock( User.class );

        Mockito.when( user.getName() ).thenReturn( "Example User" );
        Mockito.when( status.getUser() ).thenReturn( user );
        Mockito.when( status.getText() ).thenReturn( "This is a tweet" );

        Mockito.when( twitterService.getStatus( 1 ) ).thenReturn( status );
        assertThat( urlTweetAnnouncerService.onTwitterUrlMessage( client,
                                                                  "https://twitter.com/ExampleUser/status/1" ) ).isEqualTo( "' Example User: This is a tweet '" );
    }

    @Test
    public void testGetUserEmptyDescription()
        throws IOException, TwitterException
    {
        Client client = clientCreator.createClient( "MyClient" );
        Status status = Mockito.mock( Status.class );
        User user = Mockito.mock( User.class );

        Mockito.when( user.getName() ).thenReturn( "Example User" );
        Mockito.when( user.getDescription() ).thenReturn( "" );
        Mockito.when( status.getUser() ).thenReturn( user );
        Mockito.when( status.getText() ).thenReturn( "My Last Tweet" );
        Mockito.when( user.getStatus() ).thenReturn( status );

        Mockito.when( twitterService.getUser( "ExampleUser" ) ).thenReturn( user );
        assertThat( urlTweetAnnouncerService.onTwitterUrlMessage( client,
                                                                  "https://twitter.com/ExampleUser" ) ).isEqualTo( "' Name: Example User'\n"
                                                                      + "Last Tweet: 'My Last Tweet '" );
    }

    @Test
    public void testGetUserWithDescription()
        throws IOException, TwitterException
    {
        Client client = clientCreator.createClient( "MyClient" );
        Status status = Mockito.mock( Status.class );
        User user = Mockito.mock( User.class );

        Mockito.when( user.getName() ).thenReturn( "Example User" );
        Mockito.when( user.getDescription() ).thenReturn( "My Description" );
        Mockito.when( status.getUser() ).thenReturn( user );
        Mockito.when( status.getText() ).thenReturn( "My Last Tweet" );
        Mockito.when( user.getStatus() ).thenReturn( status );

        Mockito.when( twitterService.getUser( "ExampleUser" ) ).thenReturn( user );
        assertThat( urlTweetAnnouncerService.onTwitterUrlMessage( client,
                                                                  "https://twitter.com/ExampleUser" ) ).isEqualTo( "' Name: Example User | My Description'\n"
                                                                      + "Last Tweet: 'My Last Tweet '" );
    }

    @Test
    public void testTwitterException()
        throws IOException, TwitterException
    {
        Client client = clientCreator.createClient( "MyClient" );
        Status status = Mockito.mock( Status.class );
        User user = Mockito.mock( User.class );

        Mockito.when( user.getName() ).thenReturn( "Example User" );
        Mockito.when( user.getDescription() ).thenReturn( "My Description" );
        Mockito.when( status.getUser() ).thenReturn( user );
        Mockito.when( status.getText() ).thenReturn( "My Last Tweet" );
        Mockito.when( user.getStatus() ).thenReturn( status );

        Mockito.when( twitterService.getUser( "ExampleUser" ) ).thenThrow( new TwitterException( "Invalid API Keys" ) );
        assertThat( urlTweetAnnouncerService.onTwitterUrlMessage( client,
                                                                  "https://twitter.com/ExampleUser" ) ).isEqualTo( "' MyClient: Until my maintainer fixes the API Key, this is the only tweet you're gonna see. U mad, bro? '" );
    }
}
