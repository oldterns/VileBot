package com.oldterns.vilebot.db;

import redis.clients.jedis.Jedis;

/**
 * Created by eunderhi on 18/08/15.
 */
public class LogDB extends RedisDB {

    private static final String logKey = "chat-log";

    public static void addItem(String chatMessage) {
        Jedis jedis = pool.getResource();
        try {
            jedis.append(logKey, chatMessage);
        } finally {
            pool.returnResource(jedis);
        }
    }

    public static String getLog() {
        Jedis jedis = pool.getResource();
        try {
            return jedis.get(logKey);
        } finally {
            pool.returnResource(jedis);
        }
    }
}
