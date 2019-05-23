/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseNick
{
    // This would grab the leading x out of a nick that actually contains it
    private static Pattern nickPattern = Pattern.compile( "(?:x|)([a-zA-Z0-9]+)(?:\\p{Punct}+[a-zA-Z0-9]*|)" );

    private static String primaryBotNick;

    private static Set<String> allBotNicks = new HashSet<>();

    public static String toBaseNick( String nick )
    {
        if ( allBotNicks.contains( nick ) )
        {
            nick = primaryBotNick;
        }
        else
        {
            Matcher nickMatcher = nickPattern.matcher( nick );

            if ( nickMatcher.find() )
            {
                return nickMatcher.group( 1 );
            }
        }
        return nick;
    }

    public static String getPrimaryBotNick()
    {
        return primaryBotNick;
    }

    public static void setPrimaryBotNick( String primaryNick )
    {
        primaryBotNick = primaryNick;
    }

    public static Set<String> getBotNicks()
    {
        return allBotNicks;
    }

    public static void addBotNick( String nick )
    {
        allBotNicks.add( nick );
    }
}
