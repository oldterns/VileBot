/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import com.google.gson.Gson;
import com.oldterns.vilebot.db.ChurchDB;
import com.oldterns.vilebot.db.QuoteFactDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Ignore;
import com.oldterns.vilebot.util.MangleNicks;
import com.oldterns.vilebot.util.StringUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class QuotesAndFacts
    extends ListenerAdapter
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );

    private static final Pattern addPattern = Pattern.compile( "^!(fact|quote)add (" + nounPattern + ") (.+)$" );

    private static final Pattern dumpPattern = Pattern.compile( "^!(fact|quote)dump (" + nounPattern + ")\\s*$" );

    private static final Pattern randomPattern = Pattern.compile( "^!(fact|quote)random5 (" + nounPattern + ")\\s*$" );

    private static final Pattern numPattern = Pattern.compile( "^!(fact|quote)number (" + nounPattern + ")\\s*$" );

    private static final Pattern queryPattern =
        Pattern.compile( "^!(fact|quote) (" + nounPattern + ")( !jaziz)?\\s*$" );

    // "( !jaziz)" is not included in searchPattern because it is handled in factQuoteSearch method
    private static final Pattern searchPattern = Pattern.compile( "^!(fact|quote)search (" + nounPattern + ") (.*)$" );

    private static final Random random = new Random();

    // cache fact and quote dump links
    private Map<String, String> dumpCache = new HashMap<>();

    // update cache when new quotes/facts added
    private Map<String, Integer> dumpSize = new HashMap<>();

    @Override
    public void onJoin( final JoinEvent event ) // announce fact or quote on join
    {
        String baseNick = BaseNick.toBaseNick( Objects.requireNonNull( event.getUser() ).getNick() );
        if ( !Ignore.getOnJoin().contains( baseNick ) )
        {
            if ( random.nextBoolean() )
            {
                if ( !replyWithQuote( baseNick, event, false ) )
                    replyWithFact( baseNick, event, false );
            }
            else
            {
                if ( !replyWithFact( baseNick, event, false ) )
                    replyWithQuote( baseNick, event, false );
            }
        }
    }

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();

        Matcher addMatcher = addPattern.matcher( text );
        Matcher dumpMatcher = dumpPattern.matcher( text );
        Matcher factQuoteRandomDumpMatcher = randomPattern.matcher( text );
        Matcher factQuoteNumMatcher = numPattern.matcher( text );
        Matcher queryMatcher = queryPattern.matcher( text );
        Matcher searchMatcher = searchPattern.matcher( text );

        if ( addMatcher.matches() )
            factQuoteAdd( event, addMatcher );
        if ( dumpMatcher.matches() )
            factQuoteDump( event, dumpMatcher );
        if ( factQuoteRandomDumpMatcher.matches() )
            factQuoteRandomDump( event, factQuoteRandomDumpMatcher );
        if ( factQuoteNumMatcher.matches() )
            factQuoteNum( event, factQuoteNumMatcher );
        if ( queryMatcher.matches() )
            factQuoteQuery( event, queryMatcher );
        if ( searchMatcher.matches() )
            factQuoteSearch( event, searchMatcher );
    }

    private void factQuoteAdd( GenericMessageEvent event, Matcher addMatcher )
    {
        String mode = addMatcher.group( 1 );
        String noun = BaseNick.toBaseNick( addMatcher.group( 2 ) );
        String text = addMatcher.group( 3 );
        String sender = BaseNick.toBaseNick( event.getUser().getNick() );

        if ( !sender.equals( noun ) )
        {
            text = trimChars( text, " '\"" );

            if ( "fact".equals( mode ) )
            {
                QuoteFactDB.addFact( noun, text );
                event.respondWith( formatFactReply( noun, text ) );
            }
            else
            {
                QuoteFactDB.addQuote( noun, text );
                event.respondWith( formatQuoteReply( noun, text ) );
            }
        }
        else
        {
            event.respondWith( StringUtil.capitalizeFirstLetter( mode )
                + "s from yourself are both terrible and uninteresting." );
        }
    }

    private void factQuoteDump( GenericMessageEvent event, Matcher dumpMatcher )
    {
        String mode = dumpMatcher.group( 1 );
        String queried = BaseNick.toBaseNick( dumpMatcher.group( 2 ) );

        if ( mode.equals( "fact" ) )
        {
            Set<String> allFacts = QuoteFactDB.getFacts( queried );
            if ( allFacts.isEmpty() )
            {
                event.respondWith( queried + " has no facts." );
            }
            else
            {
                String dumpKey = "fact" + queried;
                String response;
                if ( allFacts.size() != dumpSize.getOrDefault( dumpKey, 0 ) )
                {
                    StringBuilder sb = new StringBuilder();
                    for ( String fact : allFacts )
                    {
                        sb.append( formatFactReply( queried, StringEscapeUtils.escapeJson( fact ) ) ).append( "\\n" );
                    }
                    try
                    {
                        String title = queried + "'s facts";
                        String pasteLink = dumpToPastebin( title, sb.toString() );
                        dumpCache.put( dumpKey, pasteLink );
                        dumpSize.put( dumpKey, allFacts.size() );
                        response = "factdump for " + queried + ": " + pasteLink;
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                        response = "Error creating pastebin link";
                    }
                }
                else
                {
                    String pasteLink = dumpCache.get( dumpKey );
                    response = "factdump for " + queried + ": " + pasteLink;
                }
                event.respondWith( response );
            }
        }
        else
        {
            Set<String> allQuotes = QuoteFactDB.getQuotes( queried );
            if ( allQuotes.isEmpty() )
            {
                event.respondWith( queried + " has no quotes." );
            }
            else
            {
                String dumpKey = "quote" + queried;
                String response;
                if ( allQuotes.size() != dumpSize.getOrDefault( dumpKey, 0 ) )
                {
                    StringBuilder sb = new StringBuilder();
                    for ( String quote : allQuotes )
                    {
                        sb.append( StringEscapeUtils.escapeJson( quote ) ).append( "\\n" );
                    }
                    try
                    {
                        String title = queried + "'s quotes";
                        String pasteLink = dumpToPastebin( title, sb.toString() );
                        dumpCache.put( dumpKey, pasteLink );
                        dumpSize.put( dumpKey, allQuotes.size() );
                        response = "quotedump for " + queried + ": " + pasteLink;
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                        response = "Error creating pastebin link";
                    }
                }
                else
                {
                    String pasteLink = dumpCache.get( dumpKey );
                    response = "quotedump for " + queried + ": " + pasteLink;
                }
                event.respondWith( response );
            }
        }
    }

    private String dumpToPastebin( String title, String contents )
        throws Exception
    {
        URL url = new URL( "https://paste.fedoraproject.org/api/paste/submit" );
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput( true );
        conn.setRequestMethod( "POST" );
        conn.setRequestProperty( "Content-Type", "application/json" );
        String input = "{\"title\": \" " + title + "\"," + " \"contents\": \"" + contents + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write( input.getBytes() );
        os.flush();

        BufferedReader br = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
        StringBuilder response = new StringBuilder();
        String line;
        while ( ( line = br.readLine() ) != null )
        {
            response.append( line );
        }
        Gson gson = new Gson();
        Paste paste = gson.fromJson( response.toString(), Paste.class );
        return paste.url;
    }

    private void factQuoteRandomDump( GenericMessageEvent event, Matcher factQuoteRandomDumpMatcher )
    {
        String mode = factQuoteRandomDumpMatcher.group( 1 );
        String queried = BaseNick.toBaseNick( factQuoteRandomDumpMatcher.group( 2 ) );

        if ( "fact".equals( mode ) )
        {
            Long factsLength = QuoteFactDB.getFactsLength( queried );
            if ( factsLength == 0 )
            {
                event.respondPrivateMessage( queried + " has no facts." );
            }
            else if ( factsLength <= 5 )
            {
                Set<String> allFacts = QuoteFactDB.getFacts( queried );
                for ( String fact : allFacts )
                {
                    event.respondPrivateMessage( formatFactReply( queried, fact ) );
                }
            }
            else
            {
                List<String> randomFacts = QuoteFactDB.getRandFacts( queried );
                for ( String fact : randomFacts )
                {
                    event.respondPrivateMessage( formatFactReply( queried, fact ) );
                }
            }
        }
        else
        {
            Long quotesLength = QuoteFactDB.getQuotesLength( queried );
            if ( quotesLength == 0 )
            {
                event.respondPrivateMessage( queried + " has no quotes." );
            }
            else if ( quotesLength <= 5 )
            {
                Set<String> allQuotes = QuoteFactDB.getQuotes( queried );
                for ( String quote : allQuotes )
                {
                    event.respondPrivateMessage( formatQuoteReply( queried, quote ) );
                }
            }
            else
            {
                List<String> randomQuote = QuoteFactDB.getRandQuotes( queried );
                for ( String quote : randomQuote )
                {
                    event.respondPrivateMessage( formatQuoteReply( queried, quote ) );
                }
            }
        }
    }

    private void factQuoteNum( GenericMessageEvent event, Matcher factQuoteNumMatcher )
    {
        String mode = factQuoteNumMatcher.group( 1 );
        String queried = BaseNick.toBaseNick( factQuoteNumMatcher.group( 2 ) );
        if ( "fact".equals( mode ) )
        {
            Long factsLength = QuoteFactDB.getFactsLength( queried );
            if ( factsLength == 0 )
            {
                event.respondWith( queried + " has no facts." );
            }
            else
            {
                event.respondWith( queried + " has " + factsLength + " facts." );
            }
        }
        else
        {
            Long quotesLength = QuoteFactDB.getQuotesLength( queried );
            if ( quotesLength == 0 )
            {
                event.respondWith( queried + " has no quotes." );
            }
            else
            {
                event.respondWith( queried + " has " + quotesLength + " quotes." );
            }
        }
    }

    private void factQuoteQuery( GenericMessageEvent event, Matcher queryMatcher )
    {
        String mode = queryMatcher.group( 1 );
        String noun = BaseNick.toBaseNick( queryMatcher.group( 2 ) );

        // check if quote/fact needs to be piped to jaziz
        boolean jaziz = event.getMessage().lastIndexOf( "!jaziz" ) >= 0;

        if ( "fact".equals( mode ) )
        {
            if ( !replyWithFact( noun, event, jaziz ) )
            {
                event.respondWith( noun + " has no facts." );
            }
        }
        else
        {
            if ( !replyWithQuote( noun, event, jaziz ) )
            {
                event.respondWith( noun + " has no quotes." );
            }
        }
    }

    private void factQuoteSearch( GenericMessageEvent event, Matcher searchMatcher )
    {
        String mode = searchMatcher.group( 1 );
        String noun = BaseNick.toBaseNick( searchMatcher.group( 2 ) );
        String regex = searchMatcher.group( 3 );

        // check if quote/fact needs to be piped to jaziz
        int jazizIdx = regex.lastIndexOf( "!jaziz" );
        boolean jaziz = jazizIdx >= 0;
        if ( jaziz )
        {
            regex = regex.substring( 0, jazizIdx - 1 );
        }

        try
        {
            // Case insensitive added automatically, use (?-i) in a message to re-enable case sensitivity
            Pattern pattern = Pattern.compile( "(?i)" + regex );

            if ( "fact".equals( mode ) )
            {
                Set<String> texts = QuoteFactDB.getFacts( noun );
                if ( texts != null )
                {
                    String randomMatch = regexSetSearch( texts, pattern );
                    if ( randomMatch != null )
                    {
                        randomMatch = MangleNicks.mangleNicks( event, randomMatch );
                        if ( jaziz )
                        {
                            try
                            {
                                event.respondWith( formatFactReply( noun, Jaziz.jazizify( randomMatch ) ) );
                            }
                            catch ( Exception e )
                            {
                                event.respondWith( "eeeh" );
                                e.printStackTrace();
                            }
                        }
                        else
                        {
                            event.respondWith( formatFactReply( noun, randomMatch ) );
                        }
                    }
                    else
                    {
                        event.respondWith( noun + " has no matching facts." );
                    }
                }
                else
                {
                    event.respondWith( noun + " has no facts." );
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
                        randomMatch = MangleNicks.mangleNicks( event, randomMatch );
                        if ( jaziz )
                        {
                            try
                            {
                                event.respondWith( formatQuoteReply( noun, Jaziz.jazizify( randomMatch ) ) );
                            }
                            catch ( Exception e )
                            {
                                event.respondWith( "eeeh" );
                                e.printStackTrace();
                            }
                        }
                        else
                        {
                            event.respondWith( formatQuoteReply( noun, randomMatch ) );
                        }
                    }
                    else
                    {
                        event.respondWith( noun + " has no matching quotes." );
                    }
                }
                else
                {
                    event.respondWith( noun + " has no quotes." );
                }
            }
        }
        catch ( PatternSyntaxException e )
        {
            event.respondWith( "Syntax error in regex pattern" );
        }
    }

    private static String regexSetSearch( Set<String> texts, Pattern pattern )
    {
        List<String> matchingTexts = new LinkedList<>();

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

    private boolean replyWithFact( String noun, GenericMessageEvent event, boolean jaziz )
    {
        String replyText = getReplyFact( noun, jaziz );
        if ( replyText != null )
        {
            replyText = MangleNicks.mangleNicks( event, replyText );
            replyText = formatFactReply( getTitle( noun ), replyText );
            event.respondWith( replyText );
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean replyWithFact( String noun, JoinEvent event, boolean jaziz )
    {
        String replyText = getReplyFact( noun, jaziz );
        if ( replyText != null )
        {
            replyText = MangleNicks.mangleNicks( event, replyText );
            replyText = formatFactReply( getTitle( noun ), replyText );
            event.getBot().send().message( event.getChannel().getName(), replyText );
            return true;
        }
        else
        {
            return false;
        }
    }

    private String getReplyFact( String noun, boolean jaziz )
    {
        String text = QuoteFactDB.getRandFact( noun );
        if ( text == null )
        {
            return null;
        }
        if ( jaziz )
        {
            try
            {
                text = Jaziz.jazizify( text );
            }
            catch ( Exception e )
            {
                text = "eeeh";
                e.printStackTrace();
            }
        }
        return text;
    }

    private String formatFactReply( String noun, String fact )
    {
        return noun + " " + fact;
    }

    private boolean replyWithQuote( String noun, GenericMessageEvent event, boolean jaziz )
    {
        String replyText = getReplyQuote( noun, jaziz );
        if ( replyText != null )
        {
            replyText = MangleNicks.mangleNicks( event, replyText );
            replyText = formatQuoteReply( getTitle( noun ), replyText );
            event.respondWith( replyText );
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean replyWithQuote( String noun, JoinEvent event, boolean jaziz )
    {
        String replyText = getReplyQuote( noun, jaziz );
        if ( replyText != null )
        {
            replyText = MangleNicks.mangleNicks( event, replyText );
            replyText = formatQuoteReply( getTitle( noun ), replyText );
            event.getBot().send().message( event.getChannel().getName(), replyText );
            return true;
        }
        else
        {
            return false;
        }
    }

    private String getReplyQuote( String noun, boolean jaziz )
    {
        String text = QuoteFactDB.getRandQuote( noun );
        if ( text == null )
        {
            return text;
        }
        if ( jaziz )
        {
            try
            {
                text = Jaziz.jazizify( text );
            }
            catch ( Exception e )
            {
                text = "eeeh";
                e.printStackTrace();
            }
        }
        return text;
    }

    private String formatQuoteReply( String noun, String quote )
    {
        return noun + " once said, \"" + quote + "\".";
    }

    private String getTitle( String noun )
    {
        if ( ChurchDB.isTopDonor( noun ) )
        {
            String title = ChurchDB.getDonorTitle( noun );
            if ( title.trim().length() > 0 )
            {
                noun = title;
            }
        }
        return noun;
    }

    /**
     * Removes all specified leading and trailing characters in the array charsToRemove.
     *
     * @param input The string to process
     * @param charsToRemove All characters to remove, treated as a set
     * @return A copy of the input String with the characters removed
     */
    private String trimChars( String input, String charsToRemove )
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

        return input.substring( st, len );
    }

    class Paste
    {
        /* response fields on success */
        public String[] attachments;

        public String contents;

        public int expiry_time;

        public boolean is_active;

        public boolean is_password_protected;

        public String language;

        public String paste_id_repr;

        public int post_time;

        public String title;

        public String url;

        public int views;

        /* response fields on failure */
        public String failure;

        public String message;

        public boolean success;
    }
}
