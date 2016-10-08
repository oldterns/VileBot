/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import redis.clients.jedis.Jedis;

public class ExcuseDB extends RedisDB {
    private static final String keyOfExcuseSet = "excuses";

    public static void addExcuse(String excuse) {
        Jedis jedis = pool.getResource();
        try {
            jedis.sadd(keyOfExcuseSet, excuse);
        } finally {
            pool.returnResource(jedis);
        }
    }

    public static String getRandExcuse() {
        Jedis jedis = pool.getResource();
        try {
            return jedis.srandmember(keyOfExcuseSet);
        } finally {
            pool.returnResource(jedis);
        }
    }
}
