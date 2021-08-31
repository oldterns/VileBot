package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.RandomProvider;
import org.kitteh.irc.client.library.element.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
public class KarmaRollService
{
    public final static int UPPER_WAGER = 10;

    @Inject
    KarmaDB karmaDB;

    @Inject
    RandomProvider randomProvider;

    Nick gameInitatorNick;

    Lock gameLock = new ReentrantLock();

    int wager;

    // ? before wager to make the space optional (for the "!roll" variant)
    @OnChannelMessage( "!roll ?@bet" )
    public String roll( User user, Optional<Integer> bet )
    {
        gameLock.lock();
        try
        {
            Nick sender = Nick.getNick( user );
            if ( isGameStarted() )
            {
                if ( bet.isPresent() )
                {
                    return "A game is already active; started by " + gameInitatorNick.getBaseNick() + " for " + wager
                        + " karma. Use !roll to accept.";
                }
                if ( Nick.getNick( user ).getBaseNick().equals( gameInitatorNick.getBaseNick() ) )
                {
                    return "You can't accept your own wager.";
                }
                return getGameResult( gameInitatorNick, Nick.getNick( user ) );
            }
            else
            {
                int actualBet = bet.orElse( UPPER_WAGER );
                Long senderKarma = karmaDB.getNounKarma( sender.getBaseNick() ).orElse( 0L );

                if ( !validWager( actualBet, senderKarma ) )
                {
                    return actualBet + " isn't a valid wager. Must be greater than 0. If your wager is larger than "
                        + UPPER_WAGER + " you must have at least as much karma as your wager.";
                }
                gameInitatorNick = sender;
                wager = actualBet;
                return sender.getBaseNick() + " has rolled with " + wager + " karma points on the line.  Who's up?";
            }
        }
        finally
        {
            gameLock.unlock();
        }
    }

    @OnChannelMessage( "!rollcancel" )
    public String rollCancel( User user )
    {
        gameLock.lock();
        try
        {
            if ( gameInitatorNick == null )
            {
                return "No roll game is active.";
            }
            Nick sender = Nick.getNick( user );
            if ( !gameInitatorNick.getBaseNick().equals( sender.getBaseNick() ) )
            {
                return "Only " + gameInitatorNick.getBaseNick() + " may cancel this game.";
            }
            endGame();
            return "Roll game cancelled.";
        }
        finally
        {
            gameLock.unlock();
        }
    }

    /**
     * A valid wager is one that meets the following standards: 1. Is greater than 0. 2. If it is greater than 10, then
     * the user's karma is equal to or greater than the wager. The reasoning behind this is to have some base amount of
     * karma that user's can bet with, but also provide a way of betting large amounts of karma. To avoid destroying the
     * karma economy users cannot bet more than their current amount of karma.
     *
     * @param wager the amount wagered
     * @param senderKarma the wagerer's karma
     */
    private boolean validWager( int wager, long senderKarma )
    {
        return !( wager > UPPER_WAGER ) && ( wager > 0 ) || ( wager > UPPER_WAGER ) && ( senderKarma >= wager );
    }

    private String getGameResult( Nick user1, Nick user2 )
    {
        Nick winner = null;
        Nick loser = null;

        int user1Roll = randomProvider.getRandomInt( 1, 6 );
        int user2Roll = randomProvider.getRandomInt( 1, 6 );

        if ( user1Roll > user2Roll )
        {
            winner = user1;
            loser = user2;
        }
        else if ( user1Roll < user2Roll )
        {
            winner = user2;
            loser = user1;
        }

        StringBuilder sb = new StringBuilder();
        sb.append( "Results: " );
        sb.append( user1.getBaseNick() );
        sb.append( " rolled " );
        sb.append( user1Roll );
        sb.append( ", and " );
        sb.append( user2.getBaseNick() );
        sb.append( " rolled " );
        sb.append( user2Roll );
        sb.append( ". " );

        if ( winner != null && loser != null )
        {
            sb.append( winner );
            sb.append( " takes " );
            sb.append( wager );
            sb.append( " from " );
            sb.append( loser );
            sb.append( "!!!" );

            karmaDB.modNounKarma( winner.getBaseNick(), wager );
            karmaDB.modNounKarma( loser.getBaseNick(), -1 * wager );
        }
        else
        {
            sb.append( "A tie!" );
        }
        sb.append( '\n' );
        sb.append( "Play again?" );
        endGame();
        return sb.toString();
    }

    private boolean isGameStarted()
    {
        return gameInitatorNick != null;
    }

    private void endGame()
    {
        gameInitatorNick = null;
    }
}
