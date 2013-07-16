/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pmw.tinylog.Logger;

import net.engio.mbassy.listener.Handler;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import com.oldterns.vilebot.util.NickIgnore;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

@HandlerContainer
public class Weather
{
    private static final String LESS_NICK = "owilliams";
    static
    {
        NickIgnore.addOnJoin( LESS_NICK );
    }

    private static final String defaultLocation = "ytz";

    private static final HashMap<String, URL> weatherFeedsByIataCode = new LinkedHashMap<String, URL>();
    static
    {
        try
        {
            weatherFeedsByIataCode.put( "yxu", new URL( "http://weatheroffice.gc.ca/rss/city/on-137_e.xml" ) );
            weatherFeedsByIataCode.put( "yyz", new URL( "http://weatheroffice.gc.ca/rss/city/on-143_e.xml" ) );
            weatherFeedsByIataCode.put( "ygk", new URL( "http://weatheroffice.gc.ca/rss/city/on-69_e.xml" ) );
            weatherFeedsByIataCode.put( "yeg", new URL( "http://weatheroffice.gc.ca/rss/city/ab-50_e.xml" ) );
            weatherFeedsByIataCode.put( "yyc", new URL( "http://weatheroffice.gc.ca/rss/city/ab-52_e.xml" ) );
            weatherFeedsByIataCode.put( "yul", new URL( "http://weatheroffice.gc.ca/rss/city/qc-147_e.xml" ) );
            weatherFeedsByIataCode.put( "yvr", new URL( "http://weatheroffice.gc.ca/rss/city/bc-74_e.xml" ) );
            weatherFeedsByIataCode.put( "yhm", new URL( "http://weatheroffice.gc.ca/rss/city/on-77_e.xml" ) );
            weatherFeedsByIataCode.put( "ytz", new URL( "http://weatheroffice.gc.ca/rss/city/on-128_e.xml" ) );
            weatherFeedsByIataCode.put( "ykz", new URL( "http://weatheroffice.gc.ca/rss/city/on-85_e.xml" ) );
            weatherFeedsByIataCode.put( "ykf", new URL( "http://weatheroffice.gc.ca/rss/city/on-82_e.xml" ) );
            weatherFeedsByIataCode.put( "yhz", new URL( "http://weatheroffice.gc.ca/rss/city/ns-19_e.xml" ) );
            weatherFeedsByIataCode.put( "yfc", new URL( "http://weatheroffice.gc.ca/rss/city/nb-29_e.xml" ) );
            weatherFeedsByIataCode.put( "yyt", new URL( "http://weatheroffice.gc.ca/rss/city/nl-24_e.xml" ) );
            weatherFeedsByIataCode.put( "yyg", new URL( "http://weatheroffice.gc.ca/rss/city/pe-5_e.xml" ) );
            weatherFeedsByIataCode.put( "yqt", new URL( "http://weatheroffice.gc.ca/rss/city/on-100_e.xml" ) );
            weatherFeedsByIataCode.put( "ywg", new URL( "http://weatheroffice.gc.ca/rss/city/mb-38_e.xml" ) );
            weatherFeedsByIataCode.put( "yqr", new URL( "http://weatheroffice.gc.ca/rss/city/sk-32_e.xml" ) );
            weatherFeedsByIataCode.put( "yxe", new URL( "http://weatheroffice.gc.ca/rss/city/sk-40_e.xml" ) );
            weatherFeedsByIataCode.put( "ymm", new URL( "http://weatheroffice.gc.ca/rss/city/ab-20_e.xml" ) );
            weatherFeedsByIataCode.put( "yxs", new URL( "http://weatheroffice.gc.ca/rss/city/bc-79_e.xml" ) );
            weatherFeedsByIataCode.put( "yyj", new URL( "http://weatheroffice.gc.ca/rss/city/bc-85_e.xml" ) );
            weatherFeedsByIataCode.put( "ylw", new URL( "http://weatheroffice.gc.ca/rss/city/bc-48_e.xml" ) );
            weatherFeedsByIataCode.put( "yka", new URL( "http://weatheroffice.gc.ca/rss/city/bc-45_e.xml" ) );
            weatherFeedsByIataCode.put( "yow", new URL( "http://weatheroffice.gc.ca/rss/city/on-118_e.xml" ) );
            weatherFeedsByIataCode.put( "yxy", new URL( "http://weatheroffice.gc.ca/rss/city/yt-16_e.xml" ) );
            weatherFeedsByIataCode.put( "yzf", new URL( "http://weatheroffice.gc.ca/rss/city/nt-24_e.xml" ) );
            weatherFeedsByIataCode.put( "yfb", new URL( "http://weatheroffice.gc.ca/rss/city/nu-21_e.xml" ) );
            weatherFeedsByIataCode.put( "ylt", new URL( "http://weatheroffice.gc.ca/rss/city/nu-22_e.xml" ) );
        }
        catch ( MalformedURLException e )
        {
            Logger.error( e, "Error loading weather URLs" );
            throw new RuntimeException( e );
        }
    }

