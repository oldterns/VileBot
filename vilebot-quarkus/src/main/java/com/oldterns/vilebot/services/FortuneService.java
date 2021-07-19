package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnChannelMessage;
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
public class FortuneService
{
    @Inject
    RandomProvider randomProvider;

    List<String> fortunes = loadFortunes();

    @OnChannelMessage( "!fortune" )
    public String onFortune()
    {
        return randomProvider.getRandomElement( fortunes );
    }

    @OnChannelMessage( "!fortunedirty" )
    public String onDirtyFortune()
    {
        return "oooo you dirty";
    }

    private List<String> loadFortunes()
    {
        try ( InputStream fortuneListResource = FortuneService.class.getResourceAsStream( "/fortunelist.txt" ) )
        {
            if ( fortuneListResource == null )
            {
                throw new IOException( "Unable to find fortunelist.txt" );
            }
            return new BufferedReader( new InputStreamReader( fortuneListResource ) ).lines().collect( Collectors.toList() );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "Unable to open fortunelist.txt" );
        }
    }

}
