/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.annotations.Delimiter;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.irc.bot.annotations.Regex;
import com.oldterns.vilebot.util.RandomProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DecideService
{
    @Inject
    RandomProvider randomProvider;

    @OnMessage( "!decide ?@maybeNoun ?@maybePrefix @choices" )
    public String onDecision( @Regex( "\\[.*\\]" ) Optional<String> maybeNoun,
                              @Regex( "\\{.*\\}" ) Optional<String> maybePrefix,
                              @Delimiter( "\\|" ) List<String> choices )
    {
        String noun = maybeNoun.orElse( "you" );
        if ( !noun.equals( "you" ) )
        {
            noun = noun.substring( 1, noun.length() - 1 );
        }

        String prefix = maybePrefix.orElse( "" );

        if ( prefix.length() >= 2 )
        {
            prefix = prefix.substring( 1, prefix.length() - 1 ).concat( " " );
        }

        StringBuilder sb = new StringBuilder();

        if ( choices.size() == 1 )
        {
            sb.append( "If you already know what you want to do, what are you bugging me for? You should " );
        }
        else
        {
            sb.append( "I think " + noun + " should " );
        }

        sb.append( prefix );
        sb.append( randomProvider.getRandomElement( choices ).trim() );
        sb.append( ". Go do that now." );

        return sb.toString();
    }
}
