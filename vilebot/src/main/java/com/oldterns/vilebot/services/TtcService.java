package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.vilebot.util.URLFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.stream.Collectors;

@ApplicationScoped
public class TtcService
{

    public static final String TTC_URL = "https://www.ttc.ca/Service_Advisories/all_service_alerts.jsp";

    @Inject
    URLFactory urlFactory;

    @OnChannelMessage( "!ttc" )
    public String printAlerts()
    {
        try
        {
            return parseContent( getContent() ).stream().map( Element::text ).collect( Collectors.joining( "\n" ) );
        }
        catch ( Exception e )
        {
            return "Unable to retrieve alerts.";
        }
    }

    private Elements parseContent( String content )
    {
        Elements alerts = new Elements();
        Document doc = Jsoup.parse( content );
        Elements alertDivs = doc.select( "div[class=alert-content]" );
        for ( Element element : alertDivs )
        {
            if ( !element.text().toLowerCase().contains( "elevator" ) )
            {
                alerts.addAll( element.select( "p[class=veh-replace]" ) );
            }
        }
        return alerts;
    }

    private String getContent()
        throws Exception
    {
        String content;
        URLConnection connection;
        connection = urlFactory.build( TTC_URL ).openConnection();
        connection.addRequestProperty( "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)" );
        Scanner scanner = new Scanner( connection.getInputStream() );
        scanner.useDelimiter( "\\Z" );
        content = scanner.next();
        return content;
    }
}
