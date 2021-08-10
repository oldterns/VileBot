package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.util.URLFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@ApplicationScoped
public class GetInfoOnService
{

    @Inject
    URLFactory urlFactory;

    @OnMessage( "!infoon @query" )
    public String getInfoOn( String query )
    {
        String queryModifier = " site:wikipedia.org";
        return getWiki( query, queryModifier );
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
        connection = urlFactory.build( url ).openConnection();
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
