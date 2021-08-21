package com.oldterns.vilebot.services;

import com.github.lalyos.jfiglet.FigletFont;
import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.irc.bot.annotations.OnPrivateMessage;
import com.oldterns.irc.bot.annotations.Regex;
import com.oldterns.vilebot.util.LimitService;
import com.oldterns.vilebot.util.URLFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kitteh.irc.client.library.element.User;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.LimitExceededException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class AsciiService
{

    private static final int MAX_CHARS_PER_LINE = 20;

    private static final int MAX_CHARS = MAX_CHARS_PER_LINE * 3; // max 3 lines

    Map<String, String> fontNameToDataMap;

    @Inject
    URLFactory urlFactory;

    @Inject
    LimitService limitCommand;

    @ConfigProperty( name = "vilebot.ascii.font-directory", defaultValue = ".fonts" )
    Path fontDirectory;

    @PostConstruct
    public void constructFontMap()
    {
        fontNameToDataMap = new HashMap<>();
        try ( InputStream fontListResource = AsciiService.class.getResourceAsStream( "/fontlist.txt" ) )
        {
            if ( fontListResource == null )
            {
                throw new IOException( "/fontlist.txt does not exist" );
            }
            new BufferedReader( new InputStreamReader( fontListResource ) ).lines().forEach( fontLocation -> {
                try
                {
                    URL url = urlFactory.build( fontLocation );
                    String fontFileName = Path.of( url.getFile() ).getFileName().toString();
                    Path fontFile = fontDirectory.resolve( fontFileName );
                    InputStream fontFileInputStream;
                    if ( Files.exists( fontFile ) )
                    {
                        fontFileInputStream = Files.newInputStream( fontFile );
                    }
                    else
                    {
                        fontFileInputStream = url.openStream();
                    }
                    InputStreamReader inputStreamReader = new InputStreamReader( fontFileInputStream );
                    BufferedReader bufferedReader = new BufferedReader( inputStreamReader );

                    String fontName = fontFileName.substring( 0, fontFileName.length() - 4 );
                    String lines = bufferedReader.lines().collect( Collectors.joining( "\n" ) );
                    fontNameToDataMap.put( fontName, lines );
                    if ( !Files.exists( fontFile ) )
                    {
                        Files.createDirectories( fontFile.getParent() );
                        Files.writeString( fontFile, lines );
                    }
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            } );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Unable to open /fontlist.txt", e );
        }
    }

    @OnChannelMessage( "!ascii @font @message" )
    public String ascii( User user, @Regex( "\\S+" ) String font, String message )
    {
        try
        {
            limitCommand.addUse( user );
            return runAscii( font, message );
        }
        catch ( LimitExceededException e )
        {
            return e.getMessage();
        }
    }

    @OnPrivateMessage( "!ascii @font @message" )
    public String asciiPrivateMessage( @Regex( "\\S+" ) String font, String message )
    {
        return runAscii( font, message );
    }

    @OnMessage( "!asciifonts" )
    public void asciifonts( User user )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "Available fonts for !ascii:\n" );
        int i = 0;
        for ( String font : fontNameToDataMap.keySet().stream().sorted().collect( Collectors.toList() ) )
        {
            sb.append( String.format( "%20s ", font ) );
            if ( ( ( i + 1 ) % 5 ) == 0 )
            {
                sb.append( "\n" );
            }
            i++;
        }
        for ( String line : sb.toString().split( "\n" ) )
        {
            user.sendMessage( line );
        }
    }

    private String runAscii( String font, String message )
    {
        String asciiArt;
        if ( fontNameToDataMap.containsKey( font ) )
        {
            return getAsciiArt( message, font );
        }
        else
        {
            asciiArt = getAsciiArt( font + " " + message );
        }
        return asciiArt;
    }

    private static String getAsciiArt( String text )
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            String[] lines = splitMessage( text );
            for ( String line : lines )
            {
                sb.append( FigletFont.convertOneLine( line ) );
            }
            return sb.toString();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            return "Could not get ASCII art";
        }
    }

    private String getAsciiArt( String text, String font )
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            String[] lines = splitMessage( text );
            for ( String line : lines )
            {
                sb.append( FigletFont.convertOneLine( new ByteArrayInputStream( fontNameToDataMap.get( font ).getBytes() ),
                                                      line ) );
            }
            return sb.toString();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            return "Could not get ASCII art";
        }
    }

    private static String[] splitMessage( String message )
    {
        List<String> lines = new ArrayList<>();
        if ( message.length() > MAX_CHARS )
        {
            message = message.substring( 0, MAX_CHARS );
        }
        int len = message.length();
        for ( int i = 0; i < len; i += MAX_CHARS_PER_LINE )
        {
            int substrEndIdx = Math.min( len, i + MAX_CHARS_PER_LINE );
            if ( i + MAX_CHARS_PER_LINE < len && Character.isLetter( message.charAt( substrEndIdx - 1 ) )
                && Character.isLetter( message.charAt( substrEndIdx ) ) )
            {
                lines.add( message.substring( i, substrEndIdx ) + "-" );
            }
            else
            {
                lines.add( message.substring( i, substrEndIdx ) );
            }
        }
        return lines.toArray( new String[0] );
    }

}