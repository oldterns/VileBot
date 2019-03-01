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
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

/**
 * Created by ipun on 15/05/16.
 */
// @HandlerContainer
public class Inspiration
    extends ListenerAdapter
{
    private static final Pattern FORTUNE_PATTERN = Pattern.compile( "^!inspiration(.*)" );

    private static final String FORTUNE_LIST_PATH = Vilebot.getConfig().get( "InspirationList" );

    private static final String FORTUNE_INDEX_PATH = Vilebot.getConfig().get( "InspirationIndex" );

    private ArrayList<String> inspiration = loadInspirations();

    private List<String> inspirationIndex = loadInspirationIndex();

    // @Handler
    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
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

    private void inspirationReply( GenericMessageEvent event )
    {
        int index = Integer.parseInt( inspirationIndex.get( new Random().nextInt( inspirationIndex.size() - 1 ) ) );
        String line = inspiration.get( index );
        while ( !line.matches( "%" ) )
        {
            event.respondWith( line );
            line = inspiration.get( ++index );
        }
    }

    private ArrayList<String> loadInspirations()
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