    private static HashMap<String, WeatherData> weatherDataByIataCode = new HashMap<String, WeatherData>();

    private static final long weatherDataCacheTime = 1000 * 60 * 30;

    private static final Pattern weatherPattern = Pattern.compile( "!(less|more|)weather(?: ([a-zA-Z]{3,3})|)" );

    private static final Pattern forecastPattern = Pattern.compile( "!forecast(?: ([a-zA-Z]{3,3})|)" );

    @AssignedBot
    private KeratinBot bot;

    @Handler
    private void forecastWeather( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = forecastPattern.matcher( text );
        if ( matcher.matches() )
        {
            String locationCode = matcher.group( 1 ); // The IATA code
            if ( locationCode == null )
            {
                locationCode = defaultLocation;
            }
            locationCode = locationCode.toLowerCase();

            if ( weatherFeedsByIataCode.containsKey( locationCode ) )
            {
                WeatherData weather = getWeatherFor( locationCode );
                if ( weather == null )
                {
                    event.reply( "Error reading weather feed for " + locationCode );
                }
                else
                {
                    StringBuilder sb = new StringBuilder();
                    for ( Entry<String, String> forecastDay : weather.getForecast().entrySet() )
                    {
                        sb.append( "[" );
                        sb.append( forecastDay.getKey() );
                        sb.append( ": " );
                        sb.append( forecastDay.getValue() );
                        sb.append( "] " );
                    }
                    event.reply( sb.toString() );
                }
            }
        }
    }

    @Handler
    private void currentWeather( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = weatherPattern.matcher( text );

        if ( matcher.matches() )
        {
            String modifier = matcher.group( 1 ); // Either less or more or null
            String locationCode = matcher.group( 2 ); // The IATA code
            if ( locationCode == null )
            {
                locationCode = defaultLocation;
            }
            locationCode = locationCode.toLowerCase();

            if ( weatherFeedsByIataCode.containsKey( locationCode ) )
            {
                WeatherData weather = getWeatherFor( locationCode );
                if ( weather == null )
                {
                    event.reply( "Error reading weather feed for " + locationCode );
                }
                else
                {
                    if ( !"less".equals( modifier ) )
                    {
                        for ( String alert : weather.getAlerts() )
                        {
                            event.reply( alert );
                        }
                    }

                    LinkedHashMap<String, String> currentConditions = weather.getCurrentConditions();
                    if ( "".equals( modifier ) )
                    {
                        StringBuilder sb = new StringBuilder();

                        sb.append( currentConditions.get( "Condition" ) );
                        sb.append( ", Temperature: " );
                        sb.append( currentConditions.get( "Temperature" ) );
                        sb.append( ", Humidity: " );
                        sb.append( currentConditions.get( "Humidity" ) );
                        sb.append( " - " );
                        sb.append( currentConditions.get( "Observed at" ) );

                        event.reply( sb.toString() );
                    }
                    else if ( "less".equals( modifier ) )
                    {
                        bot.sendPrivmsgAs( LESS_NICK, event.getChannel(), "IT'S "
                            + currentConditions.get( "Condition" ).toUpperCase() );
                    }
                    else if ( "more".equals( modifier ) )
                    {
                        StringBuilder sb = new StringBuilder();
                        for ( Entry<String, String> condition : currentConditions.entrySet() )
                        {
                            sb.append( "[" );
                            sb.append( condition.getKey() );
                            sb.append( ": " );
                            sb.append( condition.getValue() );
                            sb.append( "] " );
                        }
                        event.reply( sb.toString() );
                    }
                }
            }
            else
            {
                event.replyDirectly( "No weather feed available for " + locationCode );
            }
        }
    }

