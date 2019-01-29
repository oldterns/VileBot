package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.db.LogDB;
import com.oldterns.vilebot.util.MangleNicks;
import com.oldterns.vilebot.util.Zalgo;

import net.engio.mbassy.listener.Handler;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by emmett on 12/08/15.
 */

@HandlerContainer
public class Markov
{

    Map<String, List<String>> markovMap = new HashMap<String, List<String>>();

    private static final Pattern cmd = Pattern.compile( "^!speak$" );

    private static final Pattern gospelPattern = Pattern.compile( "^!gospel$" );

    @AssignedBot
    private KeratinBot bot;

    @Handler
    public void speak( ReceivePrivmsg message )
    {

        String text = message.getText();
        boolean markovMap = cmd.matcher( text ).matches();
        boolean isGospel = gospelPattern.matcher( text ).matches();

        if ( markovMap || isGospel )
        {
            train();
            String phrase = generatePhrase();
            phrase = MangleNicks.mangleNicks( bot, message, phrase );
            if ( isGospel )
            {
                phrase = Zalgo.generate( phrase );
            }
            message.reply( phrase );
        }
    }

    private void train()
    {
        String data = LogDB.getLog();
        fillMarkovMap( data );
    }

    private void fillMarkovMap( String data )
    {
        String[] words = data.split( "\\s+" );

        for ( int i = 0; i < words.length - 3; i++ )
        {
            String key = words[i] + " " + words[i + 1];
            String value = words[i + 2] + " " + words[i + 3];

            if ( key.equals( value ) )
            {
                continue;
            }
            else if ( markovMap.get( key ) != null )
            {
                markovMap.get( key ).add( value );
            }
            else
            {
                List<String> valueList = new ArrayList<String>();
                valueList.add( value );
                markovMap.put( key, valueList );
            }
        }
    }

    private String generatePhrase()
    {
        Random random = new Random();
        String key = getRandomKey( random );
        String phrase = "";

        while ( key != null && phrase.length() < 1000 )
        {
            phrase += key + " ";
            if ( shouldEnd( key ) )
            {
                break;
            }
            key = nextKey( key, random );
        }

        return phrase.replace( "\n", " " );
    }

    private String nextKey( String key, Random random )
    {
        List<String> valueList = markovMap.get( key );
        if ( valueList == null )
        {
            return null;
        }

        return valueList.get( random.nextInt( valueList.size() ) );
    }

    private String getRandomKey( Random random )
    {
        Object[] values = markovMap.keySet().toArray();
        return (String) values[random.nextInt( values.length )];
    }

    private boolean shouldEnd( String key )
    {
        return ( key.endsWith( "!" ) || key.endsWith( "?" ) || key.endsWith( "." ) );
    }

}