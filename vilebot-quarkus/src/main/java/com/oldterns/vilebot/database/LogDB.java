package com.oldterns.vilebot.database;

import io.quarkus.redis.client.RedisClient;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * Created by eunderhi on 18/08/15.
 */
@ApplicationScoped
public class LogDB
{

    public static String logKey = "chat-log";

    private static final int MAX_LOG_SIZE = 100000;

    @Inject
    RedisClient redisClient;

    public void addItem( String chatMessage )
    {
        if ( redisClient.strlen( logKey ).toLong() > MAX_LOG_SIZE )
        {
            redisClient.set( List.of( logKey, StringUtils.right( redisClient.get( logKey ).toString(),
                                                                 MAX_LOG_SIZE - chatMessage.length() ) ) );
        }
        redisClient.append( logKey, chatMessage );
    }

    public String getLog()
    {
        return redisClient.get( logKey ).toString();
    }

    public void deleteLog()
    {
        redisClient.del( List.of( logKey ) );
    }
}
