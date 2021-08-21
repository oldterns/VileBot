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
public class ExcuseDBTest
    extends AbstractDatabaseTest
{

    @Inject
    ExcuseDB excuseDB;

    @Test
    public void testAddExcuse()
    {
        excuseDB.addExcuse( "this is an excuse" );

        verify( redisClient ).sadd( List.of( "excuses", "this is an excuse" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRandomExcuseNoExcuses()
    {
        when( redisClient.srandmember( List.of( "excuses" ) ) ).thenReturn( null );
        assertThat( excuseDB.getRandExcuse() ).isNull();
        verify( redisClient ).srandmember( List.of( "excuses" ) );
        verifyNoMoreInteractions( redisClient );
    }

    @Test
    public void testRandomExcuse()
    {
        Response randomExcuse = mockResponse( Response::toString, "this is an excuse" );
        when( redisClient.srandmember( List.of( "excuses" ) ) ).thenReturn( randomExcuse );
        assertThat( excuseDB.getRandExcuse() ).isEqualTo( "this is an excuse" );
        verify( redisClient ).srandmember( List.of( "excuses" ) );
        verifyNoMoreInteractions( redisClient );
    }
}
