package com.oldterns.vilebot.handlers.user;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.db.ChurchDB;
import com.oldterns.vilebot.util.BaseNick;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Church
    extends ListenerAdapter
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );

    private static final Pattern donatePattern = Pattern.compile( "^!donate (-?[0-9]+)\\s*" );

    private static final Pattern churchTotalPattern = Pattern.compile( "^!churchtotal" );

    private static final Pattern topDonorsPattern = Pattern.compile( "^!topdonors" );

    private static final Pattern setTitlesPattern = Pattern.compile( "^!settitle (.+)$" );

    private static final Pattern inquisitPattern = Pattern.compile( "^!inquisit (" + nounPattern + ")\\s*$" );

    private static final Pattern topDonorsAyePattern = Pattern.compile( "^!aye" );

    private static final Pattern topDonorsNayPattern = Pattern.compile( "^!nay" );

    private static final long TIMEOUT = 30000L;

    private static ExecutorService timer = Executors.newScheduledThreadPool( 1 );

    private static VoteEvent currentVote = null;

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();

        Matcher donateToChurchMatcher = donatePattern.matcher( text );
        Matcher viewChurchTotalMatcher = churchTotalPattern.matcher( text );
        Matcher viewTopDonorsMatcher = topDonorsPattern.matcher( text );
        Matcher setTitleMatcher = setTitlesPattern.matcher( text );
        Matcher inquisitMatcher = inquisitPattern.matcher( text );
        Matcher matcherAye = topDonorsAyePattern.matcher( event.getMessage() );
        Matcher matcherNay = topDonorsNayPattern.matcher( event.getMessage() );

        if ( donateToChurchMatcher.matches() )
            donateToChurch( event, donateToChurchMatcher );
        if ( viewChurchTotalMatcher.matches() )
            viewChurchTotal( event );
        if ( viewTopDonorsMatcher.matches() )
            viewTopDonors( event );
        if ( setTitleMatcher.matches() )
            setTitle( event, setTitleMatcher );
        if ( inquisitMatcher.matches() )
            inquisit( event, inquisitMatcher );
        if ( matcherAye.matches() || matcherNay.matches() )
            inquisitDecision( event, matcherAye, matcherNay );
    }

    private void donateToChurch( GenericMessageEvent event, Matcher matcher )
    {
        if ( currentVote != null )
        {
            event.respondWith( "There is an ongoing vote, you cannot donate at this time." );
            return;
        }

        String rawDonationAmount = matcher.group( 1 );
        String donor = BaseNick.toBaseNick( event.getUser().getNick() );
        Integer donationAmount = Integer.parseInt( rawDonationAmount );
        Integer donorKarma = KarmaDB.getNounKarma( donor );
        donorKarma = donorKarma == null ? 0 : donorKarma;
        if ( donationAmount <= 0 )
        {
            event.respondWith( "You cannot donate a non-positive number, imbecile." );
        }
        else if ( donorKarma <= 0 || donorKarma < donationAmount )
        {
            event.respondWith( "You have insufficient karma to donate." );
        }
        else
        {
            ChurchDB.modDonorKarma( donor, donationAmount );
            KarmaDB.modNounKarma( donor, -1 * donationAmount );
            Integer churchDonorKarma = ChurchDB.getDonorKarma( donor );
            if ( churchDonorKarma != null && churchDonorKarma - donationAmount <= 0 )
            {
                ChurchDB.modDonorTitle( donor, " " );
            }
            event.respondWith( "Thank you for your donation of " + donationAmount + " " + donor + "!" );
        }
    }

    private void viewChurchTotal( GenericMessageEvent event )
    {
        long totalDonations = ChurchDB.getTotalDonations();
        long churchTotal = totalDonations + ChurchDB.getTotalNonDonations();
        event.respondWith( "The church coffers contains " + churchTotal + ", of which " + totalDonations
            + " was contributed by its loyal believers." );
    }

    // @Handler
    private void viewTopDonors( GenericMessageEvent event )
    {
        // Matcher matcher = topDonorsPattern.matcher( event.getMessage() );
        // if ( matcher.matches() )
        // {
        Set<String> nouns;
        nouns = ChurchDB.getDonorsByRanks( 0, 3 );
        if ( nouns != null && nouns.size() > 0 )
        {
            event.respondWith( "NICK           AMOUNT    TITLE" );
            for ( String noun : nouns )
            {
                replyWithRankAndDonationAmount( noun, event );
            }
        }
        else
        {
            event.respondWith( "The church does not have enough followers yet." );
        }
        // }
    }

    // @Handler
    private void setTitle( GenericMessageEvent event, Matcher matcher )
    {
        // Matcher matcher = setTitlesPattern.matcher( event.getMessage() );
        // if ( matcher.matches() )
        // {
        String donor = BaseNick.toBaseNick( event.getUser().getNick() );
        Integer donorRank = ChurchDB.getDonorRank( donor );
        if ( donorRank != null && donorRank > 4 )
        {
            event.respondWith( "You must be a top donor to set your title." );
            return;
        }

        String newTitle = matcher.group( 1 );
        if ( newTitle.length() > 40 )
        {
            newTitle = newTitle.substring( 0, 39 );
        }

        String oldTitle = ChurchDB.getDonorTitle( donor );
        ChurchDB.modDonorTitle( donor, newTitle );
        event.respondWith( donor + " is now to be referred to as " + newTitle + " instead of " + oldTitle + "." );
        // }
    }

    private void inquisit( GenericMessageEvent event, Matcher matcher )
    {
        if ( currentVote != null )
        {
            event.respondWith( "There is an ongoing inquisition against " + currentVote.getDecisionTarget()
                + ". Please use !aye or !nay to decide their fate" );
            return;
        }

        String donor = BaseNick.toBaseNick( event.getUser().getNick() );
        Integer donorRank = ChurchDB.getDonorRank( donor );
        if ( donorRank == null || donorRank > 4 )
        {
            event.respondWith( "You must be a top donor to start an inquisition." );
            return;
        }

        StringBuilder membersToPing = new StringBuilder();
        Set<String> nouns = ChurchDB.getDonorsByRanks( 0, 3 );
        if ( nouns != null && nouns.size() > 3 )
        {
            for ( String noun : nouns )
            {
                membersToPing.append( noun ).append( " " );
            }
        }
        else
        {
            event.respondWith( "There are not enough members of the church to invoke a vote" );
            return;
        }

        String inquisitedNick = BaseNick.toBaseNick( matcher.group( 1 ) );
        Integer nounKarma = ChurchDB.getDonorKarma( inquisitedNick );
        if ( nounKarma == null || nounKarma == 0 )
        {
            event.respondWith( "You cannot start an inquisition against someone who has no donation value." );
            return;
        }

        event.respondWith( "An inquisition has started against " + inquisitedNick
            + ". Please cast your votes with !aye or !nay" );
        event.respondWith( membersToPing.toString() );
        startInquisitionVote( event, inquisitedNick, donorRank );
    }

    private synchronized void startInquisitionVote( GenericMessageEvent event, String inquisitedNick, int donorRank )
    {
        currentVote = new VoteEvent();
        currentVote.setDecisionTarget( inquisitedNick );
        currentVote.updateVoteAye( donorRank );
        startTimer( event );
    }

    private void inquisitDecision( GenericMessageEvent event, Matcher matcherAye, Matcher matcherNay )
    {
        if ( currentVote == null )
        {
            event.respondWith( "There is no ongoing vote." );
            return;
        }

        String donor = BaseNick.toBaseNick( event.getUser().getNick() );
        Integer donorRank = ChurchDB.getDonorRank( donor );
        if ( donorRank == null || donorRank > 4 )
        {
            event.respondWith( "You must be a top donor to vote." );
            return;
        }

        if ( matcherAye.matches() )
        {
            currentVote.updateVoteAye( donorRank );
        }
        else if ( matcherNay.matches() )
        {
            currentVote.updateVoteNay( donorRank );
        }

        StringBuilder currentChoices = new StringBuilder();
        int i = 1;
        Set<String> nouns = ChurchDB.getDonorsByRanks( 0, 3 );
        for ( String noun : Objects.requireNonNull( nouns ) )
        {
            currentChoices.append( noun ).append( " (" ).append( currentVote.getDonorVote( i++ ) ).append( ") " );
        }
        event.respondWith( currentChoices.toString() );
    }

    private void startTimer( final GenericMessageEvent event )
    {
        timer.submit( () -> {
            try
            {
                Thread.sleep( TIMEOUT );
                timeoutTimer( event );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        } );
    }

    private void timeoutTimer( GenericMessageEvent event )
    {
        String message = "Voting is now finished\n";
        if ( currentVote.isDecisionYes() )
        {
            message += "The vote to inquisit " + currentVote.getDecisionTarget() + " has passed. "
                + currentVote.getDecisionTarget() + " will be stripped of their karma.";
            Integer donorKarma = ChurchDB.getDonorKarma( currentVote.getDecisionTarget() );
            if ( donorKarma != null )
            {
                ChurchDB.modNonDonorKarma( donorKarma );
                ChurchDB.removeDonor( currentVote.getDecisionTarget() );
            }
        }
        else
        {
            message += "The vote to inquisit " + currentVote.getDecisionTarget() + " has failed. Nothing will happen.";
        }

        event.respondWith( message );
        currentVote = null;
    }

    private static void replyWithRankAndDonationAmount( String noun, GenericMessageEvent event )
    {
        Integer nounKarma = ChurchDB.getDonorKarma( noun );
        String nounTitle = ChurchDB.getDonorTitle( noun );
        if ( nounKarma != null )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( noun );
            sb.reverse();
            String nounString = sb.toString();
            int nounLength = sb.length();
            int spaceLength;
            if ( nounLength <= 14 )
            {
                spaceLength = 15 - nounLength;
            }
            else
            {
                nounString = nounString.substring( 0, 14 );
                spaceLength = 1;
            }

            String spaces_first = String.format( "%" + spaceLength + "s", "" );
            if ( nounKarma.toString().length() <= 9 )
            {
                spaceLength = 10 - nounKarma.toString().length();
            }

            String spaces_second = String.format( "%" + spaceLength + "s", "" );
            event.respondWith( nounString + spaces_first + nounKarma.toString() + spaces_second + nounTitle );
        }
    }

    private static class VoteEvent
    {
        private boolean[] topDonorDecisions;

        private String decisionTarget;

        VoteEvent()
        {
            topDonorDecisions = new boolean[4];
            for ( int i = 0; i < 4; i++ )
            {
                topDonorDecisions[i] = false;
            }
            decisionTarget = null;
        }

        void updateVoteAye( int donorRank )
        {
            topDonorDecisions[donorRank - 1] = true;
        }

        void updateVoteNay( int donorRank )
        {
            topDonorDecisions[donorRank - 1] = false;
        }

        boolean getDonorVote( int donorRank )
        {
            return topDonorDecisions[donorRank - 1];
        }

        boolean isDecisionYes()
        {
            int totalValue = 0;
            if ( topDonorDecisions[0] )
                totalValue += 5;
            if ( topDonorDecisions[1] )
                totalValue += 3;
            if ( topDonorDecisions[2] )
                totalValue += 3;
            if ( topDonorDecisions[3] )
                totalValue += 1;
            return totalValue > 6;
        }

        void setDecisionTarget( String target )
        {
            decisionTarget = target;
        }

        String getDecisionTarget()
        {
            return decisionTarget;
        }
    }
}
