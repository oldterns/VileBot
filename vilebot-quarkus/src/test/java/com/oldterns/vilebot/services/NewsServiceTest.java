package com.oldterns.vilebot.services;

import java.util.Optional;

import javax.inject.Inject;
import javax.naming.LimitExceededException;

import com.oldterns.vilebot.util.Colors;
import com.oldterns.vilebot.util.LimitService;
import com.oldterns.vilebot.util.TestUrlStreamHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.element.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class NewsServiceTest
{

    @Inject
    NewsService newsService;

    @Inject
    TestUrlStreamHandler urlMocker;

    @InjectMock
    LimitService limitService;

    @Test
    public void testDefaultNewsChannel()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        urlMocker.mockHttpConnection( "https://rss.cbc.ca/lineup/canada-toronto.xml",
                                      NewsServiceTest.class.getResourceAsStream( "news-rss-feed.xml" ) );
        assertNewsFeed( newsService.getNews( user, Optional.empty() ) );
    }

    @Test
    public void testTopicNewsChannel()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        urlMocker.mockHttpConnection( "http://feeds.reuters.com/Reuters/domesticNews",
                                      NewsServiceTest.class.getResourceAsStream( "news-rss-feed.xml" ) );
        assertNewsFeed( newsService.getNews( user, Optional.of( "usa" ) ) );
    }

    @Test
    public void testOverLimit()
        throws LimitExceededException
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        doThrow( new LimitExceededException( "bob at limit" ) ).when( limitService ).addUse( user );

        assertThat( newsService.getNews( user, Optional.empty() ) ).isEqualTo( "bob at limit" );
    }

    @Test
    public void testDefaultFakeNewsChannel()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        urlMocker.mockHttpConnection( "https://www.thebeaverton.com/rss",
                                      NewsServiceTest.class.getResourceAsStream( "news-rss-feed.xml" ) );
        assertNewsFeed( newsService.getFakeNews( user, Optional.empty() ) );
    }

    @Test
    public void testTopicFakeNewsChannel()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        urlMocker.mockHttpConnection( "https://www.theonion.com/rss",
                                      NewsServiceTest.class.getResourceAsStream( "news-rss-feed.xml" ) );
        assertNewsFeed( newsService.getFakeNews( user, Optional.of( "usa" ) ) );
    }

    @Test
    public void testFakeOverLimit()
        throws LimitExceededException
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        doThrow( new LimitExceededException( "bob at limit" ) ).when( limitService ).addUse( user );

        assertThat( newsService.getFakeNews( user, Optional.empty() ) ).isEqualTo( "bob at limit" );
    }

    @Test
    public void testHelp()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        newsService.newsHelp( user );
        verify( user ).sendMessage( "News Categories (example: !news toronto):" );
        verify( user ).sendMessage( "  General: { top } { world } { canada } { usa } { britain }" );
        verify( user ).sendMessage( "  Topics: { politics } { business } { health } { arts } { tech } { offbeat } { indigenous }" );
    }

    @Test
    public void testFakeNewsHelp()
    {
        User user = mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );

        newsService.fakeNewsHelp( user );
        verify( user ).sendMessage( "Fake News Categories (example: !fakenews canada):" );
        verify( user ).sendMessage( "  Countries: { canada } { usa } { belgium } { france } { india } { russia } { serbia } { venezuela } { newzealand }" );
    }

    public void assertNewsFeed( String newsFeed )
    {
        assertThat( newsFeed ).isEqualTo( Colors.bold( "  News Title 1 - A story to behold" )
            + " -> https://example.com/1\n" + Colors.bold( "  News Title 2 - A stunning sight" )
            + " -> https://example.com/2\n" + Colors.bold( "  News Title 3 - A new dawn" )
            + " -> https://example.com/3" );
    }
}
