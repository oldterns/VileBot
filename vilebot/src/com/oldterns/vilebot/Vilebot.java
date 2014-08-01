/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;

import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.ConfMap;
import com.oldterns.vilebot.util.Ignore;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.misc.Logging;
import ca.szc.keratin.core.net.IrcConnection.SslMode;

public class Vilebot
{
    private static JedisPool pool;
    private static final Map<String, String> cfg = Collections.unmodifiableMap( getConfigMap( "cfg", "vilebot.conf" ) );

    public static void main( String[] args )
    {
        // Logging
        LoggingLevel logLevel = LoggingLevel.valueOf( cfg.get( "logLevel" ) );
        Logging.activateLoggingConfig( logLevel );

        Logger.trace( "Got config: " + cfg );

        // Database
        String redisHost = cfg.get( "redisHost" );
        int redisPort = Integer.parseInt( cfg.get( "redisPort" ) );
        pool = new JedisPool( new JedisPoolConfig(), redisHost, redisPort );

        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            public void run()
            {
                pool.destroy();
            }
        } );

        BaseNick.setPrimaryBotNick( cfg.get( "ircNick1" ) );

        // Bot
        int ircBotAmount = Integer.parseInt( cfg.get( "ircBotAmount" ) );
        for ( int i = 1; i <= ircBotAmount; i++ )
        {
            String ircUser = cfg.get( "ircUser" + i );
            String ircNick = cfg.get( "ircNick" + i );
            String ircRealName = cfg.get( "ircRealName" + i );
            String ircServerAddress = cfg.get( "ircServerAddress" + i );
            int ircPort = Integer.parseInt( cfg.get( "ircPort" + i ) );
            SslMode ircSslMode = SslMode.valueOf( cfg.get( "ircSslMode" + i ) );
            String ircChannel = cfg.get( "ircChannel" + i );
            boolean ircChannelAutoOp = Boolean.parseBoolean( cfg.get( "ircChannelAutoOp" + i ) );

            BaseNick.addBotNick( ircNick );

            KeratinBot keratinBot = new KeratinBot();
            keratinBot.setUser( ircUser );
            keratinBot.setNick( ircNick );
            keratinBot.setRealName( ircRealName );
            keratinBot.setServerAddress( ircServerAddress );
            keratinBot.setServerPort( ircPort );
            keratinBot.setSslMode( ircSslMode );
            keratinBot.addChannel( ircChannel );

            if ( !ircChannelAutoOp )
                Ignore.addAutoOp( ircChannel );

            keratinBot.connect();
        }

        // Done
    }

    private static Map<String, String> getConfigMap( String dir, String conf )
    {
        FileSystem fs = FileSystems.getDefault();
        Path cfgPath = fs.getPath( dir, conf );

        Map<String, String> cfg;
        try
        {
            cfg = new ConfMap( cfgPath );
        }
        catch ( IOException e )
        {
            Logger.error(e, "Can't load cfgPath " + cfgPath);
            throw new RuntimeException( "Can't load cfgPath " + cfgPath, e );
        }
        return Collections.unmodifiableMap( cfg );
    }

    public static JedisPool getPool()
    {
        return pool;
    }

    public static Map<String, String> getConfig()
    {
        return new HashMap<> (cfg);
    }
}
