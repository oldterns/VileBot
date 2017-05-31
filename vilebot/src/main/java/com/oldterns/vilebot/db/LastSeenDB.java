/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import redis.clients.jedis.Jedis;

public class LastSeenDB
    extends RedisDB
{
    private static final String keyOfLastSeenHash = "last-seen";

    public static long getLastSeenTime( String nick )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return Long.valueOf( jedis.hget( keyOfLastSeenHash, nick ) );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    public static void updateLastSeenTime( String nick )
    {
        Jedis jedis = pool.getResource();
        try
        {
            Long milliTime = System.currentTimeMillis();
            jedis.hset( keyOfLastSeenHash, nick, milliTime.toString() );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }
}
