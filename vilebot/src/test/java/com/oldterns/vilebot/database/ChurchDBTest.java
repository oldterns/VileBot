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
public class ChurchDBTest
    extends AbstractDatabaseTest
{

    @Inject
    ChurchDB churchDB;

    @Test
    public void testModDonorPositive()
    {
        churchDB.modDonorKarma( "bob", 10 );
        verify( redisClient ).zincrby( "church-donor-karma", "10", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testModDonorNegative()
    {
        churchDB.modDonorKarma( "bob", -10 );
        verify( redisClient ).zincrby( "church-donor-karma", "-10", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testModDonorTitleDoesNotExist()
    {
        Response scardResponse = mockResponse( Response::toLong, 0L );
        when( redisClient.scard( "church-title-bob" ) ).thenReturn( scardResponse );
        churchDB.modDonorTitle( "bob", "bob the king" );

        verify( redisClient ).scard( "church-title-bob" );
        verify( redisClient ).sadd( List.of( "church-title-bob", "bob the king" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testModDonorTitleExist()
    {
        Response scardResponse = mockResponse( Response::toLong, 1L );
        when( redisClient.scard( "church-title-bob" ) ).thenReturn( scardResponse );
        churchDB.modDonorTitle( "bob", "bob the king" );

        verify( redisClient ).scard( "church-title-bob" );
        verify( redisClient ).spop( List.of( "church-title-bob" ) );
        verify( redisClient ).sadd( List.of( "church-title-bob", "bob the king" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetDonorTitle()
    {
        Response titleResponse = mockResponse( Response::toString, "bob the king" );
        when( redisClient.srandmember( List.of( "church-title-bob" ) ) ).thenReturn( titleResponse );
        assertThat( churchDB.getDonorTitle( "bob" ) ).isEqualTo( "bob the king" );

        verify( redisClient ).srandmember( List.of( "church-title-bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testModNonDonorPositive()
    {
        churchDB.modNonDonorKarma( 10 );
        verify( redisClient ).zincrby( "church-karma", "10", "church-karma" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testModNonDonorNegative()
    {
        churchDB.modNonDonorKarma( -10 );
        verify( redisClient ).zincrby( "church-karma", "-10", "church-karma" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetDonorKarmaDoesNotExist()
    {
        when( redisClient.zscore( "church-donor-karma", "bob" ) ).thenReturn( null );
        assertThat( churchDB.getDonorKarma( "bob" ) ).isEmpty();
        verify( redisClient ).zscore( "church-donor-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetDonorKarma()
    {
        Response response = mockResponse( Response::toLong, 10L );
        when( redisClient.zscore( "church-donor-karma", "bob" ) ).thenReturn( response );
        assertThat( churchDB.getDonorKarma( "bob" ) ).hasValue( 10L );
        verify( redisClient ).zscore( "church-donor-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetDonorRankDoesNotExist()
    {
        when( redisClient.zrevrank( "church-donor-karma", "bob" ) ).thenReturn( null );
        assertThat( churchDB.getDonorRank( "bob" ) ).isEmpty();
        verify( redisClient ).zrevrank( "church-donor-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetDonorRank()
    {
        Response response = mockResponse( Response::toLong, 10L );
        when( redisClient.zrevrank( "church-donor-karma", "bob" ) ).thenReturn( response );
        assertThat( churchDB.getDonorRank( "bob" ) ).hasValue( 11L );
        verify( redisClient ).zrevrank( "church-donor-karma", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetDonorRankNouns()
    {
        Response response = mockResponse( Response::stream,
                                          Stream.of( mockResponse( Response::toString, "alice" ),
                                                     mockResponse( Response::toString, "bob" ),
                                                     mockResponse( Response::toString, "charlie" ) ) );
        when( redisClient.zrevrange( List.of( "church-donor-karma", "10", "13" ) ) ).thenReturn( response );
        assertThat( churchDB.getDonorsByRanks( 10L, 13L ) ).containsExactlyInAnyOrder( "alice", "bob", "charlie" );
        verify( redisClient ).zrevrange( List.of( "church-donor-karma", "10", "13" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRemDonorNotExist()
    {
        Response response = mockResponse( Response::toLong, 0L );
        when( redisClient.zrem( List.of( "church-donor-karma", "bob" ) ) ).thenReturn( response );
        assertThat( churchDB.removeDonor( "bob" ) ).isFalse();
        verify( redisClient ).zrem( List.of( "church-donor-karma", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRemDonorExist()
    {
        Response response = mockResponse( Response::toLong, 1L );
        when( redisClient.zrem( List.of( "church-donor-karma", "bob" ) ) ).thenReturn( response );
        assertThat( churchDB.removeDonor( "bob" ) ).isTrue();
        verify( redisClient ).zrem( List.of( "church-donor-karma", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetTotalDonations()
    {
        Response response = mockResponse( Response::stream,
                                          Stream.of( mockResponse( Response::toString, "alice" ),
                                                     mockResponse( Response::toString, "bob" ),
                                                     mockResponse( Response::toString, "charlie" ) ) );
        Response aliceKarma = mockResponse( Response::toLong, 10L );
        Response bobKarma = mockResponse( Response::toLong, 10L );
        Response charlieKarma = mockResponse( Response::toLong, 5L );

        when( redisClient.zrange( List.of( "church-donor-karma", "0", "-1" ) ) ).thenReturn( response );
        when( redisClient.zscore( "church-donor-karma", "alice" ) ).thenReturn( aliceKarma );
        when( redisClient.zscore( "church-donor-karma", "bob" ) ).thenReturn( bobKarma );
        when( redisClient.zscore( "church-donor-karma", "charlie" ) ).thenReturn( charlieKarma );

        assertThat( churchDB.getTotalDonations() ).isEqualTo( 25L );
        verify( redisClient ).zrange( List.of( "church-donor-karma", "0", "-1" ) );
        verify( redisClient ).zscore( "church-donor-karma", "alice" );
        verify( redisClient ).zscore( "church-donor-karma", "bob" );
        verify( redisClient ).zscore( "church-donor-karma", "charlie" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetTotalNonDonations()
    {
        Response response =
            mockResponse( Response::stream, Stream.of( mockResponse( Response::toString, "church-karma" ) ) );
        Response churchKarma = mockResponse( Response::toLong, 10L );

        when( redisClient.zrange( List.of( "church-karma", "0", "-1" ) ) ).thenReturn( response );
        when( redisClient.zscore( "church-karma", "church-karma" ) ).thenReturn( churchKarma );

        assertThat( churchDB.getTotalNonDonations() ).isEqualTo( 10L );
        verify( redisClient ).zrange( List.of( "church-karma", "0", "-1" ) );
        verify( redisClient ).zscore( "church-karma", "church-karma" );
        verifyNoMoreInteractions( redisClient );
    }
}
