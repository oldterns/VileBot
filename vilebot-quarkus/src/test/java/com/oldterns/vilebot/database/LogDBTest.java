package com.oldterns.vilebot.database;

import java.util.List;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.redis.client.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class LogDBTest
    extends AbstractDatabaseTest
{

    @Inject
    LogDB logDB;

    @Test
    public void testDeleteLog()
    {
        logDB.deleteLog();
        verify( redisClient ).del( List.of( LogDB.logKey ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testGetLog()
    {
        Response getResponse = mockResponse( Response::toString, "A log\nWith several\nMessages" );
        when( redisClient.get( LogDB.logKey ) ).thenReturn( getResponse );

        assertThat( logDB.getLog() ).isEqualTo( "A log\nWith several\nMessages" );
        verify( redisClient ).get( LogDB.logKey );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testAddItem()
    {
        Response strLenResponse = mockResponse( Response::toLong, 0L );
        when( redisClient.strlen( LogDB.logKey ) ).thenReturn( strLenResponse );

        logDB.addItem( "chat message" );

        verify( redisClient ).strlen( LogDB.logKey );
        verify( redisClient ).append( LogDB.logKey, "chat message" );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void addItemToFullLog()
    {
        String oldText = "a".repeat( 100001 );

        Response strLenResponse = mockResponse( Response::toLong, 100001L );
        Response getResponse = mockResponse( Response::toString, oldText );
        when( redisClient.strlen( LogDB.logKey ) ).thenReturn( strLenResponse );
        when( redisClient.get( LogDB.logKey ) ).thenReturn( getResponse );

        logDB.addItem( "bbbbb" );

        verify( redisClient ).strlen( LogDB.logKey );
        verify( redisClient ).get( LogDB.logKey );
        verify( redisClient ).set( List.of( LogDB.logKey, "a".repeat( 100000 - 5 ) ) );
        verify( redisClient ).append( LogDB.logKey, "bbbbb" );
    }
}
