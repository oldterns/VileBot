package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.vilebot.util.URLFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Scanner;

@ApplicationScoped
public class AnswerQuestionService
{
    private static final int MAX_RESPONSE = 500;

    @Inject
    URLFactory urlFactory;

    @ConfigProperty( name = "vilebot.wolfram.key" )
    String API_KEY;

    @OnMessage( "!tellme @question" )
    public String getAnswerForQuery( String question )
    {
        String answer = getAnswer( question );
        return truncate( answer );
    }

    private String getAnswer( String searchTerm )
    {
        try
        {
            String url = makeURL( searchTerm );
            String response = getContent( url );
            return parseResponse( response );
        }
        catch ( Exception e )
        {
            return "I couldn't find an answer for that";
        }
    }

    private String makeURL( String searchTerm )
        throws UnsupportedEncodingException
    {
        searchTerm = URLEncoder.encode( searchTerm, "UTF-8" );
        return "http://api.wolframalpha.com/v2/query?input=" + searchTerm + "&appid=" + API_KEY
            + "&format=plaintext&output=XML";
    }

    private String getContent( String url )
    {
        String content = null;
        URLConnection connection;
        try
        {
            connection = urlFactory.build( url ).openConnection();
            Scanner scanner = new Scanner( connection.getInputStream() );
            scanner.useDelimiter( "\\Z" );
            content = scanner.next();
        }
        catch ( Exception ex )
        {
            ex.printStackTrace();
        }
        return content;
    }

    private String parseResponse( String response )
        throws Exception
    {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse( new InputSource( new StringReader( response ) ) );
        NodeList nodeList = document.getElementsByTagName( "plaintext" );
        String answer = nodeList.item( 0 ).getTextContent().trim() + "\n" + nodeList.item( 1 ).getTextContent().trim();

        if ( answer.isEmpty() )
        {
            throw new Exception();
        }

        return answer;
    }

    private String truncate( String response )
    {
        if ( response.length() > MAX_RESPONSE )
        {
            response = response.substring( 0, MAX_RESPONSE ) + "...";
        }
        return response;
    }
}
