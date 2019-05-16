package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.util.BaseNick;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Christopher Chianelli on 27/08/17. Thanks to Josh Matsuoka for helping squashing bugs RockPaperScissors
 * implementation based off of Trivia.java
 */
public class RockPaperScissors
    extends ListenerAdapter
{

    private static final String RPS_CHANNEL = Vilebot.getConfig().get( "rpsChannel" );

    private static final int DEFAULT_STAKE = 10;

    private static final String RED = "\u000304";

    private static final String RESET = "\u000f";

    private static final String BLUE = "\u000302";

    private static final String GREEN = "\u000303";

    private static final Pattern rpsPattern = Pattern.compile( "^!rps (\\S*)(.*)" );

    private static final Pattern answerPattern = Pattern.compile( "^!(rock|paper|scissors)" );

    private static final Pattern cancelPattern = Pattern.compile( "^!rpscancel" );

    private static final Pattern rulesPattern = Pattern.compile( "^!rpsrules" );

    private static RPSGame currGame = null;

    private static class RPSGame
    {
        private static final String[] answers = { "rock", "paper", "scissors" };

        private String callerNick;

        private String daredNick;

        private String callerAnswer;

        private String daredAnswer;

        private String daredThing;

        private GenericMessageEvent event;

        Random rand = new Random();

        RPSGame( String callerNick, String daredNick, String daredThing, GenericMessageEvent event )
        {
            this.callerNick = callerNick;
            this.daredNick = daredNick;
            this.callerAnswer = null;
            this.daredAnswer = null;
            this.daredThing = daredThing;
            this.event = event;
            rand = new Random();
            if ( daredNick.equals( event.getBot().getNick() ) )
            {
                this.daredAnswer = answers[rand.nextInt( answers.length )];
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

        private GenericMessageEvent getEvent()
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

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher rpsMatcher = rpsPattern.matcher( text );
        Matcher answerMatcher = answerPattern.matcher( text );
        Matcher rulesMatcher = rulesPattern.matcher( text );
        Matcher cancelMatcher = cancelPattern.matcher( text );
        if ( event instanceof MessageEvent && rpsMatcher.matches() && correctChannel( (MessageEvent) event ) )
        {
            try
            {
                startRPSGame( event, rpsMatcher.group( 1 ), rpsMatcher.group( 2 ) );
            }
            catch ( Exception e )
            {
                event.respondWith( e.getMessage() );
            }
        }
        else if ( answerMatcher.matches() && correctSolutionChannel( event ) )
        {
            String answer = answerMatcher.group( 1 );
            checkAnswer( event, answer );
        }
        else if ( rulesMatcher.matches() )
        {
            event.respondPrivateMessage( getRules( event ) );
        }
        else if ( cancelMatcher.matches() )
        {
            cancelGame( event );
        }
    }

    private boolean correctChannel( MessageEvent event )
    {
        String currChannel = event.getChannel().getName();
        if ( RPS_CHANNEL.equals( currChannel ) )
        {
            return true;
        }
        else
        {
            event.respondWith( "To play Rock Paper Scissors join : " + RPS_CHANNEL );
            return false;
        }
    }

    private boolean isPrivate( GenericMessageEvent event )
    {
        return event instanceof PrivateMessageEvent;
    }

    private boolean inGameChannel( GenericMessageEvent event )
    {
        return event instanceof MessageEvent && ( (MessageEvent) event ).getChannel().getName().equals( RPS_CHANNEL );
    }

    private boolean correctSolutionChannel( GenericMessageEvent event )
    {
        if ( ( isPrivate( event ) && inGameChannel( event ) ) )
        {
            return true;
        }
        else if ( ( (MessageEvent) event ).getChannel().getName().equals( RPS_CHANNEL ) )
        {
            event.respondWith( getSubmissionRuleString( event ) );
            return false;
        }
        else
        {
            event.respondWith( "To play Rock Paper Scissors join : " + RPS_CHANNEL );
            return false;
        }
    }

    private synchronized void cancelGame( GenericMessageEvent event )
    {
        if ( null != currGame )
        {
            String caller = BaseNick.toBaseNick( event.getUser().getNick() );
            if ( caller.equals( currGame.getCaller() ) || caller.equals( currGame.getDared() ) )
            {
                currGame = null;
                event.respondWith( "RPS game cancelled" );
            }
            else
            {
                event.respondWith( "Only " + currGame.getCaller() + " or " + currGame.getDared()
                    + " can cancel this game." );
            }
        }
        else
        {
            event.respondWith( "No active game. Start a new one with !rps dared" );
        }
    }

    private synchronized void startRPSGame( GenericMessageEvent event, String dared, String daredThing )
    {
        if ( null == currGame )
        {
            String caller = BaseNick.toBaseNick( event.getUser().getNick() );
            if ( caller.equals( dared ) )
            {
                event.respondWith( "You cannot challenge yourself to a Rock Paper Scissors game!" );
                return;
            }
            if ( !daredThing.trim().isEmpty() )
            {
                currGame = new RPSGame( caller, dared, daredThing.trim(), event );
            }
            else
            {
                currGame = new RPSGame( caller, dared, null, event );
            }
            event.respondWith( currGame.getRPSIntro() + getSubmissionRuleString( event ) );
        }
        else
        {
            event.respondWith( currGame.alreadyPlaying() );
        }
    }

    private synchronized void checkAnswer( GenericMessageEvent event, String submission )
    {
        String contestant = BaseNick.toBaseNick( event.getUser().getNick() );
        if ( currGame != null )
        {
            try
            {
                boolean endGame = currGame.setSubmission( contestant, submission );
                event.respondWith( String.format( "Your submission of %s has been recieved!", submission ) );
                if ( endGame )
                {
                    currGame.getEvent().respondWith( currGame.getRPSOutro() );
                    currGame = null;
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                event.respondWith( e.getMessage() );
            }
        }
        else
        {
            event.respondWith( "No active game. Start a new one with !rps dared [bet]" );
        }

    }

    private String getRules( GenericMessageEvent event )
    {
        return RED + "RPS RULES: \n" + RESET + "1) Dare someone to play rock paper scissors with you! \n" + RESET
            + "2) Rock beats scissors, paper beats rocks, and scissors beat paper \n" + "3) Use /msg "
            + event.getBot().getNick() + " !(rock|paper|scissors) to set your action. Cannot be undone.";
    }

    private String getSubmissionRuleString( GenericMessageEvent event )
    {
        return RED + "Use \" /msg " + RESET + event.getBot().getNick() + RED + " !(rock|paper|scissors) \" to submit."
            + RESET;
    }
}
