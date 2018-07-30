package com.oldterns.vilebot.handlers.user;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.db.ChurchDB;
import com.oldterns.vilebot.util.BaseNick;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.interfaces.Replyable;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@HandlerContainer
public class Church
{
    private static final Pattern nounPattern = Pattern.compile( "\\S+" );
    private static final Pattern donatePattern = Pattern.compile( "^!donate (-?[0-9]+)\\s*" );
    private static final Pattern churchTotalPattern = Pattern.compile( "^!churchtotal" );
    private static final Pattern topDonorsPattern = Pattern.compile( "^!topdonors" );
    private static final Pattern setTitlesPattern = Pattern.compile( "^!settitle (.+)$" );
    private static final Pattern inquisitPattern = Pattern.compile( "^!inquisit (" + nounPattern + ")\\s*$" );
    private static final Pattern topDonorsAyePattern = Pattern.compile( "^!aye" );
    private static final Pattern topDonorsNayPattern = Pattern.compile( "^!nay" );

    private static final long TIMEOUT  = 30000L;
    private static ExecutorService timer = Executors.newScheduledThreadPool(1);

    private static VoteEvent currentVote = null;

    @Handler
    private void donateToChurch(ReceivePrivmsg event){
        Matcher matcher = donatePattern.matcher(event.getText());
        if (matcher.matches()) {
            if (currentVote != null) {
                event.reply("There is an ongoing vote, you cannot donate at this time.");
                return;
            }

            String rawDonationAmount = matcher.group(1);
            String donor = BaseNick.toBaseNick(event.getSender());
            Integer donationAmount = Integer.parseInt(rawDonationAmount);
            Integer donorKarma = KarmaDB.getNounKarma(donor);
            donorKarma = donorKarma == null ? 0 : donorKarma;
            if (donationAmount <= 0) {
                event.reply("You cannot donate a non-positive number, imbecile.");
            }
            else if (donorKarma <= 0 || donorKarma < donationAmount) {
                event.reply("You have insufficient karma to donate.");
            }
            else {
                ChurchDB.modDonorKarma(donor, donationAmount);
                KarmaDB.modNounKarma(donor, -1 * donationAmount);
                if (ChurchDB.getDonorKarma(donor) - donationAmount <= 0) {
                     ChurchDB.modDonorTitle (donor, " ");
                }
                event.reply("Thank you for your donation of " + donationAmount + " " + donor + "!");
            }
        }
    }

    @Handler
    private void viewChurchTotal(ReceivePrivmsg event) {
        Matcher matcher = churchTotalPattern.matcher(event.getText());
        if (matcher.matches()) {
            long totalDonations = ChurchDB.getTotalDonations();
            long churchTotal = totalDonations + ChurchDB.getTotalNonDonations();
            event.reply("The church coffers contains " + churchTotal + ", of which " + totalDonations
                        + " was contributed by its loyal believers.");
        }
    }

    @Handler
    private void viewTopDonors(ReceivePrivmsg event) {
        Matcher matcher = topDonorsPattern.matcher(event.getText());
        if (matcher.matches()) {
            Set<String> nouns = null;
            nouns = ChurchDB.getDonorsByRanks(0, 3);
            if (nouns != null && nouns.size() > 0) {
                event.reply("NICK           AMOUNT    TITLE");
                for (String noun : nouns) {
                    replyWithRankAndDonationAmount(noun, event);
                }
            }
            else {
                event.reply("The church does not have enough followers yet.");
            }
        }
    }

    @Handler
    private void setTitle(ReceivePrivmsg event) {
        Matcher matcher = setTitlesPattern.matcher(event.getText());
        if (matcher.matches()) {
           String donor = BaseNick.toBaseNick(event.getSender());
           if (ChurchDB.getDonorRank( donor ) > 4) {
               event.reply ("You must be a top donor to set your title.");
               return;
           }

           String newTitle = matcher.group(1);
           if (newTitle.length() > 40) {
                newTitle = newTitle.substring(0,39);
           }

           String oldTitle = ChurchDB.getDonorTitle (donor);
           ChurchDB.modDonorTitle(donor, newTitle);
           event.reply( donor + " is now to be referred to as " + newTitle + " instead of " + oldTitle + ".");
        }
    }

    @Handler
    private void inquisit(ReceivePrivmsg event) {
        Matcher matcher = inquisitPattern.matcher(event.getText());
        if (matcher.matches()) {
            if(currentVote != null) {
                event.reply("There is an ongoing inquisition against " + currentVote.getDecisionTarget() + ". Please use !aye or !nay to decide their fate");
                return;
            }

            String donor = BaseNick.toBaseNick(event.getSender());
            Integer donorRank = ChurchDB.getDonorRank(donor);
            if (donorRank == null || donorRank > 4) {
                event.reply("You must be a top donor to start an inquisition.");
                return;
            }

            String membersToPing = "";
            Set<String> nouns = ChurchDB.getDonorsByRanks(0, 3);
            if (nouns != null && nouns.size() > 3) {
                for (String noun : nouns) {
                    membersToPing += noun + " ";
                }
            }
            else {
                event.reply("There are not enough members of the church to invoke a vote");
                return;
            }

            String inquisitedNick = BaseNick.toBaseNick(matcher.group(1));
            Integer nounKarma = ChurchDB.getDonorKarma(inquisitedNick);
            if (nounKarma == null || nounKarma == 0) {
                event.reply("You cannot start an inquisition against someone who has no donation value.");
                return;
            }

            event.reply("An inquisition has started against " + inquisitedNick + ". Please cast your votes with !aye or !nay");
            event.reply(membersToPing);
            startInquisitionVote(event, inquisitedNick, donorRank);
        }
    }

