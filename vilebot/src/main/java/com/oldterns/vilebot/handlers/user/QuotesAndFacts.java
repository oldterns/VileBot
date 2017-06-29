/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.oldterns.vilebot.db.ChurchDB;
import com.oldterns.vilebot.db.QuoteFactDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Ignore;

import net.engio.mbassy.listener.Handler;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.interfaces.Replyable;
import ca.szc.keratin.core.event.message.recieve.ReceiveJoin;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import main.java.com.oldterns.vilebot.util.StringUtil;

@HandlerContainer
public class QuotesAndFacts
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );

    private static final Pattern addPattern = Pattern.compile( "^!(fact|quote)add (" + nounPattern + ") (.+)$" );

    private static final Pattern dumpPattern = Pattern.compile( "^!(fact|quote)dump (" + nounPattern + ")\\s*$" );

    private static final Pattern queryPattern = Pattern.compile( "^!(fact|quote) (" + nounPattern + ")\\s*$" );

    private static final Pattern searchPattern = Pattern.compile( "^!(fact|quote)search (" + nounPattern + ") (.*)$" );

    private static final Random random = new Random();

    @Handler
    private void factQuoteAdd( ReceivePrivmsg event )
    {
        Matcher matcher = addPattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String mode = matcher.group( 1 );
            String noun = BaseNick.toBaseNick( matcher.group( 2 ) );
            String text = matcher.group( 3 );
            String sender = BaseNick.toBaseNick( event.getSender() );

            if ( !sender.equals( noun ) ) {
                text = trimChars( text, " '\"" );

                if ( "fact".equals( mode ) )
                {
                    QuoteFactDB.addFact( noun, text );
                    event.reply( formatFactReply( noun, text ) );
                }
                else
                {
                    QuoteFactDB.addQuote( noun, text );
                    event.reply( formatQuoteReply( noun, text ) );
                }
            } else {
                event.reply( StringUtil.capitalizeFirstLetter( mode ) + "s from yourself are both terrible and uninteresting." );
            }
        }
    }

    @Handler
    private void factQuoteDump( ReceivePrivmsg event )
    {
        Matcher matcher = dumpPattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String mode = matcher.group( 1 );
            String queried = BaseNick.toBaseNick( matcher.group( 2 ) );

            if ( "fact".equals( mode ) )
            {
                Set<String> allFacts = QuoteFactDB.getFacts( queried );
                if ( allFacts.isEmpty() )
                {
                    event.replyPrivately( queried + " has no facts." );
                }
                for ( String fact : allFacts )
                {
                    event.replyPrivately( formatFactReply( queried, fact ) );
                }
            }
            else
            {
                Set<String> allQuotes = QuoteFactDB.getQuotes( queried );
                if ( allQuotes.isEmpty() )
                {
                    event.replyPrivately( queried + " has no quotes." );
                }
                for ( String quote : allQuotes )
                {
                    event.replyPrivately( formatFactReply( queried, quote ) );
                }
            }
        }
    }

    @Handler
    private void factQuoteQuery( ReceivePrivmsg event )
    {
        Matcher matcher = queryPattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String mode = matcher.group( 1 );
            String noun = BaseNick.toBaseNick( matcher.group( 2 ) );

            if ( "fact".equals( mode ) )
            {
                if ( !replyWithFact( noun, event ) )
                {
                    event.reply( noun + " has no facts." );
                }
            }
            else
            {
                if ( !replyWithQuote( noun, event ) )
                {
                    event.reply( noun + " has no quotes." );
                }
            }
        }
    }

    @Handler
    private void factQuoteSearch( ReceivePrivmsg event )
    {
        Matcher matcher = searchPattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String mode = matcher.group( 1 );
            String noun = BaseNick.toBaseNick( matcher.group( 2 ) );
            String regex = matcher.group( 3 );

            try
            {
                // Case insensitive added automatically, use (?-i) in a message to reenable case sensitivity
                Pattern pattern = Pattern.compile( "(?i)" + regex );

                if ( "fact".equals( mode ) )
                {
                    Set<String> texts = QuoteFactDB.getFacts( noun );
                    if ( texts != null )
                    {
                        String randomMatch = regexSetSearch( texts, pattern );
                        if ( randomMatch != null )
                        {
                            event.reply( formatFactReply( noun, randomMatch ) );
                        }
                        else
                        {
                            event.reply( noun + " has no matching facts." );
                        }
                    }
                    else
                    {
                        event.reply( noun + " has no facts." );
                    }
                }
                else
                {
                    Set<String> texts = QuoteFactDB.getQuotes( noun );
                    if ( texts != null )
                    {
                        String randomMatch = regexSetSearch( texts, pattern );
                        if ( randomMatch != null )
                        {
                            event.reply( formatQuoteReply( noun, randomMatch ) );
                        }
                        else
                        {
                            event.reply( noun + " has no matching quotes." );
                        }
                    }
                    else
                    {
                        event.reply( noun + " has no quotes." );
                    }
                }
            }
            catch ( PatternSyntaxException e )
            {
                event.reply( "Syntax error in regex pattern" );
            }
        }
    }

    private static String regexSetSearch( Set<String> texts, Pattern pattern )
    {
        List<String> matchingTexts = new LinkedList<String>();

        for ( String text : texts )
        {
            Matcher matcher = pattern.matcher( text );
            if ( matcher.find() )
            {
                matchingTexts.add( text );
            }
        }

        int matchCount = matchingTexts.size();
        if ( matchCount > 0 )
        {
            int selection = random.nextInt( matchCount );
            return matchingTexts.get( selection );
        }
        else
        {
            return null;
        }
    }

    @Handler
    private void announceFactOrQuoteOnJoin( ReceiveJoin event )
    {
        String baseNick = BaseNick.toBaseNick( event.getJoiner() );

        if ( !Ignore.getOnJoin().contains( baseNick ) )
        {
            if ( random.nextBoolean() )
            {
                if ( !replyWithQuote( baseNick, event ) )
                    replyWithFact( baseNick, event );
            }
            else
            {
                if ( !replyWithFact( baseNick, event ) )
                    replyWithQuote( baseNick, event );
            }
        }
    }

    private static boolean replyWithFact( String noun, Replyable event )
    {
        String text = QuoteFactDB.getRandFact( noun );
        if ( text != null )
        {
            if ( ChurchDB.getDonorRank( noun ) != null && ChurchDB.getDonorRank( noun ) < 4 )
            {
                String title = ChurchDB.getDonorTitle( noun );
                if ( title.trim().length() > 0 )
                {
                    noun = title;
                }
            }
            event.reply( formatFactReply( noun, text ) );
            return true;
        }
        return false;
    }

    private static String formatFactReply( String noun, String fact )
    {
        return noun + " " + fact;
    }

    private static boolean replyWithQuote( String noun, Replyable event )
    {
        String text = QuoteFactDB.getRandQuote( noun );
        if ( text != null )
        {
            if ( ChurchDB.getDonorRank( noun ) != null && ChurchDB.getDonorRank( noun ) < 4 )
            {
            String title = ChurchDB.getDonorTitle( noun );
                if ( title.trim().length() > 0 )
                {
                    noun = title;
                }
            }
            event.reply( formatQuoteReply( noun, text ) );
            return true;
        }
        return false;
    }

    private static String formatQuoteReply( String noun, String quote )
    {
        return noun + " once said, \"" + quote + "\".";
    }

    /**
     * Removes all specified leading and trailing characters in the array charsToRemove.
     * 
     * @param input The string to process
     * @param charsToRemove All characters to remove, treated as a set
     * @return A copy of the input String with the characters removed
     * @see String.trim()
     */
    private static String trimChars( String input, String charsToRemove )
    {
        char[] value = input.toCharArray();
        char[] rmChars = charsToRemove.toCharArray();
        Arrays.sort( rmChars );

        int len = value.length;
        int st = 0;

        while ( ( st < len ) && ( Arrays.binarySearch( rmChars, value[st] ) >= 0 ) )
        {
            st++;
        }
        while ( ( st < len ) && ( Arrays.binarySearch( rmChars, value[len - 1] ) >= 0 ) )
        {
            len--;
        }

        return new String( input.substring( st, len ) );
    }
}
