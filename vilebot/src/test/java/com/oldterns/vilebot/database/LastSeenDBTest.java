package com.oldterns.vilebot.database;

import java.util.List;

import javax.inject.Inject;

import com.oldterns.vilebot.util.TimeService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.vertx.redis.client.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class LastSeenDBTest
    extends AbstractDatabaseTest
{

    @Inject
    LastSeenDB lastSeenDB;

    @InjectMock
    TimeService timeService;

    @Test
    public void testGetLastSeenTimeDoesNotExist()
    {
        when( redisClient.hget( "last-seen", "bob" ) ).thenReturn( null );
        assertThat( lastSeenDB.getLastSeenTime( "bob" ) ).isEmpty();

        verify( redisClient ).hget( "last-seen", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetLastSeenTime()
    {
        Response hgetResponse = mockResponse( Response::toLong, 1234L );
        when( redisClient.hget( "last-seen", "bob" ) ).thenReturn( hgetResponse );
        assertThat( lastSeenDB.getLastSeenTime( "bob" ) ).hasValue( 1234L );

        verify( redisClient ).hget( "last-seen", "bob" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testUpdateLastSeenTime()
    {
        when( timeService.getCurrentTimeMills() ).thenReturn( 1234L );
        lastSeenDB.updateLastSeenTime( "bob" );

        verify( redisClient ).hset( List.of( "last-seen", "bob", "1234" ) );
        verifyNoMoreInteractions( redisClient );
    }
}
