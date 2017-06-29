/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.ChurchDB;
import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Ignore;

import com.oldterns.vilebot.util.Sessions;
import net.engio.mbassy.listener.Handler;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.interfaces.Replyable;
import ca.szc.keratin.core.event.message.recieve.ReceiveJoin;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class Karma
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );

    private static final Pattern nickBlobPattern = Pattern.compile( "(?:(" + nounPattern + "?)(?:, +| +|$))" );

    private static final Pattern incBlobPattern = Pattern.compile( "(?:(" + nounPattern + "?\\+\\+)(?:, +| +|$))" );

    private static final Pattern decBlobPattern = Pattern.compile( "(?:(" + nounPattern + "?--)(?:, +| +|$))" );

    private static final Pattern incrementPattern = Pattern.compile( "(?:^|^.*\\s+)(" + incBlobPattern + "+)(?:.*|$)" );

    private static final Pattern decrementPattern = Pattern.compile( "(?:^|^.*\\s+)(" + decBlobPattern + "+)(?:.*|$)" );
    // The opening (?:^|^.*\\s+) and closing (?:.*|$) are needed when only part of the message is ++ or -- events

    private static final Pattern selfKarmaQueryPattern = Pattern.compile( "^\\s*!(rev|)rank\\s*$" );

    private static final Pattern karmaQueryPattern = Pattern.compile( "!(rev|)rank (" + nickBlobPattern + "+)" );

    private static final Pattern ranknPattern = Pattern.compile( "!(rev|)rankn ([0-9]+)\\s*" );

    private static final Pattern topBottomThreePattern = Pattern.compile( "!(top|bottom)three\\s*" );

    private static final Pattern removePattern = Pattern.compile( "!admin unrank (" + nounPattern + ")\\s*" );

    private static final Pattern totalPattern = Pattern.compile( "^!total" );

    @AssignedBot
    private KeratinBot bot;

    @Handler
    private void announceKarmaOnJoin( ReceiveJoin event )
    {
        String noun = BaseNick.toBaseNick( event.getJoiner() );

        if ( !Ignore.getOnJoin().contains( noun ) )
            replyWithRankAndKarma( noun, event, false, false, true);
    }

    @Handler
    private void karmaInc( ReceivePrivmsg event )
    {
        // Match any string that has at least one word that ends with ++
        Matcher incMatcher = incrementPattern.matcher( event.getText() );

        if ( incMatcher.matches() )
        {
            if (isPrivate(event)) {
                KarmaDB.modNounKarma(event.getSender(), -1);
                return;
            }

            // If one match is found, take the entire text of the message (group(0)) and check each word
            // This is needed in the case that only part of the message is karma events (ie "wow anestico++")
            String wordBlob = incMatcher.group( 0 );
            String sender = BaseNick.toBaseNick( event.getSender() );

            Set<String> nicks = new HashSet<String>();
            Matcher nickMatcher = incBlobPattern.matcher( wordBlob );
            while ( nickMatcher.find() )
            {
                nicks.add( BaseNick.toBaseNick( nickMatcher.group( 1 ) ) );
            }

            boolean insult = false;

            for ( String nick : nicks )
            {
                if ( !nick.equals( sender ) )
                    KarmaDB.modNounKarma( nick, 1 );
                else
                    insult = true;
            }

            if ( insult )
                // TODO insult generator?
                event.reply( "I think I'm supposed to insult you now." );
        }
    }

    @Handler
    private void karmaDec( ReceivePrivmsg event )
    {
        // Match any string that has at least one word that ends with --
        Matcher decMatcher = decrementPattern.matcher( event.getText() );

        if ( decMatcher.matches() )
        {
            if (isPrivate(event)) {
                KarmaDB.modNounKarma(event.getSender(), -1);
                return;
            }
            // If one match is found, take the entire text of the message (group(0)) and check each word
            String wordBlob = decMatcher.group( 0 );

            List<String> nicks = new LinkedList<String>();
            Matcher nickMatcher = decBlobPattern.matcher( wordBlob );
            while ( nickMatcher.find() )
            {
                nicks.add( BaseNick.toBaseNick( nickMatcher.group( 1 ) ) );
            }

            boolean insult = false;

            for ( String nick : nicks )
            {
                if ( !nick.equals( bot.getNick() ) )
                    KarmaDB.modNounKarma( nick, -1 );
                else
                    insult = true;
            }

            if ( insult )
                // TODO insult generator?
                event.reply( "I think I'm supposed to insult you now." );
        }
    }

    private boolean isPrivate(ReceivePrivmsg event) {
        try {
            bot.getChannel(event.getChannel()).getNicks();
        }
        catch (Exception e) {
            return true;
        }
        return false;
    }

    @Handler
    private void karmaQuery( ReceivePrivmsg event )
    {
        Matcher specificMatcher = karmaQueryPattern.matcher( event.getText() );
        Matcher selfMatcher = selfKarmaQueryPattern.matcher( event.getText() );

        if ( specificMatcher.matches() )
        {
            String mode = specificMatcher.group( 1 );
            String nickBlob = specificMatcher.group( 2 );

            List<String> nicks = new LinkedList<String>();
            Matcher nickMatcher = nickBlobPattern.matcher( nickBlob );
            while ( nickMatcher.find() )
            {
                nicks.add( nickMatcher.group( 1 ) );
            }

            boolean reverse = "rev".equals( mode );

            for ( String nick : nicks )
            {
                if (!replyWithRankAndKarma(nick, event, reverse))
                    event.reply(nick + " has no karma.");
            }
        }
        else if ( selfMatcher.matches() )
        {
            String mode = selfMatcher.group( 1 );
            String noun = BaseNick.toBaseNick( event.getSender() );

            boolean reverse = "rev".equals( mode );
            if ( !replyWithRankAndKarma( noun, event, reverse ) )
                event.reply( noun + " has no karma." );
        }
    }

    @Handler
    private void rankNumber( ReceivePrivmsg event )
    {
        Matcher matcher = ranknPattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String mode = matcher.group( 1 );
            String place = matcher.group( 2 );

            boolean reverse = "rev".equals( mode );

            String noun;
            if ( reverse )
                noun = KarmaDB.getRevRankNoun( Long.parseLong( place ) );
            else
                noun = KarmaDB.getRankNoun( Long.parseLong( place ) );

            if ( noun != null )
                replyWithRankAndKarma( noun, event, reverse );
            else
                event.reply( "No noun at that rank." );
        }
    }

    @Handler
    private void totalKarma( ReceivePrivmsg event )
    {
        Matcher matcher = totalPattern.matcher( event.getText() );
        if (matcher.matches()) {
            event.reply("" + KarmaDB.getTotalKarma());
        }
    }

    @Handler
    private void topBottomThree( ReceivePrivmsg event )
    {
        Matcher matcher = topBottomThreePattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String mode = matcher.group( 1 );

            Set<String> nouns = null;
            if ( "top".equals( mode ) )
            {
                nouns = KarmaDB.getRankNouns( 0, 2 );
            }
            else if ( "bottom".equals( mode ) )
            {
                nouns = KarmaDB.getRevRankNouns( 0, 2 );
            }

            if ( nouns != null && nouns.size() > 0 )
            {
                for ( String noun : nouns )
                {
                    replyWithRankAndKarma( noun, event, false, true );
                }
            }
            else
            {
                event.reply( "No nouns at ranks 1 to 3." );
            }
        }
    }

    @Handler
    private void unrank( ReceivePrivmsg event )
    {
        // Admin-only command: remove all of a user's karma.
        Matcher matcher = removePattern.matcher( event.getText() );
        String username = Sessions.getSession( event.getSender() );

        if ( matcher.matches() && GroupDB.isAdmin(username) )
        {
            String noun = BaseNick.toBaseNick( matcher.group( 1 ) );

            if ( replyWithRankAndKarma( noun, event ) )
            {
                event.reply( "Removing " + noun + "." );
                KarmaDB.remNoun( noun );
            }
            else
            {
                event.reply( noun + " isn't ranked." );
            }
        }
    }

    private static boolean replyWithRankAndKarma( String noun, Replyable event )
    {
        return replyWithRankAndKarma( noun, event, false );
    }

    private static boolean replyWithRankAndKarma( String noun, Replyable event, boolean reverseOrder )
    {
        return replyWithRankAndKarma( noun, event, reverseOrder, false );
    }

    private static boolean replyWithRankAndKarma( String noun, Replyable event, boolean reverseOrder,
            boolean obfuscateNick )
    {
        return replyWithRankAndKarma( noun, event, reverseOrder, obfuscateNick, false );
    }
    private static boolean replyWithRankAndKarma( String noun, Replyable event, boolean reverseOrder,
                                                  boolean obfuscateNick, boolean useTitle )
    {
        Integer nounRank;
        if ( reverseOrder )
            nounRank = KarmaDB.getNounRevRank( noun );
        else
            nounRank = KarmaDB.getNounRank( noun );

        Integer nounKarma = KarmaDB.getNounKarma( noun );

        if ( useTitle && ChurchDB.getDonorRank( noun ) != null && ChurchDB.getDonorRank( noun ) < 4 )
        {
            String title = ChurchDB.getDonorTitle( noun );
            if ( title.trim().length() > 0 )
            {
                noun = title;
            }
        }

        if ( nounKarma != null )
        {
            StringBuilder sb = new StringBuilder();

            sb.append( noun );
            if ( obfuscateNick )
                sb.reverse();

            sb.append( " is " );
            sb.append( "ranked at " );

            if ( reverseOrder )
                sb.append( "(reverse) " );

            sb.append( "#" );
            sb.append( nounRank );
            sb.append( " with " );
            sb.append( nounKarma );
            sb.append( " points of karma." );

            event.reply( sb.toString() );
            return true;
        }
        return false;
    }
}
