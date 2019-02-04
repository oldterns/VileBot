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
public class Inspiration
{
    private static final Pattern FORTUNE_PATTERN = Pattern.compile( "^!inspiration(.*)" );

    private static final String FORTUNE_LIST_PATH = Vilebot.getConfig().get( "InspirationList" );

    private static final String FORTUNE_INDEX_PATH = Vilebot.getConfig().get( "InspirationIndex" );

    private ArrayList<String> inspiration = loadInspirations();

    private List<String> inspirationIndex = loadInspirationIndex();

    @Handler
    public void inspiration( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher inspirationMatcher = FORTUNE_PATTERN.matcher( text );
        try
        {
            if ( inspirationMatcher.matches() )
            {
                String dirty = inspirationMatcher.group( 1 );
                if ( dirty == null || dirty.isEmpty() )
                {
                    inspirationReply( event );
                }

            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
    }

    private void inspirationReply( ReceivePrivmsg event )
    {
        int index = Integer.parseInt( inspirationIndex.get( new Random().nextInt( inspirationIndex.size() - 1 ) ) );
        String line = inspiration.get( index );
        while ( !line.matches( "%" ) )
        {
            event.reply( line );
            line = inspiration.get( ++index );
        }
    }

    private ArrayList<String> loadInspirations()
    {
        try
        {
            ArrayList<String> inspirations = new ArrayList<>();
            List<String> lines = Files.readAllLines( Paths.get( FORTUNE_LIST_PATH ), Charset.forName( "UTF-8" ) );
            for ( String line : lines )
            {
                inspirations.add( line );
            }
            return inspirations;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
        return null;
    }

    private List<String> loadInspirationIndex()
    {
        try
        {
            String lines = new String( Files.readAllBytes( Paths.get( FORTUNE_INDEX_PATH ) ) );
            return Arrays.asList( lines.split( "\n" ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
        return null;
    }

}
