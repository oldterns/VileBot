package com.oldterns.vilebot.handlers.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.LogDB;
import com.oldterns.vilebot.db.QuoteFactDB;
import com.oldterns.vilebot.util.MangleNicks;
import com.oldterns.vilebot.util.Zalgo;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

/**
 * Created by emmett on 12/08/15.
 */

public class Markov
    extends ListenerAdapter
{

    private Map<String, List<String>> markovMap = new HashMap<>();

    private static final Pattern cmd = Pattern.compile( "^!speak$" );

    private static final Pattern gospelPattern = Pattern.compile( "^!gospel$" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {

        String text = event.getMessage();
        boolean markovMap = cmd.matcher( text ).matches();
        boolean isGospel = gospelPattern.matcher( text ).matches();

        if ( markovMap || isGospel )
        {
            train();
            String phrase = generatePhrase();
            phrase = MangleNicks.mangleNicks( event, phrase );
            if ( isGospel )
            {
                phrase = Zalgo.generate( phrase );
            }
            event.respondWith( phrase );
        }
    }

    protected void train()
    {
        String data = LogDB.getLog();
        fillMarkovMap( data );
    }

    protected void trainOnNick( String nick )
    {
        Set<String> quotes = QuoteFactDB.getQuotes( nick );
        Set<String> facts = QuoteFactDB.getFacts( nick );
        StringBuilder trainingData = new StringBuilder();
        quotes.forEach( q -> trainingData.append( q ).append( "\n" ) );
        facts.forEach( f -> trainingData.append( f ).append( "\n" ) );
        fillMarkovMap( trainingData.toString() );
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
                List<String> valueList = new ArrayList<>();
                valueList.add( value );
                markovMap.put( key, valueList );
            }
        }
    }

    protected String generatePhrase()
    {
        Random random = new Random();
        String key = getRandomKey( random );
        StringBuilder phrase = new StringBuilder();

        while ( key != null && phrase.length() < 1000 )
        {
            phrase.append( key ).append( " " );
            if ( shouldEnd( key ) )
            {
                break;
            }
            key = nextKey( key, random );
        }

        return phrase.toString().replace( "\n", " " );
    }

    protected String generatePhrase( String topic )
    {
        Random random = new Random();
        String key = topic;
        StringBuilder phrase = new StringBuilder();

        while ( key != null && phrase.length() < 1000 )
        {
            phrase.append( key ).append( " " );
            if ( shouldEnd( key ) )
            {
                break;
            }
            key = nextKey( key, random );
        }

        return phrase.toString().replace( "\n", " " );
    }

    protected String generatePhrase( String topic, int idealLength )
    {
        Random random = new Random();
        String key = topic;
        StringBuilder phrase = new StringBuilder();

        while ( key != null && phrase.length() < idealLength )
        {
            phrase.append( key ).append( " " );
            key = nextKey( key, random );
        }

        return phrase.toString().replace( "\n", " " );
    }

    protected String generatePhrase( int idealLength )
    {
        Random random = new Random();
        String key = getRandomKey( random );
        StringBuilder phrase = new StringBuilder();

        while ( key != null && phrase.length() < idealLength )
        {
            phrase.append( key ).append( " " );
            key = nextKey( key, random );
        }

        return phrase.toString().replace( "\n", " " );
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