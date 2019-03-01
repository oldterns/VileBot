package com.oldterns.vilebot.handlers.user;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.ChurchDB;
import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Ignore;

import com.oldterns.vilebot.util.Sessions;
//import net.engio.mbassy.listener.Handler;

//import ca.szc.keratin.bot.KeratinBot;
//import ca.szc.keratin.bot.annotation.AssignedBot;
//import ca.szc.keratin.bot.annotation.HandlerContainer;
//import ca.szc.keratin.core.event.message.interfaces.Replyable;
//import ca.szc.keratin.core.event.message.recieve.ReceiveJoin;
//import ca.szc.keratin.core.event.message.recieve.GenericMessageEvent;
import com.oldterns.vilebot.Vilebot;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

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

    // @AssignedBot
    // private KeratinBot bot;

    @Override
    public void onJoin( JoinEvent event ) // announce karma on join
    {
        String noun = BaseNick.toBaseNick( Objects.requireNonNull( event.getUser() ).getNick() );

        if ( !Ignore.getOnJoin().contains( noun ) )
            replyWithRankAndKarma( noun, (GenericMessageEvent) event, false, false, true );
    }

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();

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
            specificKarmaQuery( event, specificMatcher );
        if ( selfMatcher.matches() )
            selfKarmaQuery( event, selfMatcher );
        if ( rankNumberMatcher.matches() )
            rankNumber( event, rankNumberMatcher );
        if ( totalKarmaMatcher.matches() )
            totalKarma( event );
        if ( topBottomThreeMatcher.matches() )
            topBottomThree( event, topBottomThreeMatcher );
        if ( unrankMatcher.matches() )
            unrank( event, unrankMatcher );
    }

    private void karmaInc( GenericMessageEvent event, Matcher incMatcher )
    {
        // Match any string that has at least one word that ends with ++
        // Matcher incMatcher = incrementPattern.matcher( event.getMessage() );
        //
        // if ( incMatcher.matches() )
        // {
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
        // }
    }

    private void karmaDec( GenericMessageEvent event, Matcher decMatcher )
    {
        // Match any string that has at least one word that ends with --
        // Matcher decMatcher = decrementPattern.matcher( event.getMessage() );
        //
        // if ( decMatcher.matches() )
        // {
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
        // }
    }

    // @Override
    private void karmaIncOrDec( GenericMessageEvent event, Matcher incOrDecMatcher )
    {
        // Match any string that has at least one word that ends with ++
        // Matcher incOrDecMatcher = incOrDecPattern.matcher( event.getMessage() );
        //
        // if ( incOrDecMatcher.matches() )
        // {
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
        // }
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

    // @Override
    // private void karmaQuery( GenericMessageEvent event )
    // {
    // Matcher specificMatcher = karmaQueryPattern.matcher( event.getMessage() );
    // Matcher selfMatcher = selfKarmaQueryPattern.matcher( event.getMessage() );
    //
    // if ( specificMatcher.matches() )
    // {
    //
    // }
    // else if ( selfMatcher.matches() )
    // {
    //
    // }
    // }

    private void specificKarmaQuery( GenericMessageEvent event, Matcher specificMatcher )
    {
        String mode = specificMatcher.group( 1 );
        String nickBlob = specificMatcher.group( 2 );

        List<String> nicks = new LinkedList<>();
        Matcher nickMatcher = nickBlobPattern.matcher( nickBlob );
        while ( nickMatcher.find() )
        {
            nicks.add( nickMatcher.group( 1 ) );
        }

        boolean reverse = "rev".equals( mode );

        for ( String nick : nicks )
        {
            if ( !replyWithRankAndKarma( nick, event, reverse ) )
                event.respondWith( nick + " has no karma." );
        }
    }

    private void selfKarmaQuery( GenericMessageEvent event, Matcher selfMatcher )
    {
        String mode = selfMatcher.group( 1 );
        String noun = BaseNick.toBaseNick( event.getUser().getNick() );

        boolean reverse = "rev".equals( mode );
        if ( !replyWithRankAndKarma( noun, event, reverse ) )
            event.respondWith( noun + " has no karma." );
    }

    // @Override
    private void rankNumber( GenericMessageEvent event, Matcher rankNumberMatcher )
    {
        // Matcher rankNumberMatcher = ranknPattern.matcher( event.getMessage() );
        //
        // if ( rankNumberMatcher.matches() )
        // {
        String mode = rankNumberMatcher.group( 1 );
        String place = rankNumberMatcher.group( 2 );

        boolean reverse = "rev".equals( mode );

        String noun;
        if ( reverse )
            noun = KarmaDB.getRevRankNoun( Long.parseLong( place ) );
        else
            noun = KarmaDB.getRankNoun( Long.parseLong( place ) );

        if ( noun != null )
            replyWithRankAndKarma( noun, event, reverse );
        else
            event.respondWith( "No noun at that rank." );
        // }
    }

    // @Override
    private void totalKarma( GenericMessageEvent event )
    {
        // Matcher matcher = totalPattern.matcher( event.getMessage() );
        // if ( matcher.matches() )
        // {
        event.respondWith( "" + KarmaDB.getTotalKarma() );
        // }
    }

    // @Override
    private void topBottomThree( GenericMessageEvent event, Matcher topBottomThreeMatcher )
    {
        // Matcher topBottomThreeMatcher = topBottomThreePattern.matcher( event.getMessage() );
        //
        // if ( matcher.matches() )
        // {
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
                replyWithRankAndKarma( noun, event, false, true );
            }
        }
        else
        {
            event.respondWith( "No nouns at ranks 1 to 3." );
        }
        // }
    }

    // @Override
    private void unrank( GenericMessageEvent event, Matcher unrankMatcher )
    {
        // Admin-only command: remove all of a user's karma.
        // Matcher matcher = removePattern.matcher( event.getMessage() );
        String username = Sessions.getSession( event.getUser().getNick() );

        if ( GroupDB.isAdmin( username ) )
        {
            String noun = BaseNick.toBaseNick( unrankMatcher.group( 1 ) );

            if ( replyWithRankAndKarma( noun, event ) )
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

    private static boolean replyWithRankAndKarma( String noun, GenericEvent event )
    {
        return replyWithRankAndKarma( noun, event, false );
    }

    private static boolean replyWithRankAndKarma( String noun, GenericEvent event, boolean reverseOrder )
    {
        return replyWithRankAndKarma( noun, event, reverseOrder, false );
    }

    private static boolean replyWithRankAndKarma( String noun, GenericEvent event, boolean reverseOrder,
                                                  boolean obfuscateNick )
    {
        return replyWithRankAndKarma( noun, event, reverseOrder, obfuscateNick, false );
    }

    private static boolean replyWithRankAndKarma( String noun, GenericEvent event, boolean reverseOrder,
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

            event.respond( sb.toString() );
            return true;
        }
        return false;
    }
}
