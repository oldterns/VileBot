package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.vilebot.util.RandomProvider;
import com.oldterns.vilebot.util.URLFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@ApplicationScoped
public class JazizService
{
    @Inject
    RandomProvider randomProvider;

    @Inject
    URLFactory urlFactory;

    @ConfigProperty( name = "vilebot.jaziz.thesaurus-key" )
    String API_KEY;

    private static final String API_URL = "http://words.bighugelabs.com/api/2/";

    private static final String API_FORMAT = "/json";

    private static String[] splitWords( String message )
    {
        return message.split( "\\b" );
    }

    @OnChannelMessage( "!jaziz @message" )
    public String jazizify( String message )
    {
        String[] words = splitWords( message );
        for ( int i = 0; i < words.length; i++ )
        {
            if ( !words[i].contains( " " ) )
            {
                String replacement;
                try
                {
                    replacement = words[i].length() > 3 ? randomChoice( getSynonyms( words[i] ) ) : words[i];
                }
                catch ( Exception e )
                {
                    replacement = "";
                }
                if ( !replacement.isEmpty() )
                {
                    words[i] = replacement;
                }
            }
        }
        return stringify( words );
    }

    private List<String> getSynonyms( String word )
        throws Exception
    {
        JSONObject json = new JSONObject( getContent( word ) );

        List<String> synonyms = new ArrayList<>();
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

    private JSONArray getSyns( JSONObject json )
        throws JSONException
    {
        return json.has( "syn" ) ? json.getJSONArray( "syn" ) : new JSONArray();
    }

    private String randomChoice( List<String> list )
    {
        if ( list.size() == 0 )
        {
            return "";
        }
        return randomProvider.getRandomElement( list );
    }

    private String stringify( String[] list )
    {
        StringBuilder builder = new StringBuilder();
        for ( String word : list )
        {
            builder.append( word );
        }
        return builder.toString().trim();
    }

    private List<String> jsonToList( JSONArray array )
        throws JSONException
    {
        List<String> words = new ArrayList<>();
        for ( int i = 0; i < array.length(); i++ )
        {
            words.add( array.getString( i ) );
        }
        return words;
    }

    private String getContent( String word )
        throws Exception
    {
        String content;
        URLConnection connection;
        connection = urlFactory.build( API_URL + API_KEY + "/" + word + API_FORMAT ).openConnection();
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
