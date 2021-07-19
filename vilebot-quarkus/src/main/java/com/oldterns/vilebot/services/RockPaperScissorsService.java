package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.annotations.OnPrivateMessage;
import com.oldterns.vilebot.annotations.Regex;
import com.oldterns.vilebot.util.RandomProvider;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Christopher Chianelli on 27/08/17. Thanks to Josh Matsuoka for helping squashing bugs RockPaperScissors
 * implementation based off of Trivia.java
 */
@ApplicationScoped
public class RockPaperScissorsService
{

    private static final String RED = "\u000304";

    private static final String RESET = "\u000f";

    private static final String BLUE = "\u000302";

    private static final String GREEN = "\u000303";

    @Inject
    RandomProvider randomProvider;

    Lock gameLock = new ReentrantLock();

    RPSGame currentGame;

    @OnMessage( "!rpsrules" )
    public void requestRules( ActorEvent<User> event )
    {
        for ( String message : getRules( event ).split( "\n" ) )
        {
            event.getActor().sendMessage( message );
        }
    }

    @OnChannelMessage( "!rpscancel" )
    public String cancelGame( User user )
    {
        try
        {
            gameLock.lock();
            if ( null != currentGame )
            {
                String caller = Nick.getNick( user ).getBaseNick();
                if ( caller.equals( currentGame.getCaller() ) || caller.equals( currentGame.getDared() ) )
                {
                    currentGame = null;
                    return "RPS game cancelled";
                }
                else
                {
                    return "Only " + currentGame.getCaller() + " or " + currentGame.getDared()
                        + " can cancel this game.";
                }
            }
            else
            {
                return "No active game. Start a new one with !rps dared";
            }
        }
        finally
        {
            gameLock.unlock();
        }
    }

    @OnChannelMessage( "!rps @dared ?@daredThing" )
    public String startRpsGame( ChannelMessageEvent event, Nick dared, Optional<String> daredThing )
    {
        try
        {
            gameLock.lock();
            if ( currentGame == null )
            {
                String caller = Nick.getNick( event ).getBaseNick();
                if ( caller.equals( dared.getBaseNick() ) )
                {
                    return "You cannot challenge yourself to a Rock Paper Scissors game!";
                }
                if ( daredThing.isPresent() )
                {
                    currentGame =
                        new RPSGame( caller, dared.getBaseNick(), daredThing.get().trim(), event, randomProvider );
                }
                else
                {
                    currentGame = new RPSGame( caller, dared.getBaseNick(), null, event, randomProvider );
                }
                return currentGame.getRPSIntro() + getSubmissionRuleString( event );
            }
            else
            {
                return currentGame.alreadyPlaying();
            }
        }
        finally
        {
            gameLock.unlock();
        }
    }

    @OnPrivateMessage( "!@submission" )
    public String onContestantAnswer( User contestantUser, @Regex( "rock|paper|scissors" ) String submission )
    {
        try
        {
            gameLock.lock();
            String contestant = Nick.getNick( contestantUser ).getBaseNick();
            if ( currentGame != null )
            {
                try
                {
                    boolean endGame = currentGame.setSubmission( contestant, submission );
                    if ( endGame )
                    {
                        currentGame.getEvent().sendReply( currentGame.getRPSOutro() );
                        currentGame = null;
                    }
                    return String.format( "Your submission of %s has been recieved!", submission );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    return e.getMessage();
                }
            }
            else
            {
                return "No active game. Start a new one with !rps dared [bet]";
            }
        }
        finally
        {
            gameLock.unlock();
        }
    }

    private String getRules( ActorEvent<?> event )
    {
        return RED + "RPS RULES: \n" + RESET + "1) Dare someone to play rock paper scissors with you! \n" + RESET
            + "2) Rock beats scissors, paper beats rocks, and scissors beat paper \n" + "3) Use /msg "
            + event.getClient().getNick() + " !(rock|paper|scissors) to set your action. Cannot be undone.";
    }

    private String getSubmissionRuleString( ActorEvent<?> event )
    {
        return RED + "Use \" /msg " + RESET + event.getClient().getNick() + RED
            + " !(rock|paper|scissors) \" to submit." + RESET;
    }

    private static class RPSGame
    {
        private static final String[] answers = { "rock", "paper", "scissors" };

        private String callerNick;

        private String daredNick;

        private String callerAnswer;

        private String daredAnswer;

        private String daredThing;

        private ChannelMessageEvent event;

        Random rand = new Random();

        RPSGame( String callerNick, String daredNick, String daredThing, ChannelMessageEvent event,
                 RandomProvider randomProvider )
        {
            this.callerNick = callerNick;
            this.daredNick = daredNick;
            this.callerAnswer = null;
            this.daredAnswer = null;
            this.daredThing = daredThing;
            this.event = event;
            rand = new Random();
            if ( daredNick.equals( event.getClient().getNick() ) )
            {
                this.daredAnswer = randomProvider.getRandomElement( answers );
            }
        }

        private String getCaller()
        {
            return callerNick;
        }

        private String getDared()
        {
            return daredNick;
        }

        private ChannelMessageEvent getEvent()
        {
            return event;
        }

        private String getRPSIntro()
        {
            String start = RED + callerNick + " dared " + daredNick + " to a Rock Paper Scissors match";
            if ( daredThing != null )
            {
                return start + " for " + daredThing + "!" + RESET + "\n";
            }
            return start + "!" + RESET + "\n";
        }

        private String getRPSOutro()
        {
            String start =
                RED + callerNick + " used " + callerAnswer + ", " + daredNick + " used " + daredAnswer + ", ";
            if ( null != getWinner() )
            {
                return start + getWinner() + " wins" + ( ( null != daredThing ) ? " " + daredThing : "" ) + "!";
            }
            return start + "no one wins!";
        }

        private String alreadyPlaying()
        {
            return "A current game is already in progress.\n" + getRPSIntro();
        }

        private boolean setSubmission( String contestant, String answer )
            throws Exception
        {
            if ( contestant.equals( callerNick ) )
            {
                if ( null != callerAnswer )
                {
                    throw new Exception( "You have already submitted your answer!\n" );
                }
                callerAnswer = answer;
                return null != daredAnswer;
            }
            else if ( contestant.equals( daredNick ) )
            {
                if ( null != daredAnswer )
                {
                    throw new Exception( "You have already submitted your answer!\n" );
                }
                daredAnswer = answer;
                return null != callerAnswer;
            }
            throw new Exception( "You are not playing in this RPS game!\n" );
        }

        private String getWinner()
        {
            if ( callerAnswer.equals( daredAnswer ) )
            {
                return null;
            }

            if ( firstBeatsSecond( callerAnswer, daredAnswer ) )
            {
                return callerNick;
            }
            return daredNick;
        }

        private String getLoser()
        {
            String winner = getWinner();
            if ( null == winner )
            {
                return null;
            }
            if ( callerNick.equals( winner ) )
            {
                return daredNick;
            }
            return callerNick;
        }

        private static boolean firstBeatsSecond( String first, String second )
        {
            if ( first.equals( "rock" ) )
            {
                return "scissors".equals( second );
            }
            else if ( first.equals( "paper" ) )
            {
                return "rock".equals( second );
            }
            else
            {
                return "paper".equals( second );
            }
        }
    }
}
