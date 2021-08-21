/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.database;

import com.oldterns.vilebot.util.TimeService;
import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LastSeenDB
{
    private static final String keyOfLastSeenHash = "last-seen";

    @Inject
    RedisClient redisClient;

    @Inject
    TimeService timeService;

    public Optional<Long> getLastSeenTime( String nick )
    {
        Response response = redisClient.hget( keyOfLastSeenHash, nick );
        if ( response != null )
        {
            return Optional.of( response.toLong() );
        }
        else
        {
            return Optional.empty();
        }
    }

    public void updateLastSeenTime( String nick )
    {
        Long milliTime = timeService.getCurrentTimeMills();
        redisClient.hset( List.of( keyOfLastSeenHash, nick, milliTime.toString() ) );
    }
}
