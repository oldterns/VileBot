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

@HandlerContainer
public class Church
{
    private static final Pattern donatePattern = Pattern.compile( "^!donate (-?[0-9]+)\\s*" );
    private static final Pattern churchTotalPattern = Pattern.compile( "^!churchtotal" );
    private static final Pattern topDonorsPattern = Pattern.compile( "^!topdonors" );
    private static final Pattern setTitlesPattern = Pattern.compile( "^!settitle (.+)$" );

    // churchKarmaEntryString will be stored in the database for all non-donated karma (tax, forfeiture)
    private static final String churchKarmaEntryString = "Church of VileBot";

    @Handler
    private void donateToChurch( ReceivePrivmsg event )
    {
        Matcher matcher = donatePattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            String rawDonationAmount = matcher.group( 1 );
            String donor = BaseNick.toBaseNick( event.getSender() );

            Integer donationAmount = Integer.parseInt( rawDonationAmount );
            Integer donorKarma = KarmaDB.getNounKarma(donor);
            donorKarma = donorKarma == null ? 0 : donorKarma;

            if ( donationAmount <= 0)
            {
                event.reply("You cannot donate a non-positive number, imbecile.");
            }
            else if ( donorKarma <= 0 || donorKarma < donationAmount)
            {
                event.reply("You have insufficient karma to donate.");
            }
            else
            {
                ChurchDB.modDonorKarma( donor, donationAmount );
                KarmaDB.modNounKarma( donor, -1 * donationAmount );
                if ( ChurchDB.getDonorKarma ( donor ) - donationAmount <= 0 )
                {
                     ChurchDB.modDonorTitle ( donor, " " );
                }
                event.reply("Thank you for your donation of " + donationAmount + " " + donor
                            + "! You are now rank " + ChurchDB.getDonorRank(donor) + " in donations.");
            }
        }
    }

    @Handler
    private void viewChurchTotal( ReceivePrivmsg event )
    {
        Matcher matcher = churchTotalPattern.matcher( event.getText() );

        if ( matcher.matches() )
        {
            long totalDonations = ChurchDB.getTotalDonations();
            long churchTotal = totalDonations + ChurchDB.getTotalNonDonations();
            event.reply("The church coffers contains " + churchTotal + ", of which " + totalDonations
                        + " was contributed by its loyal believers.");
        }
    }

    @Handler
    private void viewTopDonors( ReceivePrivmsg event )
    {
        Matcher matcher = topDonorsPattern.matcher( event.getText() );
        if ( matcher.matches() )
        {
            Set<String> nouns = null;

            nouns = ChurchDB.getDonorsByRanks( 0, 3 );

            if ( nouns != null && nouns.size() > 0 )
            {
                event.reply("NICK           AMOUNT    TITLE");
                for ( String noun : nouns )
                {
                    replyWithRankAndDonationAmount( noun, event );
                }
            }
            else
            {
                event.reply( "The church does not have enough followers yet." );
            }
        }
    }

    @Handler
    private void setTitle( ReceivePrivmsg event )
    {
        Matcher matcher = setTitlesPattern.matcher( event.getText() );
        if ( matcher.matches() )
        {
           String donor = BaseNick.toBaseNick( event.getSender() );
           if ( ChurchDB.getDonorRank( donor ) > 4 )
           {
               event.reply ( "You must be a top donor to set your title." );
               return;
           }

           String newTitle = matcher.group( 1 );
           if ( newTitle.length() > 40 )
           {
                newTitle = newTitle.substring(0,39);
           }
           String oldTitle = ChurchDB.getDonorTitle ( donor );
           ChurchDB.modDonorTitle( donor, newTitle );
           event.reply( donor + " is now to be referred to as " + newTitle + " instead of " + oldTitle + ".");
        }
    }

    private static boolean replyWithRankAndDonationAmount( String noun, Replyable event )
    {
        Integer nounRank = ChurchDB.getDonorRank( noun );

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
                nounString = nounString.substring(0, 14);
                spaceLength = 1;
            }

            String spaces_first = String.format("%" + spaceLength + "s", "");

            if (nounKarma.toString().length() <= 9)
            {
                spaceLength = 10 - nounKarma.toString().length();
            }

            String spaces_second = String.format("%" + spaceLength + "s", "");
            event.reply( nounString + spaces_first + nounKarma.toString() + spaces_second + nounTitle);
            return true;
        }
        return false;
    }
}
