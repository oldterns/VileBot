package com.oldterns.vilebot.handlers.user;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetInfoOn
    extends ListenerAdapter
{

    private static final Pattern questionPattern = Pattern.compile( "^!(infoon)\\s(.+)$" );

    private static final Pattern dumbQuestionPattern = Pattern.compile( "^!(infun)\\s(.+)$" );

    @Override
    public void onGenericMessage( GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher infoonMatch = questionPattern.matcher( text );

        if ( infoonMatch.matches() )
        {
            String question = infoonMatch.group( 2 );
            String queryModifier = " site:wikipedia.org";
            String answer = getWiki( question, queryModifier );
            for ( String line : answer.split( "\n" ) )
            {
                event.respondWith( line );
            }
        }
    }

    private String getWiki( String query, String queryModifier )
    {
        try
        {
            String wikiURL = getWikiURLFromGoogle( query + queryModifier );
            String wikipediaContent = getContent( wikiURL );
            return parseResponse( wikipediaContent );
        }
        catch ( Exception e )
        {
            return "Look, I don't know.";
        }
    }

    private String getWikiURLFromGoogle( String query )
        throws Exception
    {
        String googleURL = makeGoogleURL( query );
        String googleResponse = getContent( googleURL );
        return getWikiLink( googleResponse );
    }

    private String makeGoogleURL( String query )
        throws Exception
    {
        query = encode( query );
        return "https://www.google.com/search?q=" + query;
    }

    private String getWikiLink( String googleHTML )
    {
        Document doc = Jsoup.parse( googleHTML );
        Element link = doc.select( "a[href*=/url?q=https://en.wikipedia]" ).first();
        return link.attr( "href" ).replace( "/url?q=", "" ).split( "&" )[0];
    }

    private String getContent( String url )
        throws Exception
    {
        String content;
        URLConnection connection;
        connection = new URL( url ).openConnection();
        connection.addRequestProperty( "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)" );
        Scanner scanner = new Scanner( connection.getInputStream() );
        scanner.useDelimiter( "\\Z" );
        content = scanner.next();
        return content;
    }

    private String parseResponse( String response )
        throws Exception
    {
        Document doc = Jsoup.parse( response );
        Element bodyDiv = doc.getElementById( "mw-content-text" );
        Element firstParagraph = bodyDiv.getElementsByTag( "p" ).first();
        String answer = firstParagraph.text();
        if ( answer.isEmpty() )
        {
            throw new Exception();
        }
        answer = answer.replaceAll( "\\[[0-9]+\\]", "" );
        return answer;
    }

    private String encode( String string )
        throws Exception
    {
        return URLEncoder.encode( string, StandardCharsets.UTF_8 );
    }
}
