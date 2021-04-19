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
        return redisClient.srandmember( List.of( keyOfExcuseSet ) ).toString();
    }
}
