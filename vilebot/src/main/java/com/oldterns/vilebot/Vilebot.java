/*
  Copyright (C) 2013 Oldterns

  This file may be modified and distributed under the terms
  of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.oldterns.vilebot.handlers.admin.AdminManagement;
import com.oldterns.vilebot.handlers.admin.AdminPing;
import com.oldterns.vilebot.handlers.admin.Auth;
import com.oldterns.vilebot.handlers.admin.GetLog;
import com.oldterns.vilebot.handlers.admin.NickChange;
import com.oldterns.vilebot.handlers.admin.Quit;
import com.oldterns.vilebot.handlers.user.AnswerQuestion;
import com.oldterns.vilebot.handlers.user.Ascii;
import com.oldterns.vilebot.handlers.user.ChatLogger;
import com.oldterns.vilebot.handlers.user.Church;
import com.oldterns.vilebot.handlers.user.Countdown;
import com.oldterns.vilebot.handlers.user.Decide;
import com.oldterns.vilebot.handlers.user.DownOrJustMe;
import com.oldterns.vilebot.handlers.user.Excuses;
import com.oldterns.vilebot.handlers.user.FakeNews;
import com.oldterns.vilebot.handlers.user.Fortune;
import com.oldterns.vilebot.handlers.user.GetInfoOn;
import com.oldterns.vilebot.handlers.user.Help;
import com.oldterns.vilebot.handlers.user.ImageToAscii;
import com.oldterns.vilebot.handlers.user.Inspiration;
import com.oldterns.vilebot.handlers.user.Jaziz;
import com.oldterns.vilebot.handlers.user.Jokes;
import com.oldterns.vilebot.handlers.user.Kaomoji;
import com.oldterns.vilebot.handlers.user.Karma;
import com.oldterns.vilebot.handlers.user.KarmaRoll;
import com.oldterns.vilebot.handlers.user.KarmaTransfer;
import com.oldterns.vilebot.handlers.user.LastMessageSed;
import com.oldterns.vilebot.handlers.user.LastSeen;
import com.oldterns.vilebot.handlers.user.Markov;
import com.oldterns.vilebot.handlers.user.News;
import com.oldterns.vilebot.handlers.user.Omgword;
import com.oldterns.vilebot.handlers.user.Ops;
import com.oldterns.vilebot.handlers.user.QuotesAndFacts;
import com.oldterns.vilebot.handlers.user.RemindMe;
import com.oldterns.vilebot.handlers.user.RockPaperScissors;
import com.oldterns.vilebot.handlers.user.Summon;
import com.oldterns.vilebot.handlers.user.Trivia;
import com.oldterns.vilebot.handlers.user.Ttc;
import com.oldterns.vilebot.handlers.user.TwitterCorrection;
import com.oldterns.vilebot.handlers.user.UrlTitleAnnouncer;
import com.oldterns.vilebot.handlers.user.UrlTweetAnnouncer;
import com.oldterns.vilebot.handlers.user.UserPing;
import com.oldterns.vilebot.handlers.user.Userlists;
import com.oldterns.vilebot.handlers.user.Weather;
import com.oldterns.vilebot.util.BaseNick;
import org.pircbotx.Configuration;
import org.pircbotx.MultiBotManager;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class Vilebot
    extends ListenerAdapter
{

    private static final Logger logger = LoggerFactory.getLogger( Vilebot.class );

    private static final String BOT_CONFIG_FILE = "cfg/vilebot.conf";

    private static Map<String, String> cfg = getConfigMap();

    private static JedisPool pool;

    public static void main( String[] args )
    {
        // Database
        String redisHost = cfg.get( "redisHost" );
        int redisPort = Integer.parseInt( cfg.get( "redisPort" ) );
        String redisPassword = cfg.get( "redisPassword" );
        if ( redisPassword.length() > 0 )
        {
            pool =
                new JedisPool( new JedisPoolConfig(), redisHost, redisPort, Protocol.DEFAULT_TIMEOUT, redisPassword );
        }
        else
        {
            pool = new JedisPool( new JedisPoolConfig(), redisHost, redisPort );
        }

        Runtime.getRuntime().addShutdownHook( new Thread( () -> pool.destroy() ) );

        BaseNick.setPrimaryBotNick( cfg.get( "ircNick1" ) );

        MultiBotManager botManager = new MultiBotManager();

        // Bot
        int ircBotAmount = Integer.parseInt( cfg.get( "ircBotAmount" ) );
        for ( int i = 1; i <= ircBotAmount; i++ )
        {
            String ircUser = cfg.get( "ircUser" + i );
            String ircNick = cfg.get( "ircNick" + i );
            String ircRealName = cfg.get( "ircRealName" + i );
            String ircServerAddress = cfg.get( "ircServerAddress" + i );
            int ircPort = Integer.parseInt( cfg.get( "ircPort" + i ) );
            String ircChannel = cfg.get( "ircChannel" + i );

            BaseNick.addBotNick( ircNick );

            Configuration botConfiguration =
                new Configuration.Builder().setName( ircNick ).setLogin( ircUser ).setRealName( ircRealName ).addServer( ircServerAddress,
                                                                                                                         ircPort ).addAutoJoinChannel( ircChannel ).setAutoReconnect( true ).addListener( new Vilebot() ).addListener( new AdminManagement() ).addListener( new AdminPing() ).addListener( new Auth() ).addListener( new DownOrJustMe() ).addListener( new GetLog() ).addListener( new com.oldterns.vilebot.handlers.admin.Help() ).addListener( new NickChange() ).addListener( new com.oldterns.vilebot.handlers.admin.Ops() ).addListener( new Quit() ).addListener( new AnswerQuestion() ).addListener( new Ascii() ).addListener( new ChatLogger() ).addListener( new Church() ).addListener( new Countdown() ).addListener( new Decide() ).addListener( new Excuses() ).addListener( new FakeNews() ).addListener( new Fortune() ).addListener( new GetInfoOn() ).addListener( new Help() ).addListener( new ImageToAscii() ).addListener( new Inspiration() ).addListener( new Jaziz() ).addListener( new Jokes() ).addListener( new Kaomoji() ).addListener( new Karma() ).addListener( new KarmaRoll() ).addListener( new KarmaTransfer() ).addListener( new LastMessageSed() ).addListener( new LastSeen() ).addListener( new Markov() ).addListener( new News() ).addListener( new Omgword() ).addListener( new Ops() ).addListener( new QuotesAndFacts() ).addListener( new RemindMe() ).addListener( new RockPaperScissors() ).addListener( new Summon() ).addListener( new Trivia() ).addListener( new Ttc() ).addListener( new TwitterCorrection() ).addListener( new UrlTitleAnnouncer() ).addListener( new UrlTweetAnnouncer() ).addListener( new Userlists() ).addListener( new UserPing() ).addListener( new Weather() ).buildConfiguration();

            botManager.addBot( botConfiguration );
        }

        botManager.start();
        // Done
    }

    private static Map<String, String> getConfigMap()
    {
        Map<String, String> cfg = new HashMap<>();
        Properties prop = new Properties();
        InputStream input = null;
        try
        {
            input = new FileInputStream( BOT_CONFIG_FILE );
            prop.load( input );
            Enumeration<?> e = prop.propertyNames();
            while ( e.hasMoreElements() )
            {
                String key = (String) e.nextElement();
                String val = prop.getProperty( key );
                cfg.put( key, val );
            }
        }
        catch ( IOException e )
        {
            logger.error( e.getMessage() );
        }
        finally
        {
            if ( input != null )
            {
                try
                {
                    input.close();
                }
                catch ( IOException e )
                {
                    logger.error( e.getMessage() );
                }
            }
        }
        return cfg;
    }

    public static JedisPool getPool()
    {
        return pool;
    }

    public static Map<String, String> getConfig()
    {
        return new HashMap<>( cfg );
    }

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        logger.info( String.format( "[%s] %s", event.getUser().getNick(), event.getMessage() ) );
    }
}
