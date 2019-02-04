package com.oldterns.vilebot.handlers.user;

import java.io.IOException;
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

    @Handler
    public void ascii( ReceivePrivmsg event )
    {

        String text = event.getText();
        Matcher asciiMatch = asciiPattern.matcher( text );

        if ( asciiMatch.matches() )
        {
            String asciiArt = getAsciiArt( asciiMatch.group( 2 ) );
            event.reply( asciiArt );
        }

    }

    private String getAsciiArt( String text )
    {
        try
        {
            String asciiArt = FigletFont.convertOneLine( text );
            return asciiArt;
        }
        catch ( IOException e )
        {
            return "Could not get ASCII art";
        }
    }

}