    private synchronized void startInquisitionVote(ReceivePrivmsg event, String inquisitedNick, int donorRank) {
        currentVote = new VoteEvent();
        currentVote.setDecisionTarget(inquisitedNick);
        currentVote.updateVoteAye(donorRank);
        startTimer(event);
    }

    @Handler
    public void inquisitDecision(ReceivePrivmsg event) {
        Matcher matcherAye = topDonorsAyePattern.matcher(event.getText());
        Matcher matcherNay = topDonorsNayPattern.matcher(event.getText());
        if (matcherAye.matches() || matcherNay.matches()) {
            if(currentVote == null) {
                event.reply("There is no ongoing vote.");
                return;
            }

            String donor = BaseNick.toBaseNick(event.getSender());
            Integer donorRank = ChurchDB.getDonorRank(donor);
            if (donorRank == null || donorRank > 4) {
                event.reply("You must be a top donor to vote.");
                return;
            }

            if(matcherAye.matches()) {
                currentVote.updateVoteAye(donorRank);
            }
            else if(matcherNay.matches()) {
                currentVote.updateVoteNay(donorRank);
            }

            String currentChoices = "";
            int i = 1;
            Set<String> nouns = ChurchDB.getDonorsByRanks(0, 3);
            for (String noun : nouns) {
                currentChoices += noun + " (" + currentVote.getDonorVote(i++) + ") ";
            }
            event.reply(currentChoices);
        }
    }

    private void startTimer(final ReceivePrivmsg event) {
        timer.submit(new Runnable() {
            @Override
            public void run() {
                try {
		    Thread.sleep(TIMEOUT);
                    timeoutTimer(event);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void timeoutTimer(ReceivePrivmsg event) {
        String message = "Voting is now finished\n";
        if(currentVote.isDecisionYes()) {
            message += "The vote to inquisit " + currentVote.getDecisionTarget() + " has passed. " + currentVote.getDecisionTarget() + " will be stripped of their karma.";
            ChurchDB.modNonDonorKarma(ChurchDB.getDonorKarma(currentVote.getDecisionTarget()));
            ChurchDB.removeDonor(currentVote.getDecisionTarget());
        }
        else {
            message += "The vote to inquisit " + currentVote.getDecisionTarget() + " has failed. Nothing will happen.";
        }

        event.reply(message);
        currentVote = null;
    }

    private static boolean replyWithRankAndDonationAmount(String noun, Replyable event) {
        Integer nounKarma = ChurchDB.getDonorKarma(noun);
        String nounTitle = ChurchDB.getDonorTitle(noun);
        if (nounKarma != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(noun);
            sb.reverse();
            String nounString = sb.toString();
            int nounLength = sb.length();
            int spaceLength;
            if (nounLength <= 14) {
                spaceLength = 15 - nounLength;
            }
            else {
                nounString = nounString.substring(0, 14);
                spaceLength = 1;
            }

            String spaces_first = String.format("%" + spaceLength + "s", "");
            if (nounKarma.toString().length() <= 9) {
                spaceLength = 10 - nounKarma.toString().length();
            }

            String spaces_second = String.format("%" + spaceLength + "s", "");
            event.reply( nounString + spaces_first + nounKarma.toString() + spaces_second + nounTitle);
            return true;
        }
        return false;
    }

    private static class VoteEvent {
        private boolean[] topDonorDecisions;
        private String decisionTarget;

        public VoteEvent() {
            topDonorDecisions = new boolean[4];
            for (int i = 0; i < 4; i++) {
                topDonorDecisions[i] = false;
            }
            decisionTarget = null;
        }

        public void updateVoteAye(int donorRank){
            topDonorDecisions[donorRank-1] = true;
        }

        public void updateVoteNay(int donorRank){
            topDonorDecisions[donorRank-1] = false;
        }

        public boolean getDonorVote(int donorRank) {
                return topDonorDecisions[donorRank-1];
        }

        public boolean isDecisionYes(){
            int totalValue = 0;
            if (topDonorDecisions[0]) totalValue += 5;
            if (topDonorDecisions[1]) totalValue += 3;
            if (topDonorDecisions[2]) totalValue += 3;
            if (topDonorDecisions[3]) totalValue += 1;
            if (totalValue > 6) return true;
            else return false;
        }

        public void setDecisionTarget(String target) {
            decisionTarget = target;
        }

        public String getDecisionTarget() {
            return decisionTarget;
        }
    }
}
