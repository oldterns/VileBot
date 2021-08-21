package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.Regex;
import com.oldterns.vilebot.util.URLFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.regex.Pattern;

@ApplicationScoped
public class UrlTitleAnnouncerService
{
    private static final String URL_PATTERN =
        "((?:http|https)://(?:www.|)(?:(?:abstrusegoose|xkcd)\\.com|youtube\\.(?:com|ca)|youtu\\.be)[^ ]*)";

    private static final Pattern titlePattern = Pattern.compile( "<title>(.*)</title>" );

    @Inject
    URLFactory urlFactory;

    @OnChannelMessage( "@url" )
    public String getUrlTitle( @Regex( URL_PATTERN ) String url )
    {
        String title = scrapeURLHTMLTitle( url );
        return "'" + title + "'";
    }

    /**
     * Accesses the source of a HTML page and looks for a title element
     *
     * @param url http URI String
     * @return String of text between the first <title> tag group on the page, empty if error.
     */
    private String scrapeURLHTMLTitle( String url )
    {
        String title = "";

        try
        {
            Document doc = Jsoup.parse( urlFactory.build( url ), 5000 );
            title = doc.title();
        }
        catch ( IOException x )
        {
            System.err.format( "scrapeURLHTMLTitle BufferedReader error: %s%n", x );
        }

        return title;
    }
}
