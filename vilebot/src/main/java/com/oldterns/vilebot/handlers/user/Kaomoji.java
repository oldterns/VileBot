package com.oldterns.vilebot.handlers.user;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;
import twitter4j.JSONObject;

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ltulloch on 07/24/18.
 */
public class Kaomoji
    extends ListenerAdapter
{

    private static final String API_URL = "https://jckcthbrt.stdlib.com/kaomoji/?search=";

    private static final String API_FORMAT = "/json";

    private static final Pattern kaomojiPattern = Pattern.compile( "^!kaomoji (.+)" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        Matcher questionMatcher = kaomojiPattern.matcher( event.getMessage() );
        if ( questionMatcher.matches() )
        {
            String message = questionMatcher.group( 1 );
            try
            {
                message = kaomojiify( message );
                event.respondWith( message );
            }
            catch ( Exception e )
            {
                event.respondWith( "ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)" );
                e.printStackTrace();
            }
        }
    }

    private String kaomojiify( String message )
        throws Exception
    {
        String[] words = splitWords( message );
        String kaomoji = "";
        if ( words.length <= 0 )
        {
            return "ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)";
        }
        switch ( words[0] )
        {
            case "wat":
                words[0] = "confused";
                break;
            case "nsfw":
            case "wtf":
                kaomoji = "ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)";
                break;
            case "vilebot":
                kaomoji = "( ͡° ͜ʖ ͡° )";
                break;
            default:
                kaomoji = getKaomoji( words[0] );
                break;
        }

        if ( kaomoji.isEmpty() )
        {
            kaomoji = "щ(ಥдಥщ)";
        }
        return kaomoji;
    }

    private String[] splitWords( String message )
    {
        return message.split( "\\b" );
    }

    private String getKaomoji( String word )
        throws Exception
    {
        String emoji = "";
        JSONObject json = new JSONObject( getContent( word ) );
        if ( json.has( "success" ) )
        {
            emoji = json.getString( "emoji" );
        }
        return emoji;
    }

    private String getContent( String word )
        throws Exception
    {
        String content;
        URLConnection connection;
        connection = new URL( API_URL + word ).openConnection();
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
