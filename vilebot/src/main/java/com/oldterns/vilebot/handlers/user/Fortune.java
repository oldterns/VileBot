package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ipun on 15/05/16.
 */
public class Fortune
    extends ListenerAdapter
{
    private static final Pattern FORTUNE_PATTERN = Pattern.compile( "^!fortune(.*)" );

    private static final String FORTUNE_LIST_PATH = Vilebot.getConfig().get( "FortuneList" );

    private ArrayList<String> fortune = loadFortunes();

    private static final String DIRTY_ARG = "dirty";

    @Override
    public void onGenericMessage( GenericMessageEvent event )
    {
        String text = event.getMessage();
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
                else if ( dirty.equals( DIRTY_ARG ) )
                {
                    event.respondWith( "oooo you dirty" );
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
    }

    private void fortuneReply( GenericMessageEvent event )
    {
        String randomFortune = fortune.get( new Random().nextInt( fortune.size() ) );
        event.respondWith( randomFortune );
    }

    private ArrayList<String> loadFortunes()
    {
        try
        {
            List<String> lines = Files.readAllLines( Paths.get( FORTUNE_LIST_PATH ), Charset.forName( "UTF-8" ) );
            return new ArrayList<>( lines );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
        return null;
    }
}
