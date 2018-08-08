package com.oldterns.vilebot.handlers.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.db.KarmalyticsDB;
import com.oldterns.vilebot.karmalytics.KarmalyticsRecord;
import com.oldterns.vilebot.util.BaseNick;
import net.engio.mbassy.listener.Handler;

// Formater removes empty lines from javadoc...
@HandlerContainer
// Process Karmalytics requests Currently supported requests are:
//
// !ka report [nick] [from date] [to date]
//
// - Gives a general summary of karma transactions for a specified range
// (from defaults to one week before to, to defaults to
// today). If nick is provided, give summary for user with nick instead
//
// !ka past number [nick]
//
// - Displays the past number karma transactions. If nick was provided, show
// their last number
// karma transaction
//
public class Karmalytics
{
    private static final Pattern PERFORM_KARMALYTIC_PATTERN = Pattern.compile( "!ka (.*)" );

    private static final Pattern REPORT_PATTERN = Pattern.compile( "report( [^-].*?)?( -from .*?)?( -to .*?)?" );

    private static final Pattern PAST_PATTERN = Pattern.compile( "past ([0-9]+)( .*?)?" );

    @Handler
    public void performKarmalytics( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = PERFORM_KARMALYTIC_PATTERN.matcher( text );

        if ( matcher.matches() )
        {
            String request = matcher.group( 1 );
            processRequest( event, BaseNick.toBaseNick( event.getSender() ), request );
        }
    }

    private void processRequest( ReceivePrivmsg event, String nick, String request )
    {
        Matcher reportMatcher = REPORT_PATTERN.matcher( request );
        Matcher pastMatcher = PAST_PATTERN.matcher( request );

        if ( reportMatcher.matches() )
        {
            Optional<String> nickToReport = Optional.ofNullable( reportMatcher.group( 1 ) );
            Optional<String> from = Optional.ofNullable( reportMatcher.group( 2 ) );
            Optional<String> to = Optional.ofNullable( reportMatcher.group( 3 ) );
            processReport( event, nickToReport, from, to );
        }
        else if ( pastMatcher.matches() )
        {
            int numberOfPastTransactionsToFetch = Integer.parseInt( pastMatcher.group( 1 ) );
            Optional<String> nickToReport = Optional.ofNullable( pastMatcher.group( 2 ) );
            processPastTransactions( event, numberOfPastTransactionsToFetch, nickToReport );
        }
        else
        {
            event.reply( "\"" + request + "\" is not a recognized command. See !help for karmalytics command list." );
        }
    }

    private void processPastTransactions( ReceivePrivmsg event, int numberOfPastTransactionsToFetch,
                                          Optional<String> nickToReport )
    {
        if ( nickToReport.isPresent() )
        {
            String baseNick = BaseNick.toBaseNick( nickToReport.get().substring( 1 ) );
            List<KarmalyticsRecord> myRecords = new ArrayList<>();
            for ( int i = 0; myRecords.size() < numberOfPastTransactionsToFetch; i++ )
            {
                List<KarmalyticsRecord> records = KarmalyticsDB.getRangeByEndRank( i * 100, 100 );
                if ( records.isEmpty() )
                {
                    break;
                }

                records.stream().filter( r -> r.getNick().equals( baseNick ) ).forEachOrdered( r -> myRecords.add( r ) );
            }

            if ( myRecords.isEmpty() )
            {
                event.reply( "No karma transactions has occurred yet for " + baseNick + "." );
            }
            else
            {
                myRecords.stream().limit( numberOfPastTransactionsToFetch ).forEachOrdered( record -> {
                    event.reply( KarmalyticsDB.getRecordDescriptorFunctionForSource( record.getSource() ).apply( record ) );
                } );
            }

        }
        else
        {
            List<KarmalyticsRecord> records = KarmalyticsDB.getRangeByEndRank( 0, numberOfPastTransactionsToFetch );

            if ( records.isEmpty() )
            {
                event.reply( "No karma transactions has occurred yet." );
            }
            else
            {
                for ( KarmalyticsRecord record : records )
                {
                    event.reply( KarmalyticsDB.getRecordDescriptorFunctionForSource( record.getSource() ).apply( record ) );
                }
            }

        }
    }

    private void processReport( ReceivePrivmsg event, Optional<String> nick, Optional<String> from,
                                Optional<String> to )
    {
        LocalDateTime fromDateTime;
        LocalDateTime toDateTime;

        if ( to.isPresent() )
        {
            try
            {
                toDateTime = LocalDate.parse( to.get().substring( 5 ) ).atStartOfDay();
            }
            catch ( DateTimeParseException parseException )
            {
                event.reply( "Error in -to date format; format your date following ISO standards (YYYY-MM-DD)" );
                return;
            }
        }
        else
        {
            toDateTime = LocalDate.now().plusDays( 1 ).atStartOfDay();
        }
        if ( from.isPresent() )
        {
            try
            {

                fromDateTime = LocalDate.parse( from.get().substring( 7 ) ).atStartOfDay();
            }
            catch ( DateTimeParseException parseException )
            {
                event.reply( "Error in -from date format; format your date following ISO standards (YYYY-MM-DD)" );
                return;
            }
        }
        else
        {
            fromDateTime = toDateTime.minusWeeks( 1 );
        }

        Set<KarmalyticsRecord> karmalyticsRecords = KarmalyticsDB.getRecordsBetween( fromDateTime, toDateTime );
        Map<String, Integer> sourceToKarmaMap = new HashMap<>();

        event.reply( "There are " + karmalyticsRecords.size() + " records" );

        if ( nick.isPresent() )
        {
            String baseNick = BaseNick.toBaseNick( nick.get().trim() );
            for ( KarmalyticsRecord record : karmalyticsRecords )
            {
                if ( record.getNick().equals( baseNick ) )
                {
                    sourceToKarmaMap.compute( record.getSource(), ( source, karma ) -> ( ( karma != null ) ? karma : 0 )
                        + record.getKarmaModAmount() );
                }
            }
        }
        else
        {
            for ( KarmalyticsRecord record : karmalyticsRecords )
            {
                sourceToKarmaMap.compute( record.getSource(), ( source, karma ) -> ( ( karma != null ) ? karma : 0 )
                    + record.getKarmaModAmount() );
            }
        }

        if ( nick.isPresent() )
        {
            event.reply( "Karmalytics report for " + BaseNick.toBaseNick( nick.get().trim() ) + " from "
                + fromDateTime.toString() + " to " + toDateTime.toString() + ":" );
        }
        else
        {
            event.reply( "Karmalytics report from " + fromDateTime.toString() + " to " + toDateTime.toString() + ":" );
        }
        for ( String source : KarmalyticsDB.getSources() )
        {
            StringBuilder line = new StringBuilder();
            line.append( source );
            line.append( ": " );
            int karma = sourceToKarmaMap.getOrDefault( source, 0 );
            if ( karma > 0 )
            {
                line.append( "+" );
            }
            line.append( karma );
            event.reply( line.toString() );
        }
    }
}
