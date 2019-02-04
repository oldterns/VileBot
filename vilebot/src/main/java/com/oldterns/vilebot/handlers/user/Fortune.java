package com.oldterns.vilebot.handlers.user;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.Vilebot;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;

/**
 * Created by ipun on 15/05/16.
 */
@HandlerContainer
public class Fortune
{
    private static final Pattern FORTUNE_PATTERN = Pattern.compile( "^!fortune(.*)" );

    private static final String FORTUNE_LIST_PATH = Vilebot.getConfig().get( "FortuneList" );

    private ArrayList<String> fortune = loadFortunes();

    private static final String DIRTY_ARG = "dirty";

    @Handler
    public void fortune( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher fortuneMatcher = FORTUNE_PATTERN.matcher( text );
        try
        {
            if ( fortuneMatcher.matches() )
            {
                String dirty = fortuneMatcher.group( 1 );
                if ( dirty == null || dirty.isEmpty() )
                {
                    fortuneReply( event );
                }
                if ( dirty.equals( DIRTY_ARG ) )
                {
                    event.reply( "oooo you dirty" );
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
    }

    private void fortuneReply( ReceivePrivmsg event )
    {
        String randomFortune = fortune.get( new Random().nextInt( fortune.size() ) );
        event.reply( randomFortune );
    }

    private ArrayList<String> loadFortunes()
    {
        try
        {
            ArrayList<String> fortunes = new ArrayList<>();
            List<String> lines = Files.readAllLines( Paths.get( FORTUNE_LIST_PATH ), Charset.forName( "UTF-8" ) );
            for ( String line : lines )
            {
                fortunes.add( line );
            }
            return fortunes;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
        return null;
    }
}
