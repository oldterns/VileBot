package com.oldterns.vilebot.util;

import javax.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@ApplicationScoped
public class RandomProvider
{
    final SecureRandom random = new SecureRandom();

    public Random getRandom()
    {
        return random;
    }

    public boolean getRandomBoolean()
    {
        return random.nextBoolean();
    }

    public int getRandomInt( int min, int max )
    {
        return getRandomInt( max - min + 1 ) + min;
    }

    public int getRandomInt( int bound )
    {
        return random.nextInt( bound );
    }

    public <T> T getRandomElement( List<T> list )
    {
        return list.get( getRandomInt( list.size() ) );
    }

    public <T> T getRandomElement( T[] array )
    {
        return array[getRandomInt( array.length )];
    }

    public <T> List<T> shuffleList( List<T> list )
    {
        List<T> out = new ArrayList<>( list );
        Collections.shuffle( out );
        return out;
    }
}
