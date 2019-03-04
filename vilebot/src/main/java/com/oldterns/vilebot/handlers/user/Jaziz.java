package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;
import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eunderhi on 27/07/16.
 */

public class Jaziz
    extends ListenerAdapter
{

    private static final String API_KEY = Vilebot.getConfig().get( "thesaurusKey" );

    private static final String API_URL = "http://words.bighugelabs.com/api/2/" + API_KEY + "/";

    private static final String API_FORMAT = "/json";

    private static final Random random = new Random();

    private static final Pattern jazizPattern = Pattern.compile( "^!jaziz (.+)" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        Matcher questionMatcher = jazizPattern.matcher( event.getMessage() );
        if ( questionMatcher.matches() )
        {
            String message = questionMatcher.group( 1 );
            try
            {
                message = jazizify( message );
                event.respondWith( message );
            }
            catch ( Exception e )
            {
                event.respondWith( "eeeh" );
                e.printStackTrace();
            }
        }
    }

    static String jazizify( String message )
        throws Exception
    {
        String[] words = splitWords( message );
        for ( int i = 0; i < words.length; i++ )
        {
            if ( !words[i].contains( " " ) )
            {
                String replacement = words[i].length() > 3 ? randomChoice( getSynonyms( words[i] ) ) : words[i];
                if ( !replacement.isEmpty() )
                {
                    words[i] = replacement;
                }
            }
        }
        return stringify( words );
    }

    private static String[] splitWords( String message )
    {
        return message.split( "\\b" );
    }

    private static List<String> getSynonyms( String word )
        throws Exception
    {
        JSONObject json = new JSONObject( getContent( word ) );

        List<String> synonyms = new ArrayList<String>();
        String[] wordTypes = { "adjective", "noun", "adverb", "verb", "pronoun" };
        for ( String type : wordTypes )
        {
            if ( json.has( type ) )
            {
                JSONArray syns = getSyns( json.getJSONObject( type ) );
                synonyms.addAll( jsonToList( syns ) );
            }
        }
        return synonyms;
    }

    private static JSONArray getSyns( JSONObject json )
        throws JSONException
    {
        return json.has( "syn" ) ? json.getJSONArray( "syn" ) : new JSONArray();
    }

    private static String randomChoice( List<String> list )
    {
        if ( list.size() == 0 )
        {
            return "";
        }
        int index = random.nextInt( list.size() );
        return list.get( index );
    }

    private static String stringify( String[] list )
    {
        StringBuilder builder = new StringBuilder();
        for ( String word : list )
        {
            builder.append( word );
        }
        return builder.toString().trim();
    }

    private static List<String> jsonToList( JSONArray array )
        throws JSONException
    {
        List<String> words = new ArrayList<>();
        for ( int i = 0; i < array.length(); i++ )
        {
            words.add( array.getString( i ) );
        }
        return words;
    }

    private static String getContent( String word )
        throws Exception
    {
        String content;
        URLConnection connection;
        connection = new URL( API_URL + word + API_FORMAT ).openConnection();
        connection.addRequestProperty( "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)" );
        try
        {
            Scanner scanner = new Scanner( connection.getInputStream() );
            scanner.useDelimiter( "\\Z" );
            content = scanner.next();
            return content;
        }
        catch ( FileNotFoundException e )
        {
            return "{}";
        }
    }

}
