package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.vilebot.util.RandomProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ipun on 15/05/16.
 */
@ApplicationScoped
public class InspirationService
{
    @Inject
    RandomProvider randomProvider;

    List<String> inspiration = loadInspirations();

    List<Integer> inspirationIndex = loadInspirationIndex();

    @OnChannelMessage( "!inspiration" )
    public String onInspiration()
    {
        int index = randomProvider.getRandomElement( inspirationIndex );

        StringBuilder reply = new StringBuilder();
        String line = inspiration.get( index );
        while ( !line.matches( "%" ) )
        {
            reply.append( line );
            reply.append( '\n' );
            line = inspiration.get( ++index );
        }
        reply.deleteCharAt( reply.length() - 1 );
        return reply.toString();
    }

    private List<String> loadInspirations()
    {
        try ( InputStream inspirationIndexResource =
            InspirationService.class.getResourceAsStream( "/inspirationslist.txt" ) )
        {
            if ( inspirationIndexResource == null )
            {
                throw new IOException( "Unable to find inspirationslist.txt" );
            }
            return new BufferedReader( new InputStreamReader( inspirationIndexResource ) ).lines().collect( Collectors.toList() );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Unable to open inspirationslist.txt" );
        }
    }

    private List<Integer> loadInspirationIndex()
    {
        try ( InputStream inspirationIndexResource =
            InspirationService.class.getResourceAsStream( "/inspirationsindex.txt" ) )
        {
            if ( inspirationIndexResource == null )
            {
                throw new IOException( "Unable to find inspirationsindex.txt" );
            }
            return new BufferedReader( new InputStreamReader( inspirationIndexResource ) ).lines().map( Integer::parseInt ).collect( Collectors.toList() );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Unable to open inspirationsindex.txt" );
        }
    }

}
