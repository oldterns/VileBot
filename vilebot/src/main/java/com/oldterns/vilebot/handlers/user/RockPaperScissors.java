package com.oldterns.vilebot.handlers.user;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.util.BaseNick;
import net.engio.mbassy.listener.Handler;

/**
 * Created by Christopher Chianelli on 27/08/17. Thanks to Josh Matsuoka for helping squashing bugs RockPaperScissors
 * implementation based off of Trivia.java
 */
@HandlerContainer
public class RockPaperScissors
{

    private static final String RPS_CHANNEL = Vilebot.getConfig().get( "rpsChannel" );

    private static final int DEFAULT_STAKE = 10;

    public static final String RED = "\u000304";

    public static final String RESET = "\u000f";

    public static final String BLUE = "\u000302";

    public static final String GREEN = "\u000303";

    private static final Pattern rpsPattern = Pattern.compile( "^!rps (\\S*)(.*)" );

    private static final Pattern answerPattern = Pattern.compile( "^!(rock|paper|scissors)" );

    private static final Pattern cancelPattern = Pattern.compile( "^!rpscancel" );

    private static final Pattern rulesPattern = Pattern.compile( "^!rpsrules" );

    private static RPSGame currGame = null;

    @AssignedBot
    private KeratinBot bot;

    private static class RPSGame
    {
        private static final String[] answers = { "rock", "paper", "scissors" };

        private String callerNick;

        private String daredNick;

        private String callerAnswer;

        private String daredAnswer;

        private String daredThing;

        private ReceivePrivmsg event;

        Random rand = new Random();

        public RPSGame( String callerNick, String daredNick, String daredThing, ReceivePrivmsg event, KeratinBot bot )
        {
            this.callerNick = callerNick;
            this.daredNick = daredNick;
            this.callerAnswer = null;
            this.daredAnswer = null;
            this.daredThing = daredThing;
            this.event = event;
            rand = new Random();
            if ( daredNick.equals( bot.getNick() ) )
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

        private ReceivePrivmsg getEvent()
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

    @Handler
    public void rockPaperScissors( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher rpsMatcher = rpsPattern.matcher( text );
        Matcher answerMatcher = answerPattern.matcher( text );
        Matcher rulesMatcher = rulesPattern.matcher( text );
        Matcher cancelMatcher = cancelPattern.matcher( text );
        if ( rpsMatcher.matches() && correctChannel( event ) )
        {
            try
            {
                startRPSGame( event, rpsMatcher.group( 1 ), rpsMatcher.group( 2 ) );
            }
            catch ( Exception e )
            {
                event.reply( e.getMessage() );
            }
        }
        else if ( answerMatcher.matches() && correctSolutionChannel( event ) )
        {
            String answer = answerMatcher.group( 1 );
            checkAnswer( event, answer );
        }
        else if ( rulesMatcher.matches() )
        {
            event.replyPrivately( getRules() );
        }
        else if ( cancelMatcher.matches() )
        {
            cancelGame( event );
        }
    }

    private boolean correctChannel( ReceivePrivmsg event )
    {
        String currChannel = event.getChannel();
        if ( RPS_CHANNEL.equals( currChannel ) )
        {
            return true;
        }
        else
        {
            event.reply( "To play Rock Paper Scissors join : " + RPS_CHANNEL );
            return false;
        }
    }

    private boolean isPrivate( ReceivePrivmsg event )
    {
        try
        {
            bot.getChannel( event.getChannel() ).getNicks();
        }
        catch ( Exception e )
        {
            return true;
        }
        return false;
    }

    private boolean inGameChannel( ReceivePrivmsg event )
    {
        String currChannel = event.getChannel();
        if ( RPS_CHANNEL.equals( currChannel ) )
        {
            try
            {
                return bot.getChannel( event.getChannel() ).getNicks().contains( event.getSender() );
            }
            catch ( Exception e )
            {
                return false;
            }
        }
        return true;
    }

    private boolean correctSolutionChannel( ReceivePrivmsg event )
    {
        String currChannel = event.getChannel();
        if ( ( isPrivate( event ) && inGameChannel( event ) ) )
        {
            return true;
        }
        else if ( currChannel.equals( RPS_CHANNEL ) )
        {
            event.reply( getSubmissionRuleString() );
            return false;
        }
        else
        {
            event.reply( "To play Rock Paper Scissors join : " + RPS_CHANNEL );
            return false;
        }
    }

    private synchronized void cancelGame( ReceivePrivmsg event )
    {
        if ( null != currGame )
        {
            String caller = BaseNick.toBaseNick( event.getSender() );
            if ( caller.equals( currGame.getCaller() ) || caller.equals( currGame.getDared() ) )
            {
                currGame = null;
                event.reply( "RPS game cancelled" );
            }
            else
            {
                event.reply( "Only " + currGame.getCaller() + " or " + currGame.getDared() + " can cancel this game." );
            }
        }
        else
        {
            event.reply( "No active game. Start a new one with !rps dared" );
        }
    }

    private synchronized void startRPSGame( ReceivePrivmsg event, String dared, String daredThing )
        throws Exception
    {
        if ( null == currGame )
        {
            String caller = BaseNick.toBaseNick( event.getSender() );
            if ( caller.equals( dared ) )
            {
                event.reply( "You cannot challenge yourself to a Rock Paper Scissors game!" );
                return;
            }
            if ( !daredThing.trim().isEmpty() )
            {
                currGame = new RPSGame( caller, dared, daredThing.trim(), event, bot );
            }
            else
            {
                currGame = new RPSGame( caller, dared, null, event, bot );
            }
            event.reply( currGame.getRPSIntro() + getSubmissionRuleString() );
        }
        else
        {
            event.reply( currGame.alreadyPlaying() );
        }
    }

    private synchronized void checkAnswer( ReceivePrivmsg event, String submission )
    {
        String contestant = BaseNick.toBaseNick( event.getSender() );
        if ( currGame != null )
        {
            try
            {
                boolean endGame = currGame.setSubmission( contestant, submission );
                event.reply( String.format( "Your submission of %s has been recieved!", submission ) );
                if ( endGame )
                {
                    currGame.getEvent().reply( currGame.getRPSOutro() );
                    currGame = null;
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                event.reply( e.getMessage() );
            }
        }
        else
        {
            event.reply( "No active game. Start a new one with !rps dared [bet]" );
        }

    }

    private String getRules()
    {
        return RED + "RPS RULES: \n" + RESET + "1) Dare someone to play rock paper scissors with you! \n" + RESET
            + "2) Rock beats scissors, paper beats rocks, and scissors beat paper \n" + "3) Use /msg " + bot.getNick()
            + " !(rock|paper|scissors) to set your action. Cannot be undone.";
    }

    private String getSubmissionRuleString()
    {
        return RED + "Use \" /msg " + RESET + bot.getNick() + RED + " !(rock|paper|scissors) \" to submit." + RESET;
    }
}
