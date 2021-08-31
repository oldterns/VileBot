package com.oldterns.vilebot.database;

import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.redis.client.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class GroupDBTest
    extends AbstractDatabaseTest
{
    @Inject
    GroupDB groupDB;

    @Test
    public void testNoAdminsIsTrueIfNoAdmins()
    {
        Response scardResponse = mockResponse( Response::toLong, 0L );
        when( redisClient.scard( "groups-admins" ) ).thenReturn( scardResponse );

        assertThat( groupDB.noAdmins() ).isTrue();
        verify( redisClient ).scard( "groups-admins" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testNoAdminsIsFalseIfAdmins()
    {
        Response scardResponse = mockResponse( Response::toLong, 1L );
        when( redisClient.scard( "groups-admins" ) ).thenReturn( scardResponse );

        assertThat( groupDB.noAdmins() ).isFalse();
        verify( redisClient ).scard( "groups-admins" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testNullIsNotAdmin()
    {
        assertThat( groupDB.isAdmin( null ) ).isFalse();
        verifyNoInteractions( redisClient );
    }

    @Test
    public void testNonAdminIsNotAdmin()
    {
        Response sisMemberResponse = mockResponse( Response::toBoolean, false );
        when( redisClient.sismember( "groups-admins", "bob" ) ).thenReturn( sisMemberResponse );
        assertThat( groupDB.isAdmin( "bob" ) ).isFalse();

        verify( redisClient ).sismember( "groups-admins", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testAdminIsAdmin()
    {
        Response sisMemberResponse = mockResponse( Response::toBoolean, true );
        when( redisClient.sismember( "groups-admins", "bob" ) ).thenReturn( sisMemberResponse );
        assertThat( groupDB.isAdmin( "bob" ) ).isTrue();

        verify( redisClient ).sismember( "groups-admins", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testNullIsNotOp()
    {
        assertThat( groupDB.isOp( null ) ).isFalse();
        verifyNoInteractions( redisClient );
    }

    @Test
    public void testNonOpIsNotOp()
    {
        Response sisMemberResponse = mockResponse( Response::toBoolean, false );
        when( redisClient.sismember( "groups-ops", "bob" ) ).thenReturn( sisMemberResponse );
        assertThat( groupDB.isOp( "bob" ) ).isFalse();

        verify( redisClient ).sismember( "groups-ops", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testOpIsOp()
    {
        Response sisMemberResponse = mockResponse( Response::toBoolean, true );
        when( redisClient.sismember( "groups-ops", "bob" ) ).thenReturn( sisMemberResponse );
        assertThat( groupDB.isOp( "bob" ) ).isTrue();

        verify( redisClient ).sismember( "groups-ops", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void addAdminThatAlreadyExist()
    {
        Response saddResponse = mockResponse( Response::toLong, 0L );
        when( redisClient.sadd( List.of( "groups-admins", "bob" ) ) ).thenReturn( saddResponse );
        assertThat( groupDB.addAdmin( "bob" ) ).isFalse();

        verify( redisClient ).sadd( List.of( "groups-admins", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void addNewAdmin()
    {
        Response saddResponse = mockResponse( Response::toLong, 1L );
        when( redisClient.sadd( List.of( "groups-admins", "bob" ) ) ).thenReturn( saddResponse );
        assertThat( groupDB.addAdmin( "bob" ) ).isTrue();

        verify( redisClient ).sadd( List.of( "groups-admins", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void addOpThatAlreadyExist()
    {
        Response saddResponse = mockResponse( Response::toLong, 0L );
        when( redisClient.sadd( List.of( "groups-ops", "bob" ) ) ).thenReturn( saddResponse );
        assertThat( groupDB.addOp( "bob" ) ).isFalse();

        verify( redisClient ).sadd( List.of( "groups-ops", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void addNewOp()
    {
        Response saddResponse = mockResponse( Response::toLong, 1L );
        when( redisClient.sadd( List.of( "groups-ops", "bob" ) ) ).thenReturn( saddResponse );
        assertThat( groupDB.addOp( "bob" ) ).isTrue();

        verify( redisClient ).sadd( List.of( "groups-ops", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void removeAdminThatDoesNotExist()
    {
        Response sremResponse = mockResponse( Response::toLong, 0L );
        when( redisClient.srem( List.of( "groups-admins", "bob" ) ) ).thenReturn( sremResponse );
        assertThat( groupDB.remAdmin( "bob" ) ).isFalse();

        verify( redisClient ).srem( List.of( "groups-admins", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void removeAdmin()
    {
        Response sremResponse = mockResponse( Response::toLong, 1L );
        when( redisClient.srem( List.of( "groups-admins", "bob" ) ) ).thenReturn( sremResponse );
        assertThat( groupDB.remAdmin( "bob" ) ).isTrue();

        verify( redisClient ).srem( List.of( "groups-admins", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void removeOpThatDoesNotExist()
    {
        Response sremResponse = mockResponse( Response::toLong, 0L );
        when( redisClient.srem( List.of( "groups-ops", "bob" ) ) ).thenReturn( sremResponse );
        assertThat( groupDB.remOp( "bob" ) ).isFalse();

        verify( redisClient ).srem( List.of( "groups-ops", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void removeOp()
    {
        Response sremResponse = mockResponse( Response::toLong, 1L );
        when( redisClient.srem( List.of( "groups-ops", "bob" ) ) ).thenReturn( sremResponse );
        assertThat( groupDB.remOp( "bob" ) ).isTrue();

        verify( redisClient ).srem( List.of( "groups-ops", "bob" ) );
        verifyNoMoreInteractions( redisClient );
    }
}
