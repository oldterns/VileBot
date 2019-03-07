package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnswerQuestion
    extends ListenerAdapter
{

    private static final Pattern questionPattern = Pattern.compile( "^!(tellme)\\s(.+)$" );

    private static final String API_KEY = Vilebot.getConfig().get( "wolframKey" );

    private static final int MAX_RESPONSE = 500;

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher tellMeMatcher = questionPattern.matcher( text );

        if ( tellMeMatcher.matches() )
            tellMe( event, tellMeMatcher );
    }

    private void tellMe( GenericMessageEvent event, Matcher matcher )
    {
        String question = matcher.group( 2 );
        String answer = getAnswer( question );
        answer = truncate( answer );
        event.respondWith( answer );
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
            connection = new URL( url ).openConnection();
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
