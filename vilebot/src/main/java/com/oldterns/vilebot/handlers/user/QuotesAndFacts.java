/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
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
import com.oldterns.vilebot.util.StringUtil;

@HandlerContainer
public class QuotesAndFacts
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );

    private static final Pattern addPattern = Pattern.compile( "^!(fact|quote)add (" + nounPattern + ") (.+)$" );

    private static final Pattern dumpPattern =
        Pattern.compile( "^!(fact|quote)dump (" + nounPattern + ")((?: --page)?)((?:\\d*)?)((?: overwrite)?)\\s*$" );

    private static final Pattern randomPattern = Pattern.compile( "^!(fact|quote)random5 (" + nounPattern + ")\\s*$" );

    private static final Pattern numPattern = Pattern.compile( "^!(fact|quote)number (" + nounPattern + ")\\s*$" );

    private static final Pattern queryPattern = Pattern.compile( "^!(fact|quote) (" + nounPattern + ")\\s*$" );

    private static final Pattern searchPattern = Pattern.compile( "^!(fact|quote)search (" + nounPattern + ") (.*)$" );

    private static final Random random = new Random();

    private static final String DUMP_FOLDER = "dump_folder";

    private static final File DUMP_FOLDER_FILE = new File( DUMP_FOLDER );

    private static final double LINES_PER_PAGE = 5.0;

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

            if ( !sender.equals( noun ) )
            {
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
            }
            else
            {
                event.reply( StringUtil.capitalizeFirstLetter( mode )
                    + "s from yourself are both terrible and uninteresting." );
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
            String pageString = matcher.group( 4 );
            String overWrite = matcher.group( 5 );
            String sender = BaseNick.toBaseNick( event.getSender() );

            Integer page = 1;

            Set<String> allFactsOrQuotes;

            if ( "fact".equals( mode ) )
            {
                allFactsOrQuotes = QuoteFactDB.getFacts( queried );
                if ( allFactsOrQuotes.isEmpty() )
                {
                    event.replyPrivately( queried + " has no facts." );
                }
            }
            else
            {
                allFactsOrQuotes = QuoteFactDB.getQuotes( queried );
                if ( allFactsOrQuotes.isEmpty() )
                {
                    event.replyPrivately( queried + " has no quotes." );
                }
            }

            if ( !pageString.equals( "" ) )
            {
                page = Integer.parseInt( pageString );
            }

            saveToFile( allFactsOrQuotes, queried, sender, mode, event, overWrite );
            getQuoteFactPage( page, queried, sender, mode, event );
        }
    }

    @Handler
    private void factQuoteRandomDump( ReceivePrivmsg event )
    {
        Matcher matcher = randomPattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String mode = matcher.group( 1 );
            String queried = BaseNick.toBaseNick( matcher.group( 2 ) );

            if ( "fact".equals( mode ) )
            {
                Long factsLength = QuoteFactDB.getFactsLength( queried );
                if ( factsLength == 0 )
                {
                    event.replyPrivately( queried + " has no facts." );
                }
                else if ( factsLength <= 5 )
                {
                    Set<String> allFacts = QuoteFactDB.getFacts( queried );
                    for ( String fact : allFacts )
                    {
                        event.replyPrivately( formatFactReply( queried, fact ) );
                    }
                }
                else
                {
                    List<String> randomFacts = QuoteFactDB.getRandFacts( queried );
                    for ( String fact : randomFacts )
                    {
                        event.replyPrivately( formatFactReply( queried, fact ) );
                    }
                }
            }
            else
            {
                Long quotesLength = QuoteFactDB.getQuotesLength( queried );
                if ( quotesLength == 0 )
                {
                    event.replyPrivately( queried + " has no quotes." );
                }
                else if ( quotesLength <= 5 )
                {
                    Set<String> allQuotes = QuoteFactDB.getQuotes( queried );
                    for ( String quote : allQuotes )
                    {
                        event.replyPrivately( formatQuoteReply( queried, quote ) );
                    }
                }
                else
                {
                    List<String> randomQuote = QuoteFactDB.getRandQuotes( queried );
                    for ( String quote : randomQuote )
                    {
                        event.replyPrivately( formatQuoteReply( queried, quote ) );
                    }
                }
            }
        }
    }

    @Handler
    private void factQuoteNum( ReceivePrivmsg event )
    {
        Matcher matcher = numPattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String mode = matcher.group( 1 );
            String queried = BaseNick.toBaseNick( matcher.group( 2 ) );
            if ( "fact".equals( mode ) )
            {
                Long factsLength = QuoteFactDB.getFactsLength( queried );
                if ( factsLength == 0 )
                {
                    event.replyPrivately( queried + " has no facts." );
                }
                else
                {
                    event.replyPrivately( queried + " has " + factsLength + " facts." );
                }
            }
            else
            {
                Long quotesLength = QuoteFactDB.getQuotesLength( queried );
                if ( quotesLength == 0 )
                {
                    event.replyPrivately( queried + " has no quotes." );
                }
                else
                {
                    event.replyPrivately( queried + " has " + quotesLength + " quotes." );
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

    private static void saveToFile( Set<String> data, String queried, String sender, String mode, ReceivePrivmsg event,
                                    String overWrite )
    {
        if ( !DUMP_FOLDER_FILE.exists() )
        {
            DUMP_FOLDER_FILE.mkdirs();
        }

        File dumpFile = new File( DUMP_FOLDER_FILE, mode + "-of-" + queried + "-from-" + sender );

        if ( dumpFile.exists() && !overWrite.equals( "ow" ) )
        {
            return;
        }

        try
        {
            FileWriter writer = new FileWriter( dumpFile, false );

            for ( String quoteOrFact : data )
            {
                writer.write( quoteOrFact );
                writer.write( System.getProperty( "line.separator" ) );
            }

            writer.close();
        }
        catch ( Exception e )
        {
            event.replyPrivately( "Error writing quote/fact dump to file." );
        }
    }

    private static void getQuoteFactPage( Integer page, String queried, String sender, String mode,
                                          ReceivePrivmsg event )
    {
        double startingLine = ( page - 1 ) * LINES_PER_PAGE;

        if ( page < 1 )
        {
            event.replyPrivately( "That page number is invalid" );
            return;
        }

        File file = new File( DUMP_FOLDER_FILE, mode + "-of-" + queried + "-from-" + sender );

        long numberOfLines = getNumberOfLines( file, event );

        if ( numberOfLines < ( startingLine + 5 ) )
        {
            event.replyPrivately( "That page number is invalid, please enter a page number less than "
                + Math.ceil( numberOfLines / LINES_PER_PAGE ) + 1 );
            return;
        }

        try ( BufferedReader br = new BufferedReader( new FileReader( file ) ) )
        {
            for ( int i = 0; i < startingLine; i++ )
            {
                br.readLine();
            }

            String line;
            for ( long i = Math.round( startingLine ); i < startingLine + LINES_PER_PAGE; i++ )
            {
                line = br.readLine();
                event.replyPrivately( formatFactReply( queried, line ) );
            }

            event.replyPrivately( "There are " + Math.ceil( numberOfLines - ( startingLine + LINES_PER_PAGE ) )
                + " lines remaining, with " + LINES_PER_PAGE + " lines per page." );
        }
        catch ( Exception e )
        {
            event.replyPrivately( "Error reading quote/fact dump from file." );
        }
    }

    private static long getNumberOfLines( File file, ReceivePrivmsg event )
    {
        try ( LineNumberReader reader = new LineNumberReader( new FileReader( file ) ) )
        {
            while ( ( reader.readLine() ) != null )
                ;
            return reader.getLineNumber();
        }
        catch ( Exception e )
        {
            event.replyPrivately( "Error getting number of lines in file." );
            return -1;
        }
    }
}
