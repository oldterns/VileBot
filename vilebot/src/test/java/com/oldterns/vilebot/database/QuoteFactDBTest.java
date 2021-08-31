package com.oldterns.vilebot.database;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.redis.client.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class QuoteFactDBTest
    extends AbstractDatabaseTest
{

    @Inject
    QuoteFactDB quoteFactDB;

    @Test
    public void testAddQuote()
    {
        quoteFactDB.addQuote( "bob", "this is a quote" );
        verify( redisClient ).sadd( List.of( "noun-quotes-bob", "this is a quote" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testAddFact()
    {
        quoteFactDB.addFact( "bob", "this is a fact" );
        verify( redisClient ).sadd( List.of( "noun-facts-bob", "this is a fact" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetQuotes()
    {
        Response smembersResponse = mockResponse( Response::stream,
                                                  Stream.of( mockResponse( Response::toString, "quote 1" ),
                                                             mockResponse( Response::toString, "quote 2" ),
                                                             mockResponse( Response::toString, "a long quote 3" ) ) );
        when( redisClient.smembers( "noun-quotes-bob" ) ).thenReturn( smembersResponse );

        assertThat( quoteFactDB.getQuotes( "bob" ) ).containsExactlyInAnyOrder( "quote 1", "quote 2",
                                                                                "a long quote 3" );

        verify( redisClient ).smembers( "noun-quotes-bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetFacts()
    {
        Response smembersResponse = mockResponse( Response::stream,
                                                  Stream.of( mockResponse( Response::toString, "fact 1" ),
                                                             mockResponse( Response::toString, "fact 2" ),
                                                             mockResponse( Response::toString, "a long fact 3" ) ) );
        when( redisClient.smembers( "noun-facts-bob" ) ).thenReturn( smembersResponse );

        assertThat( quoteFactDB.getFacts( "bob" ) ).containsExactlyInAnyOrder( "fact 1", "fact 2", "a long fact 3" );

        verify( redisClient ).smembers( "noun-facts-bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testQuotesLength()
    {
        Response scardResponse = mockResponse( Response::toLong, 5L );
        when( redisClient.scard( "noun-quotes-bob" ) ).thenReturn( scardResponse );

        assertThat( quoteFactDB.getQuotesLength( "bob" ) ).isEqualTo( 5L );

        verify( redisClient ).scard( "noun-quotes-bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testFactsLength()
    {
        Response scardResponse = mockResponse( Response::toLong, 5L );
        when( redisClient.scard( "noun-facts-bob" ) ).thenReturn( scardResponse );

        assertThat( quoteFactDB.getFactsLength( "bob" ) ).isEqualTo( 5L );

        verify( redisClient ).scard( "noun-facts-bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRandomQuoteNoQuotes()
    {
        when( redisClient.srandmember( List.of( "noun-quotes-bob" ) ) ).thenReturn( null );

        assertThat( quoteFactDB.getRandomQuote( "bob" ) ).isNull();

        verify( redisClient ).srandmember( List.of( "noun-quotes-bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRandomQuote()
    {
        Response srandmemberResponse = mockResponse( Response::toString, "a random quote" );
        when( redisClient.srandmember( List.of( "noun-quotes-bob" ) ) ).thenReturn( srandmemberResponse );

        assertThat( quoteFactDB.getRandomQuote( "bob" ) ).isEqualTo( "a random quote" );

        verify( redisClient ).srandmember( List.of( "noun-quotes-bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRandomFactNoFacts()
    {
        when( redisClient.srandmember( List.of( "noun-facts-bob" ) ) ).thenReturn( null );

        assertThat( quoteFactDB.getRandomFact( "bob" ) ).isNull();

        verify( redisClient ).srandmember( List.of( "noun-facts-bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRandomFact()
    {
        Response srandmemberResponse = mockResponse( Response::toString, "a random fact" );
        when( redisClient.srandmember( List.of( "noun-facts-bob" ) ) ).thenReturn( srandmemberResponse );

        assertThat( quoteFactDB.getRandomFact( "bob" ) ).isEqualTo( "a random fact" );

        verify( redisClient ).srandmember( List.of( "noun-facts-bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRandomQuotes()
    {
        Response srandmemberResponse1 = mockResponse( Response::toString, "a random quote 1" );
        Response srandmemberResponse2 = mockResponse( Response::toString, "a random quote 2" );
        Response srandmemberResponse3 = mockResponse( Response::toString, "a random quote 3" );
        Response srandmemberResponse4 = mockResponse( Response::toString, "a random quote 4" );
        Response srandmemberResponse5 = mockResponse( Response::toString, "a random quote 5" );

        when( redisClient.srandmember( List.of( "noun-quotes-bob" ) ) ).thenReturn( srandmemberResponse1 ).thenReturn( srandmemberResponse2 ).thenReturn( srandmemberResponse3 ).thenReturn( srandmemberResponse4 ).thenReturn( srandmemberResponse5 );

        assertThat( quoteFactDB.getRandomQuotes( "bob" ) ).containsExactly( "a random quote 1", "a random quote 2",
                                                                            "a random quote 3", "a random quote 4",
                                                                            "a random quote 5" );

        verify( redisClient, times( 5 ) ).srandmember( List.of( "noun-quotes-bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRandomFacts()
    {
        Response srandmemberResponse1 = mockResponse( Response::toString, "a random fact 1" );
        Response srandmemberResponse2 = mockResponse( Response::toString, "a random fact 2" );
        Response srandmemberResponse3 = mockResponse( Response::toString, "a random fact 3" );
        Response srandmemberResponse4 = mockResponse( Response::toString, "a random fact 4" );
        Response srandmemberResponse5 = mockResponse( Response::toString, "a random fact 5" );

        when( redisClient.srandmember( List.of( "noun-facts-bob" ) ) ).thenReturn( srandmemberResponse1 ).thenReturn( srandmemberResponse2 ).thenReturn( srandmemberResponse3 ).thenReturn( srandmemberResponse4 ).thenReturn( srandmemberResponse5 );

        assertThat( quoteFactDB.getRandomFacts( "bob" ) ).containsExactly( "a random fact 1", "a random fact 2",
                                                                           "a random fact 3", "a random fact 4",
                                                                           "a random fact 5" );

        verify( redisClient, times( 5 ) ).srandmember( List.of( "noun-facts-bob" ) );
        verifyNoMoreInteractions( redisClient );
    }
}
