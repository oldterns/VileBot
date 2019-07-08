package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.ChurchDB;
import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Ignore;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Karma
    extends ListenerAdapter
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );

    private static final Pattern nickBlobPattern = Pattern.compile( "(?:(" + nounPattern + "?)(?:, +| +|$))" );

    private static final Pattern incBlobPattern = Pattern.compile( "(?:(" + nounPattern + "?\\+\\+)(?:, +| +|$))" );

    private static final Pattern decBlobPattern = Pattern.compile( "(?:(" + nounPattern + "?--)(?:, +| +|$))" );

    private static final Pattern incOrDecBlobPattern = Pattern.compile( "(?:(" + nounPattern + "?\\+-)(?:, +| +|$))" );

    private static final Pattern incrementPattern = Pattern.compile( "(?:^|^.*\\s+)(" + incBlobPattern + "+)(?:.*|$)" );

    private static final Pattern decrementPattern = Pattern.compile( "(?:^|^.*\\s+)(" + decBlobPattern + "+)(?:.*|$)" );

    private static final Pattern incOrDecPattern =
        Pattern.compile( "(?:^|^.*\\s+)(" + incOrDecBlobPattern + "+)(?:.*|$)" );
    // The opening (?:^|^.*\\s+) and closing (?:.*|$) are needed when only part of the message is ++ or -- events

    private static final Pattern selfKarmaQueryPattern = Pattern.compile( "^\\s*!(rev|)rank\\s*$" );

    private static final Pattern karmaQueryPattern = Pattern.compile( "!(rev|)rank (" + nickBlobPattern + "+)" );

    private static final Pattern ranknPattern = Pattern.compile( "!(rev|)rankn ([0-9]+)\\s*" );

    private static final Pattern topBottomThreePattern = Pattern.compile( "!(top|bottom)three\\s*" );

    private static final Pattern removePattern = Pattern.compile( "!admin unrank (" + nounPattern + ")\\s*" );

    private static final Pattern totalPattern = Pattern.compile( "^!total" );

    @Override
    public void onJoin( JoinEvent event ) // announce karma on join
    {
        String noun = BaseNick.toBaseNick( Objects.requireNonNull( event.getUser() ).getNick() );
        OutputIRC outputQ = event.getBot().send();
        String replyTarget = event.getChannel().getName();
        if ( !Ignore.getOnJoin().contains( noun ) )
            replyWithRankAndKarma( noun, outputQ, replyTarget, false, false, true );
    }

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        OutputIRC outputQ = event.getBot().send();
        String replyTarget;
        if ( event instanceof PrivateMessageEvent )
            replyTarget = event.getUser().getNick();
        else if ( event instanceof MessageEvent )
            replyTarget = ( (MessageEvent) event ).getChannel().getName();
        else
            return;

        Matcher incMatcher = incrementPattern.matcher( text );
        Matcher decMatcher = decrementPattern.matcher( text );
        Matcher incOrDecMatcher = incOrDecPattern.matcher( text );
        Matcher specificMatcher = karmaQueryPattern.matcher( text );
        Matcher selfMatcher = selfKarmaQueryPattern.matcher( text );
        Matcher rankNumberMatcher = ranknPattern.matcher( text );
        Matcher totalKarmaMatcher = totalPattern.matcher( text );
        Matcher topBottomThreeMatcher = topBottomThreePattern.matcher( text );
        Matcher unrankMatcher = removePattern.matcher( text );

        if ( incMatcher.matches() )
            karmaInc( event, incMatcher );
        if ( decMatcher.matches() )
            karmaDec( event, decMatcher );
        if ( incOrDecMatcher.matches() )
            karmaIncOrDec( event, incOrDecMatcher );
        if ( specificMatcher.matches() )
            specificKarmaQuery( event, outputQ, replyTarget, specificMatcher );
        if ( selfMatcher.matches() )
            selfKarmaQuery( event, outputQ, replyTarget, selfMatcher );
        if ( rankNumberMatcher.matches() )
            rankNumber( event, outputQ, replyTarget, rankNumberMatcher );
        if ( totalKarmaMatcher.matches() )
            totalKarma( event );
        if ( topBottomThreeMatcher.matches() )
            topBottomThree( event, outputQ, replyTarget, topBottomThreeMatcher );
        if ( unrankMatcher.matches() )
            unrank( event, outputQ, replyTarget, unrankMatcher );
    }

    private void karmaInc( GenericMessageEvent event, Matcher incMatcher )
    {
        if ( isPrivate( event ) )
        {
            KarmaDB.modNounKarma( Objects.requireNonNull( event.getUser() ).getNick(), -1 );
            return;
        }
        // Prevent users from increasing karma outside of #TheFoobar
        if ( !( (MessageEvent) event ).getChannel().getName().equals( Vilebot.getConfig().get( "ircChannel1" ) ) )
        {
            event.respondWith( "You must be in " + Vilebot.getConfig().get( "ircChannel1" )
                + " to give or receive karma." );
            return;
        }

        // If one match is found, take the entire text of the message (group(0)) and check each word
        // This is needed in the case that only part of the message is karma events (ie "wow anestico++")
        String wordBlob = incMatcher.group( 0 );
        String sender = BaseNick.toBaseNick( Objects.requireNonNull( event.getUser() ).getNick() );

        Set<String> nicks = new HashSet<>();
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
            event.respondWith( "I think I'm supposed to insult you now." );
    }

    private void karmaDec( GenericMessageEvent event, Matcher decMatcher )
    {
        if ( isPrivate( event ) )
        {
            KarmaDB.modNounKarma( Objects.requireNonNull( event.getUser() ).getNick(), -1 );
            return;
        }
        // Prevent users from decreasing karma outside of #TheFoobar
        if ( !( (MessageEvent) event ).getChannel().getName().equals( Vilebot.getConfig().get( "ircChannel1" ) ) )
        {
            event.respondWith( "You must be in " + Vilebot.getConfig().get( "ircChannel1" )
                + " to give or receive karma." );
            return;
        }
        // If one match is found, take the entire text of the message (group(0)) and check each word
        String wordBlob = decMatcher.group( 0 );

        List<String> nicks = new LinkedList<>();
        Matcher nickMatcher = decBlobPattern.matcher( wordBlob );
        while ( nickMatcher.find() )
        {
            nicks.add( BaseNick.toBaseNick( nickMatcher.group( 1 ) ) );
        }

        boolean insult = false;
        String botNick = event.getBot().getNick();
        for ( String nick : nicks )
        {
            if ( !nick.equals( botNick ) )
                KarmaDB.modNounKarma( nick, -1 );
            else
                insult = true;
        }

        if ( insult )
            // TODO insult generator?
            event.respondWith( "I think I'm supposed to insult you now." );
    }

    private void karmaIncOrDec( GenericMessageEvent event, Matcher incOrDecMatcher )
    {
        if ( isPrivate( event ) )
        {
            KarmaDB.modNounKarma( Objects.requireNonNull( event.getUser() ).getNick(), -1 );
            return;
        }

        // Prevent users from increasing karma outside of #TheFoobar
        if ( !( (MessageEvent) event ).getChannel().getName().equals( Vilebot.getConfig().get( "ircChannel1" ) ) )
        {
            event.respondWith( "You must be in " + Vilebot.getConfig().get( "ircChannel1" )
                + " to give or receive karma." );
            return;
        }

        // If one match is found, take the entire text of the message (group(0)) and check each word
        // This is needed in the case that only part of the message is karma events (ie "wow anestico++")
        String wordBlob = incOrDecMatcher.group( 0 );
        String sender = BaseNick.toBaseNick( Objects.requireNonNull( event.getUser() ).getNick() );

        Set<String> nicks = new HashSet<>();
        Matcher nickMatcher = incOrDecBlobPattern.matcher( wordBlob );
        while ( nickMatcher.find() )
        {
            nicks.add( BaseNick.toBaseNick( nickMatcher.group( 1 ) ) );
        }

        boolean insult = false;

        for ( String nick : nicks )
        {
            if ( !nick.equals( sender ) )
                decideIncOrDec( event, nick );
            else
                insult = true;
        }

        if ( insult )
            // TODO insult generator?
            event.respondWith( "I think I'm supposed to insult you now." );
    }

    private void decideIncOrDec( GenericMessageEvent event, String nick )
    {
        int karma = 0;
        Random rand = new Random();
        while ( karma == 0 )
        {
            karma = rand.nextInt( 3 ) - 1;
        }
        String reply = nick + " had their karma ";
        reply += karma == 1 ? "increased" : "decreased";
        reply += " by 1";
        event.respondWith( reply );
        KarmaDB.modNounKarma( nick, karma );
    }

    private boolean isPrivate( GenericMessageEvent event )
    {
        return event instanceof PrivateMessageEvent;
    }

    private void specificKarmaQuery( GenericMessageEvent event, OutputIRC outputQ, String replyTarget,
                                     Matcher specificMatcher )
    {
        String mode = specificMatcher.group( 1 );
        String nickBlob = specificMatcher.group( 2 );

        List<String> nicks = new LinkedList<>();
        Matcher nickMatcher = nickBlobPattern.matcher( nickBlob );
        while ( nickMatcher.find() )
        {
            nicks.add( BaseNick.toBaseNick( nickMatcher.group( 1 ) ) );
        }

        boolean reverse = "rev".equals( mode );

        for ( String nick : nicks )
        {
            if ( !replyWithRankAndKarma( nick, outputQ, replyTarget, reverse ) )
                event.respondWith( nick + " has no karma." );
        }
    }

    private void selfKarmaQuery( GenericMessageEvent event, OutputIRC outputQ, String replyTarget, Matcher selfMatcher )
    {
        String mode = selfMatcher.group( 1 );
        String noun = BaseNick.toBaseNick( event.getUser().getNick() );

        boolean reverse = "rev".equals( mode );
        if ( !replyWithRankAndKarma( noun, outputQ, replyTarget, reverse ) )
            event.respondWith( noun + " has no karma." );
    }

    private void rankNumber( GenericMessageEvent event, OutputIRC outputQ, String replyTarget,
                             Matcher rankNumberMatcher )
    {
        String mode = rankNumberMatcher.group( 1 );
        String place = rankNumberMatcher.group( 2 );

        boolean reverse = "rev".equals( mode );

        String noun;
        if ( reverse )
            noun = KarmaDB.getRevRankNoun( Long.parseLong( place ) );
        else
            noun = KarmaDB.getRankNoun( Long.parseLong( place ) );

        if ( noun != null )
            replyWithRankAndKarma( noun, outputQ, replyTarget, reverse );
        else
            event.respondWith( "No noun at that rank." );
    }

    private void totalKarma( GenericMessageEvent event )
    {
        event.respondWith( "" + KarmaDB.getTotalKarma() );
    }

    private void topBottomThree( GenericMessageEvent event, OutputIRC outputQ, String replyTarget,
                                 Matcher topBottomThreeMatcher )
    {
        String mode = topBottomThreeMatcher.group( 1 );

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
                replyWithRankAndKarma( noun, outputQ, replyTarget, false, true );
            }
        }
        else
        {
            event.respondWith( "No nouns at ranks 1 to 3." );
        }
    }

    private void unrank( GenericMessageEvent event, OutputIRC outputQ, String replyTarget, Matcher unrankMatcher )
    {
        // Admin-only command: remove all of a user's karma.
        // Matcher matcher = removePattern.matcher( event.getMessage() );
        String username = Sessions.getSession( event.getUser().getNick() );

        if ( GroupDB.isAdmin( username ) )
        {
            String noun = BaseNick.toBaseNick( unrankMatcher.group( 1 ) );

            if ( replyWithRankAndKarma( noun, outputQ, replyTarget ) )
            {
                event.respondWith( "Removing " + noun + "." );
                KarmaDB.remNoun( noun );
            }
            else
            {
                event.respondWith( noun + " isn't ranked." );
            }
        }
    }

    private static boolean replyWithRankAndKarma( String noun, OutputIRC outputQ, String replyTarget )
    {
        return replyWithRankAndKarma( noun, outputQ, replyTarget, false );
    }

    private static boolean replyWithRankAndKarma( String noun, OutputIRC outputQ, String replyTarget,
                                                  boolean reverseOrder )
    {
        return replyWithRankAndKarma( noun, outputQ, replyTarget, reverseOrder, false );
    }

    private static boolean replyWithRankAndKarma( String noun, OutputIRC outputQ, String replyTarget,
                                                  boolean reverseOrder, boolean obfuscateNick )
    {
        return replyWithRankAndKarma( noun, outputQ, replyTarget, reverseOrder, obfuscateNick, false );
    }

    private static boolean replyWithRankAndKarma( String noun, OutputIRC outputQ, String replyTarget,
                                                  boolean reverseOrder, boolean obfuscateNick, boolean useTitle )
    {
        Integer nounRank;
        if ( reverseOrder )
            nounRank = KarmaDB.getNounRevRank( noun );
        else
            nounRank = KarmaDB.getNounRank( noun );

        Integer nounKarma = KarmaDB.getNounKarma( noun );

        if ( useTitle && ChurchDB.isTopDonor( noun ) )
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

            outputQ.message( replyTarget, sb.toString() );
            return true;
        }
        return false;
    }
}
