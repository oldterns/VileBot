package com.oldterns.vilebot.db;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

/**
 * Created by eunderhi on 18/08/15.
 */
public class LogDB
    extends RedisDB
{

    public static String logKey = "chat-log";

    private static final int MAX_LOG_SIZE = 100000;

    public static void addItem( String chatMessage )
    {
        Jedis jedis = pool.getResource();
        try
        {
            if ( jedis.strlen( logKey ) > MAX_LOG_SIZE )
            {
                jedis.set( logKey, StringUtils.right( jedis.get( logKey ), MAX_LOG_SIZE - chatMessage.length() ) );
            }
            jedis.append( logKey, chatMessage );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    public static String getLog()
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.get( logKey );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    public static void deleteLog()
    {
        Jedis jedis = pool.getResource();
        try
        {
            jedis.del( logKey );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }
}
