package com.oldterns.vilebot.database;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.redis.client.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class KarmaDBTest
    extends AbstractDatabaseTest
{

    @Inject
    KarmaDB karmaDB;

    @Test
    public void testModKarmaPositive()
    {
        karmaDB.modNounKarma( "bob", 10 );
        verify( redisClient ).zincrby( "noun-karma", "10", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testModKarmaNegative()
    {
        karmaDB.modNounKarma( "bob", -10 );
        verify( redisClient ).zincrby( "noun-karma", "-10", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetKarmaDoesNotExist()
    {
        when( redisClient.zscore( "noun-karma", "bob" ) ).thenReturn( null );
        assertThat( karmaDB.getNounKarma( "bob" ) ).isEmpty();
        verify( redisClient ).zscore( "noun-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetKarma()
    {
        Response response = mockResponse( Response::toLong, 10L );
        when( redisClient.zscore( "noun-karma", "bob" ) ).thenReturn( response );
        assertThat( karmaDB.getNounKarma( "bob" ) ).hasValue( 10L );
        verify( redisClient ).zscore( "noun-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRankDoesNotExist()
    {
        when( redisClient.zrevrank( "noun-karma", "bob" ) ).thenReturn( null );
        assertThat( karmaDB.getNounRank( "bob" ) ).isEmpty();
        verify( redisClient ).zrevrank( "noun-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRank()
    {
        Response response = mockResponse( Response::toLong, 10L );
        when( redisClient.zrevrank( "noun-karma", "bob" ) ).thenReturn( response );
        assertThat( karmaDB.getNounRank( "bob" ) ).hasValue( 11L );
        verify( redisClient ).zrevrank( "noun-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRevRankDoesNotExist()
    {
        when( redisClient.zrank( "noun-karma", "bob" ) ).thenReturn( null );
        assertThat( karmaDB.getNounRevRank( "bob" ) ).isEmpty();
        verify( redisClient ).zrank( "noun-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRevRank()
    {
        Response response = mockResponse( Response::toLong, 10L );
        when( redisClient.zrank( "noun-karma", "bob" ) ).thenReturn( response );
        assertThat( karmaDB.getNounRevRank( "bob" ) ).hasValue( 11L );
        verify( redisClient ).zrank( "noun-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRankNounDoesNotExist()
    {
        Response response = mockResponse( Response::stream, Stream.empty() );
        when( redisClient.zrevrange( List.of( "noun-karma", "9", "10" ) ) ).thenReturn( response );
        assertThat( karmaDB.getRankNoun( 10L ) ).isEqualTo( null );
        verify( redisClient ).zrevrange( List.of( "noun-karma", "9", "10" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRankNoun()
    {
        Response response = mockResponse( Response::stream, Stream.of( mockResponse( Response::toString, "bob" ) ) );
        when( redisClient.zrevrange( List.of( "noun-karma", "9", "10" ) ) ).thenReturn( response );
        assertThat( karmaDB.getRankNoun( 10L ) ).isEqualTo( "bob" );
        verify( redisClient ).zrevrange( List.of( "noun-karma", "9", "10" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRevRankNounDoesNotExist()
    {
        Response response = mockResponse( Response::stream, Stream.empty() );
        when( redisClient.zrange( List.of( "noun-karma", "9", "10" ) ) ).thenReturn( response );
        assertThat( karmaDB.getRevRankNoun( 10L ) ).isEqualTo( null );
        verify( redisClient ).zrange( List.of( "noun-karma", "9", "10" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRevRankNoun()
    {
        Response response = mockResponse( Response::stream, Stream.of( mockResponse( Response::toString, "bob" ) ) );
        when( redisClient.zrange( List.of( "noun-karma", "9", "10" ) ) ).thenReturn( response );
        assertThat( karmaDB.getRevRankNoun( 10L ) ).isEqualTo( "bob" );
        verify( redisClient ).zrange( List.of( "noun-karma", "9", "10" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRankNouns()
    {
        Response response = mockResponse( Response::stream,
                                          Stream.of( mockResponse( Response::toString, "alice" ),
                                                     mockResponse( Response::toString, "bob" ),
                                                     mockResponse( Response::toString, "charlie" ) ) );
        when( redisClient.zrevrange( List.of( "noun-karma", "10", "13" ) ) ).thenReturn( response );
        assertThat( karmaDB.getRankNouns( 10L, 13L ) ).containsExactlyInAnyOrder( "alice", "bob", "charlie" );
        verify( redisClient ).zrevrange( List.of( "noun-karma", "10", "13" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetRevRankNouns()
    {
        Response response = mockResponse( Response::stream,
                                          Stream.of( mockResponse( Response::toString, "alice" ),
                                                     mockResponse( Response::toString, "bob" ),
                                                     mockResponse( Response::toString, "charlie" ) ) );
        when( redisClient.zrange( List.of( "noun-karma", "10", "13" ) ) ).thenReturn( response );
        assertThat( karmaDB.getRevRankNouns( 10L, 13L ) ).containsExactlyInAnyOrder( "alice", "bob", "charlie" );
        verify( redisClient ).zrange( List.of( "noun-karma", "10", "13" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRemNounNotExist()
    {
        Response response = mockResponse( Response::toLong, 0L );
        when( redisClient.zrem( List.of( "noun-karma", "bob" ) ) ).thenReturn( response );
        assertThat( karmaDB.remNoun( "bob" ) ).isFalse();
        verify( redisClient ).zrem( List.of( "noun-karma", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRemNounExist()
    {
        Response response = mockResponse( Response::toLong, 1L );
        when( redisClient.zrem( List.of( "noun-karma", "bob" ) ) ).thenReturn( response );
        assertThat( karmaDB.remNoun( "bob" ) ).isTrue();
        verify( redisClient ).zrem( List.of( "noun-karma", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetTotalKarma()
    {
        Response response = mockResponse( Response::stream,
                                          Stream.of( mockResponse( Response::toString, "alice" ),
                                                     mockResponse( Response::toString, "bob" ),
                                                     mockResponse( Response::toString, "charlie" ) ) );
        Response aliceKarma = mockResponse( Response::toLong, 10L );
        Response bobKarma = mockResponse( Response::toLong, 10L );
        Response charlieKarma = mockResponse( Response::toLong, -5L );

        when( redisClient.zrange( List.of( "noun-karma", "0", "-1" ) ) ).thenReturn( response );
        when( redisClient.zscore( "noun-karma", "alice" ) ).thenReturn( aliceKarma );
        when( redisClient.zscore( "noun-karma", "bob" ) ).thenReturn( bobKarma );
        when( redisClient.zscore( "noun-karma", "charlie" ) ).thenReturn( charlieKarma );

        assertThat( karmaDB.getTotalKarma() ).isEqualTo( 15L );
        verify( redisClient ).zrange( List.of( "noun-karma", "0", "-1" ) );
        verify( redisClient ).zscore( "noun-karma", "alice" );
        verify( redisClient ).zscore( "noun-karma", "bob" );
        verify( redisClient ).zscore( "noun-karma", "charlie" );
        verifyNoMoreInteractions( redisClient );
    }
}
