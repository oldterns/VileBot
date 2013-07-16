/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class KarmaRoll
{
    private static final Pattern rollPattern = Pattern.compile( "!roll(?: for|)(?: +([0-9]+)|)" );

    private static final Pattern cancelPattern = Pattern.compile( "!roll {0,1}cancel" );

    private static final int UPPER_WAGER = 10;

    private RollGame currentGame;

    private Object currentGameMutex = new Object();

    @Handler
    private void userHelp( ReceivePrivmsg event )
    {
        String sender = BaseNick.toBaseNick( event.getSender() );
        String text = event.getText();
        Matcher matcher = rollPattern.matcher( text );

        if ( matcher.matches() )
        {
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
                    if ( wager > UPPER_WAGER || wager < 1 )
                    {
                        event.reply( wager + " isn't a valid wager. Wagers must be between 1 and " + UPPER_WAGER
                            + " (both inclusive)." );
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
                        // A game exists, and no karma value was given. User is accepting the active wager/game.

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

                                KarmaDB.modNounKarma( winner, deltaKarma );
                                KarmaDB.modNounKarma( loser, -1 * deltaKarma );
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

    @Handler
    private void manualCancel( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = cancelPattern.matcher( text );

        if ( matcher.matches() )
        {
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
}
