package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import net.engio.mbassy.listener.Handler;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eunderhi on 13/08/15.
 */

@HandlerContainer
public class AnswerQuestion
{

    private static final Pattern questionPattern = Pattern.compile( "^!(tellme)\\s(.+)$" );

    private static final String API_KEY = Vilebot.getConfig().get( "wolframKey" );

    private static final int MAX_RESPONSE = 500;

    @Handler
    public void tellMe( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = questionPattern.matcher( text );

        if ( matcher.matches() )
        {
            String question = matcher.group( 2 );
            String answer = getAnswer( question );
            answer = truncate( answer );
            event.reply( answer );
        }
    }

    String getAnswer( String searchTerm )
    {
        try
        {
            String url = makeURL( searchTerm );
            String response = getContent( url );
            String answer = parseResponse( response );
            return answer;
        }
        catch ( Exception e )
        {
            return "I couldn't find an answer for that";
        }
    }

    String makeURL( String searchTerm )
        throws UnsupportedEncodingException
    {
        searchTerm = URLEncoder.encode( searchTerm, "UTF-8" );
        String url = "http://api.wolframalpha.com/v2/query?input=" + searchTerm + "&appid=" + API_KEY
            + "&format=plaintext&output=XML";
        return url;
    }

    String getContent( String url )
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

    String parseResponse( String response )
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