    private WeatherData getWeatherFor( String locationCode )
    {
        if ( weatherDataByIataCode.containsKey( locationCode ) )
        {
            WeatherData weather = weatherDataByIataCode.get( locationCode );

            long timeDiff = new Date().getTime() - weather.getCreationDate().getTime();
            // Cache for at most half an hour
            if ( timeDiff < weatherDataCacheTime )
            {
                return weather;
            }
        }

        URL feedSource = weatherFeedsByIataCode.get( locationCode );
        WeatherData weather = null;
        try
        {
            weather = new WeatherData( feedSource );
            weatherDataByIataCode.put( locationCode, weather );
        }
        catch ( IllegalArgumentException | IOException | FeedException e )
        {
            Logger.error( e, "Error opening RSS feed" );
        }
        return weather;
    }

    /**
     * Stores weather data from http://weather.gc.ca/rss/city/* rss feeds flexibly.
     */
    private class WeatherData
    {
        private final Pattern alertIDPattern = Pattern.compile( ".*_w[0-9]+:[0-9]{14,14}$" );

        private final Pattern currentConditionsIDPattern = Pattern.compile( ".*_cc:[0-9]{14,14}$" );

        private final Pattern forecastIDPattern = Pattern.compile( ".*_fc[0-9]:[0-9]{14,14}$" );

        private final LinkedList<String> alerts;

        private final LinkedHashMap<String, String> currentConditions;

        private final LinkedHashMap<String, String> forecast;

        private final Date creationTime;

        public WeatherData( URL feedSource )
            throws IOException, IllegalArgumentException, FeedException
        {
            alerts = new LinkedList<String>();
            forecast = new LinkedHashMap<String, String>();
            currentConditions = new LinkedHashMap<String, String>();
            creationTime = new Date();

            try (XmlReader reader = new XmlReader( feedSource ))
            {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build( reader );

                // System.out.println( feed );

                for ( Object rawEntry : feed.getEntries() )
                {
                    if ( rawEntry instanceof SyndEntry )
                    {
                        SyndEntry entry = (SyndEntry) rawEntry;

                        if ( alertIDPattern.matcher( entry.getUri() ).matches() )
                        {
                            String desc = entry.getDescription().getValue();
                            if ( !desc.equals( "No watches or warnings in effect." ) )
                            {
                                alerts.add( desc );
                            }
                        }
                        else if ( currentConditionsIDPattern.matcher( entry.getUri() ).matches() )
                        {
                            String desc = entry.getDescription().getValue();
                            desc = desc.replaceAll( "<[a-zA-Z/]+>", "" );
                            desc = desc.replace( "&#176;", "°" );
                            for ( String line : desc.split( "\n" ) )
                            {
                                int sepPos = line.indexOf( ':' );
                                currentConditions.put( line.substring( 0, sepPos ), line.substring( sepPos + 1 ).trim() );
                            }
                        }
                        else if ( forecastIDPattern.matcher( entry.getUri() ).matches() )
                        {
                            String title = entry.getTitle();
                            int sepPos = title.indexOf( ':' );
                            forecast.put( title.substring( 0, sepPos ), title.substring( sepPos + 1 ).trim() );
                        }
                    }
                }
            }
        }

        public LinkedList<String> getAlerts()
        {
            return alerts;
        }

        public LinkedHashMap<String, String> getCurrentConditions()
        {
            return currentConditions;
        }

        public LinkedHashMap<String, String> getForecast()
        {
            return forecast;
        }

        public Date getCreationDate()
        {
            return creationTime;
        }

        @Override
        public String toString()
        {
            return "WeatherData [alerts=" + alerts + ", currentConditions=" + currentConditions + ", forecast="
                + forecast + ", creationTime=" + creationTime + "]";
        }
    }
}
