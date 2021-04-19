/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.database;

import io.quarkus.redis.client.RedisClient;

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

    public Optional<Long> getLastSeenTime( String nick )
    {
        return Optional.ofNullable( redisClient.hget( keyOfLastSeenHash, nick ).toLong() );
    }

    public void updateLastSeenTime( String nick )
    {
        Long milliTime = System.currentTimeMillis();
        redisClient.hset( List.of( keyOfLastSeenHash, nick, milliTime.toString() ) );
    }
}
