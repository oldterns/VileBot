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
public class UserlistDBTest
    extends AbstractDatabaseTest
{

    @Inject
    UserlistDB userlistDB;

    @Test
    public void testGetUsersInList()
    {
        Response memberResponse = mockResponse( Response::stream,
                                                Stream.of( mockResponse( Response::toString, "alice" ),
                                                           mockResponse( Response::toString, "bob" ),
                                                           mockResponse( Response::toString, "charlie" ) ) );

        when( redisClient.smembers( "userlist-test" ) ).thenReturn( memberResponse );
        assertThat( userlistDB.getUsersIn( "test" ) ).containsExactlyInAnyOrder( "alice", "bob", "charlie" );

        verify( redisClient ).smembers( "userlist-test" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testAddUsersToList()
    {
        userlistDB.addUsersTo( "test", List.of( "alice", "bob", "charlie" ) );
        verify( redisClient ).sadd( List.of( "userlist-test", "alice", "bob", "charlie" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRemoveUsersFromList()
    {
        userlistDB.removeUsersFrom( "test", List.of( "alice", "bob", "charlie" ) );
        verify( redisClient ).srem( List.of( "userlist-test", "alice", "bob", "charlie" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetLists()
    {
        Response keysResponse = mockResponse( Response::stream,
                                              Stream.of( mockResponse( Response::toString, "userlist-alice" ),
                                                         mockResponse( Response::toString, "userlist-bob" ),
                                                         mockResponse( Response::toString, "userlist-charlie" ) ) );
        when( redisClient.keys( "userlist-*" ) ).thenReturn( keysResponse );

        assertThat( userlistDB.getLists() ).containsExactlyInAnyOrder( "alice", "bob", "charlie" );
        verify( redisClient ).keys( "userlist-*" );
        verifyNoMoreInteractions( redisClient );
    }
}
