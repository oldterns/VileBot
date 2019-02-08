package com.oldterns.vilebot.handlers.user;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;

import com.github.lalyos.jfiglet.FigletFont;

@HandlerContainer
public class Ascii
{

    private static final Pattern asciiPattern = Pattern.compile( "^!(ascii)\\s(.+)$" );

    private static final Pattern asciiFontsPattern = Pattern.compile( "!asciifonts" );

    private static final int MAX_CHARS_PER_LINE = 20;

    private static final int MAX_CHARS = MAX_CHARS_PER_LINE * 3; // max 3 lines

    private static final String fontFile = "./files/fonts/%s.flf";

    private static final String fontFileDir = "./files/fonts/";

    private static final List<String> availableFonts = getAvailableFonts();

    @Handler
    public void ascii( ReceivePrivmsg event )
    {

        String text = event.getText();
        Matcher asciiMatch = asciiPattern.matcher( text );

        if ( asciiMatch.matches() )
        {
            // check if first word is a font name
            String font = asciiMatch.group( 2 ).split( " ", 2 )[0];
            String asciiArt;
            if ( availableFonts.contains( font ) )
            {
                String message = asciiMatch.group( 2 ).split( " ", 2 )[1];
                asciiArt = getAsciiArt( message, font );
            }
            else
            {
                String message = asciiMatch.group( 2 );
                asciiArt = getAsciiArt( message );
            }
            event.reply( asciiArt );
        }
    }

    @Handler
    public void asciifonts( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = asciiFontsPattern.matcher( text );

        if ( matcher.matches() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Available fonts for !ascii:\n" );
            for ( int i = 0; i < availableFonts.size(); i++ )
            {
                sb.append( String.format( "%20s ", availableFonts.get( i ) ) );
                if ( ( ( i + 1 ) % 5 ) == 0 )
                {
                    sb.append( "\n" );
                }
            }
            event.replyPrivately( sb.toString() );
        }
    }

    private static List<String> getAvailableFonts()
    {
        List<String> fonts = new ArrayList<>();
        for ( File flf : new File( fontFileDir ).listFiles() )
        {
            if ( flf.isFile() )
            {
                String filename = flf.getName();
                fonts.add( filename.substring( 0, filename.length() - 4 ) );
            }
        }
        Collections.sort( fonts );
        return fonts;
    }

    private String getAsciiArt( String text )
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            String lines[] = splitMessage( text );
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
            String lines[] = splitMessage( text );
            for ( String line : lines )
            {
                sb.append( FigletFont.convertOneLine( String.format( fontFile, font ), line ) );
            }
            return sb.toString();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            return "Could not get ASCII art";
        }
    }

    private String[] splitMessage( String message )
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