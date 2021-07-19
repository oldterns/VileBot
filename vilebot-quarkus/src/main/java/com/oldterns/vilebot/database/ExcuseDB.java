/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.database;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;
import org.jsoup.Connection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ExcuseDB
{
    private static final String keyOfExcuseSet = "excuses";

    @Inject
    RedisClient redisClient;

    public void addExcuse( String excuse )
    {
        redisClient.sadd( List.of( keyOfExcuseSet, excuse ) );
    }

    public String getRandExcuse()
    {
        return Optional.ofNullable(redisClient.srandmember( List.of( keyOfExcuseSet ) ))
                .map(Response::toString).orElse(null);
    }
}
