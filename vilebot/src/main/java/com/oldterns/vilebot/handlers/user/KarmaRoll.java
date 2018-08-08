/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.db.KarmalyticsDB;
import com.oldterns.vilebot.karmalytics.HasKarmalytics;
import com.oldterns.vilebot.karmalytics.KarmalyticsRecord;
import com.oldterns.vilebot.util.BaseNick;
import net.engio.mbassy.listener.Handler;

@HandlerContainer
public class KarmaRoll
    implements HasKarmalytics
{
    private static final Pattern rollPattern = Pattern.compile( "!roll(?: for|)(?: +([0-9]+)|)" );

    private static final Pattern cancelPattern = Pattern.compile( "!roll {0,1}cancel" );

    private static final int UPPER_WAGER = 10;

    private RollGame currentGame;

    private Object currentGameMutex = new Object();

    public KarmaRoll()
    {
        KarmalyticsDB.intializeKarmalyticsFor( this );
    }

    @Handler
    private void userHelp( ReceivePrivmsg event )
    {
        String sender = BaseNick.toBaseNick( event.getSender() );
        String text = event.getText();
        Matcher matcher = rollPattern.matcher( text );

        if ( matcher.matches() )
        {
            // Infers ircChannel1 in JSON is #thefoobar for production Vilebot
            if ( !event.getChannel().matches( Vilebot.getConfig().get( "ircChannel1" ) ) )
            {
                event.reply( "You must be in " + Vilebot.getConfig().get( "ircChannel1" )
                    + " to make or accept wagers." );
                return;
            }

            String rawWager = matcher.group( 1 );

            synchronized ( currentGameMutex )
            {
                if ( currentGame == null )
                {
                    // No existing game

                    if ( rawWager == null )
                    {
                        // No game exists, but the user has not given a karma wager, so default to 10
                        rawWager = "10";
                    }

                    // Check the wager value is in the acceptable range.
                    Integer wager = Integer.parseInt( rawWager );
                    Integer senderKarma = KarmaDB.getNounKarma( sender );
                    senderKarma = senderKarma == null ? 0 : senderKarma;

                    if ( !validWager( wager, senderKarma ) )
                    {
                        event.reply( wager
                            + " isn't a valid wager. Must be greater than 0. If you wager is larger than " + UPPER_WAGER
                            + " you must have at least as much karma as your wager." );
                    }
                    else
                    {
                        // Acceptable wager, start new game

                        currentGame = new RollGame( sender, wager );
                        event.reply( sender + " has rolled with " + wager + " karma points on the line.  Who's up?" );
                    }
                }
                else
                {
                    // A game exists

                    if ( rawWager != null )
                    {
                        // A game exists, but the user has given a karma wager, probably an error.

                        StringBuilder sb = new StringBuilder();
                        sb.append( "A game is already active; started by " );
                        sb.append( currentGame.getFirstPlayerNick() );
                        sb.append( " for " );
                        sb.append( currentGame.getWager() );
                        sb.append( " karma. Use !roll to accept." );
                        event.reply( sb.toString() );
                    }
                    else
                    {
                        // A game exists, and no karma value was given. User is accepting the active
                        // wager/game.

                        // The user that started a game cannot accept it
                        if ( currentGame.getFirstPlayerNick().equals( sender ) )
                        {
                            event.replyDirectly( "You can't accept your own wager." );
                        }
                        else
                        {
                            GameResults result = currentGame.setSecondPlayer( sender );

                            String firstPlayer = currentGame.getFirstPlayerNick();
                            int firstRoll = result.getFirstPlayerRoll();
                            String secondPlayer = currentGame.getSecondPlayerNick();
                            int secondRoll = result.getSecondPlayerRoll();

                            String winner = result.getWinnerNick();
                            String loser = result.getLoserNick();
                            int deltaKarma = currentGame.getWager();

                            StringBuilder sb = new StringBuilder();
                            sb.append( "Results: " );
                            sb.append( firstPlayer );
                            sb.append( " rolled " );
                            sb.append( firstRoll );
                            sb.append( ", and " );
                            sb.append( secondPlayer );
                            sb.append( " rolled " );
                            sb.append( secondRoll );
                            sb.append( ". " );

                            if ( winner != null && loser != null )
                            {
                                sb.append( winner );
                                sb.append( " takes " );
                                sb.append( deltaKarma );
                                sb.append( " from " );
                                sb.append( loser );
                                sb.append( "!!!" );

                                modNounKarma( winner, loser, deltaKarma );
                                modNounKarma( loser, winner, -1 * deltaKarma );
                            }
                            else
                            {
                                sb.append( "A tie!" );
                            }

                            event.reply( sb.toString() );

                            // Reset
                            currentGame = null;

                            event.reply( "Play again?" );
                        }
                    }
                }
            }
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
     * @return
     */
    private boolean validWager( int wager, int senderKarma )
    {
        return !( wager > 10 ) && ( wager > 0 ) || ( wager > 10 ) && ( senderKarma >= wager );
    }

    @Handler
    private void manualCancel( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = cancelPattern.matcher( text );

        if ( matcher.matches() )
        {
            if ( !currentGame.getFirstPlayerNick().equals( event.getSender() ) )
            {
                event.reply( "Only " + currentGame.getFirstPlayerNick() + " may cancel this game." );
                return;
            }
            synchronized ( currentGameMutex )
            {
                currentGame = null;
            }
            event.reply( "Roll game cancelled." );
        }
    }

    private static class RollGame
    {
        private final int wager;

        private String firstPlayer;

        private String secondPlayer;

        public RollGame( String firstPlayerNick, int wager )
        {
            if ( firstPlayerNick == null )
                throw new IllegalArgumentException( "firstPlayerNick can't be null" );

            this.firstPlayer = firstPlayerNick;
            this.wager = wager;
        }

        public GameResults setSecondPlayer( String secondPlayerNick )
        {
            if ( secondPlayerNick == null )
                throw new IllegalArgumentException( "secondPlayerNick can't be null" );

            if ( secondPlayer == null )
                secondPlayer = secondPlayerNick;
            else
                throw new StateViolation( "Can't set the second player twice" );

            return new GameResults( firstPlayer, secondPlayer );
        }

        public String getFirstPlayerNick()
        {
            return firstPlayer;
        }

        public String getSecondPlayerNick()
        {
            return secondPlayer;
        }

        public int getWager()
        {
            return wager;
        }
    }

    private static class GameResults
    {
        private static final int SIDES_OF_DIE = 6;

        private static final Random random = new SecureRandom();

        private final String firstPlayer;

        private final String secondPlayer;

        private Integer firstPlayerRoll = null;

        private Integer secondPlayerRoll = null;

        public GameResults( String firstPlayer, String secondPlayer )
        {
            this.firstPlayer = firstPlayer;
            this.secondPlayer = secondPlayer;

            doRolls();
        }

        /**
         * @return The nick of the winning player, or null on a tie
         */
        public String getWinnerNick()
        {
            if ( firstPlayerRoll > secondPlayerRoll )
                return firstPlayer;
            else if ( secondPlayerRoll > firstPlayerRoll )
                return secondPlayer;
            else
                return null; // Tied
        }

        /**
         * @return The nick of the losing player, or null on a tie
         */
        public String getLoserNick()
        {
            if ( firstPlayerRoll < secondPlayerRoll )
                return firstPlayer;
            else if ( secondPlayerRoll < firstPlayerRoll )
                return secondPlayer;
            else
                return null; // Tied
        }

        /**
         * @return The value of the dice roll the first player got
         */
        public int getFirstPlayerRoll()
        {
            return firstPlayerRoll;
        }

        /**
         * @return The value of the dice roll the second player got
         */
        public int getSecondPlayerRoll()
        {
            return secondPlayerRoll;
        }

        private void doRolls()
        {
            firstPlayerRoll = random.nextInt( SIDES_OF_DIE ) + 1;
            secondPlayerRoll = random.nextInt( SIDES_OF_DIE ) + 1;
        }
    }

    private static class StateViolation
        extends RuntimeException
    {
        private static final long serialVersionUID = -7530159745349382310L;

        public StateViolation( String message )
        {
            super( message );
        }
    }

    @Override
    public List<String> getGroups()
    {
        return Arrays.asList( "Gambling" );
    }

    @Override
    public String getKarmalyticsId()
    {
        return "Betting Karma on Rolls";
    }

    @Override
    public Function<KarmalyticsRecord, String> getRecordDescriptorFunction()
    {
        return ( r ) -> {
            StringBuilder out = new StringBuilder();
            out.append( r.getNick() );
            out.append( " " );
            if ( r.getKarmaModAmount() > 0 )
            {
                out.append( "won a roll bet against " );
            }
            else
            {
                out.append( "lost a roll bet against " );
            }
            out.append( r.getExtraInfo() );
            out.append( " for " );
            out.append( Math.abs( r.getKarmaModAmount() ) );
            out.append( " karma." );
            return out.toString();
        };
    }
}
