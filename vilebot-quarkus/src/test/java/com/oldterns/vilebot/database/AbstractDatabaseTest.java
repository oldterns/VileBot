package com.oldterns.vilebot.database;

import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.test.junit.mockito.InjectMock;
import io.vertx.redis.client.Response;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractDatabaseTest
{

    @InjectMock
    RedisClient redisClient;

    <T> Response mockResponse( Function<Response, T> method, T result )
    {
        Response out = mock( Response.class );
        when( method.apply( out ) ).thenReturn( result );
        return out;
    }

}
