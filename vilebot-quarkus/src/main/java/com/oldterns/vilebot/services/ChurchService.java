package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.database.ChurchDB;
import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.TimeService;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class ChurchService {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Inject
    TimeService timeService;

    @Inject
    KarmaDB karmaDB;

    @Inject
    ChurchDB churchDB;

    VoteEvent currentVote = null;

    @OnChannelMessage("!donate @donationAmount")
    public String donate(User user, Integer donationAmount) {
        if ( currentVote != null )
        {
            return  "There is an ongoing vote, you cannot donate at this time.";
        }

        String donor = Nick.getNick( user ).getBaseNick();
        long donorKarma = karmaDB.getNounKarma( donor ).orElse(0L);
        if ( donationAmount <= 0 )
        {
            return "You cannot donate a non-positive number.";
        }
        else if ( donorKarma <= 0 || donorKarma < donationAmount )
        {
            return "You have insufficient karma to donate.";
        }
        else
        {
            churchDB.modDonorKarma( donor, donationAmount );
            karmaDB.modNounKarma( donor, -1 * donationAmount );
            Long churchDonorKarma = churchDB.getDonorKarma( donor ).orElse(null);
            if ( churchDonorKarma != null && churchDonorKarma - donationAmount <= 0 )
            {
                churchDB.modDonorTitle( donor, " " );
            }
            return "Thank you for your donation of " + donationAmount + " " + donor + "!";
        }
    }

    @OnMessage("!churchtotal")
    public String churchTotal() {
        long totalDonations = churchDB.getTotalDonations();
        long churchTotal = totalDonations + churchDB.getTotalNonDonations();
        return "The church coffers contains " + churchTotal + ", of which " + totalDonations
                + " was contributed by its loyal believers.";
    }

    @OnMessage("!topdonors")
    public String topDonors() {
        Set<String> nouns;
        nouns = churchDB.getDonorsByRanks( 0L, 3L );
        if ( nouns != null && nouns.size() > 0 )
        {
            StringBuilder out = new StringBuilder();
            out.append( "NICK           AMOUNT    TITLE\n" );
            for ( String noun : nouns )
            {
                out.append(getReplyWithRankAndDonationAmount( noun ));
                out.append('\n');
            }
            out.deleteCharAt(out.length() - 1);
            return out.toString();
        }
        else
        {
            return "The church does not have enough followers yet.";
        }
    }

    @OnMessage("!settitle @newTitle")
    public String setTitle(User user, String newTitle) {
        String donor = Nick.getNick( user ).getBaseNick();
        Long donorRank = churchDB.getDonorRank( donor ).orElse(null);
        if ( donorRank != null && donorRank > 4 )
        {
            return "You must be a top donor to set your title.";
        }

        if ( newTitle.length() > 40 )
        {
            newTitle = newTitle.substring( 0, 39 );
        }

        String oldTitle = churchDB.getDonorTitle( donor );
        churchDB.modDonorTitle( donor, newTitle );
        return donor + " is now to be referred to as " + newTitle + " instead of " + oldTitle + ".";
    }

    @OnChannelMessage("!inquisit @toInquisit")
    public String inquisit( ChannelMessageEvent event, User user, Nick toInquisit )
    {
        if ( currentVote != null )
        {
            return "There is an ongoing inquisition against " + currentVote.getDecisionTarget()
                    + ". Please use !aye or !nay to decide their fate";
        }

        String donor = Nick.getNick( user ).getBaseNick();
        Long donorRank = churchDB.getDonorRank( donor ).orElse(null);
        if ( donorRank == null || donorRank > 4 )
        {
            return "You must be a top donor to start an inquisition.";
        }

        StringBuilder membersToPing = new StringBuilder();
        Set<String> nouns = churchDB.getDonorsByRanks( 0L, 3L );
        if ( nouns != null && nouns.size() > 3 )
        {
            for ( String noun : nouns )
            {
                membersToPing.append( noun ).append( " " );
            }
        }
        else
        {
            return "There are not enough members of the church to invoke a vote";
        }

        String inquisitedNick = toInquisit.getBaseNick();
        Long nounKarma = churchDB.getDonorKarma( inquisitedNick ).orElse(null);
        if ( nounKarma == null || nounKarma == 0 )
        {
            return "You cannot start an inquisition against someone who has no donation value.";
        }

        startInquisitionVote( event.getChannel(), inquisitedNick, donorRank );
        return "An inquisition has started against " + inquisitedNick
                + ". Please cast your votes with !aye or !nay\n" + membersToPing;

    }

    private synchronized void startInquisitionVote( Channel channel, String inquisitedNick, Long donorRank )
    {
        currentVote = new VoteEvent();
        currentVote.setDecisionTarget( inquisitedNick );
        currentVote.updateVoteAye( donorRank.intValue() );
        startTimer( channel );
    }

    @OnChannelMessage("!aye")
    public String voteAye(User user) {
        return vote(user, true);
    }

    @OnChannelMessage("!nay")
    public String voteNay(User user) {
        return vote(user, false);
    }

    public String vote(User user, boolean isAye) {
        if ( currentVote == null )
        {
            return "There is no ongoing vote.";
        }

        String donor = Nick.getNick( user ).getBaseNick();
        Long donorRank = churchDB.getDonorRank( donor ).orElse(null);
        if ( donorRank == null || donorRank > 4 )
        {
            return "You must be a top donor to vote.";
        }

        if ( isAye )
        {
            currentVote.updateVoteAye( donorRank.intValue() );
        }
        else
        {
            currentVote.updateVoteNay( donorRank.intValue() );
        }

        StringBuilder currentChoices = new StringBuilder();
        int i = 1;
        Set<String> nouns = churchDB.getDonorsByRanks( 0L, 3L );
        for ( String noun : Objects.requireNonNull( nouns ) )
        {
            currentChoices.append( noun ).append( " (" ).append( currentVote.getDonorVote( i++ ) ).append( ") " );
        }
        return currentChoices.toString();
    }

    private void startTimer( final Channel channel )
    {
        timeService.onTimeout(TIMEOUT, () -> timeoutTimer(channel));
    }

    private void timeoutTimer( Channel channel )
    {
        String message = "Voting is now finished\n";
        if ( currentVote.isDecisionYes() )
        {
            message += "The vote to inquisit " + currentVote.getDecisionTarget() + " has passed. "
                    + currentVote.getDecisionTarget() + " will be stripped of their karma.";
            Long donorKarma = churchDB.getDonorKarma( currentVote.getDecisionTarget() ).orElse(null);
            if ( donorKarma != null )
            {
                churchDB.modNonDonorKarma( donorKarma.intValue() );
                churchDB.removeDonor( currentVote.getDecisionTarget() );
            }
        }
        else
        {
            message += "The vote to inquisit " + currentVote.getDecisionTarget() + " has failed. Nothing will happen.";
        }
        for ( String line : message.split( "\n" ) )
        {
            channel.sendMessage( line );
        }
        currentVote = null;
    }

    private String getReplyWithRankAndDonationAmount( String noun )
    {
        Long nounKarma = churchDB.getDonorKarma( noun ).orElse(null);
        String nounTitle = churchDB.getDonorTitle( noun );
        StringBuilder sb = new StringBuilder();
        sb.append( noun );
        sb.reverse();
        String nounString = sb.toString();
        if ( nounKarma != null )
        {
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
            return nounString + spaces_first + nounKarma + spaces_second + nounTitle;
        } else {
            return nounString + " has no donations";
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
