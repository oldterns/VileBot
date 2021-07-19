package com.oldterns.vilebot.services;

import bsh.EvalError;
import bsh.Interpreter;
import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.Bot;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.annotations.OnPrivateMessage;
import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.TimeService;
import org.apache.commons.lang3.ArrayUtils;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ipun on 29/06/17. Countdown implementation based off of Trivia.java
 */
@ApplicationScoped
@Bot( "CountdownB0t1" )
public class CountdownService
{
    private static final String COUNTDOWN_CHANNEL = "${vilebot.countdown.channel}";

    private static final Duration TIMEOUT = Duration.ofSeconds( 45 );

    private static final int ANSWER_THRESHOLD = 200;

    private static final int INVALID_STAKE = 10;

    private static final String RED = "\u000304";

    private static final String RESET = "\u000f";

    private static final String BLUE = "\u000302";

    private static final String GREEN = "\u000303";

    @Inject
    TimeService timeService;

    @Inject
    KarmaDB karmaDB;

    CountdownGame currGame = null;

    private static ExecutorService timer = Executors.newScheduledThreadPool( 1 );

    @OnChannelMessage( value = "!countdown", channel = COUNTDOWN_CHANNEL )
    public String startNewGame( ChannelMessageEvent event )
    {
        return startCountdownGame( event );
    }

    @OnPrivateMessage( "!solution @answer" )
    public String onSolution( User user, String answer )
    {
        return checkAnswer( user, answer );
    }

    @OnMessage( "!countdownrules" )
    public void getRules( User user )
    {
        for ( String line : getRules() )
        {
            user.sendMessage( line );
        }
    }

    private synchronized String startCountdownGame( ChannelMessageEvent channelMessageEvent )
    {
        if ( currGame == null )
        {
            currGame = new CountdownGame();
            StringBuilder out = new StringBuilder();
            for ( String line : currGame.getCountdownIntro() )
            {
                out.append( line + "\n" );
            }
            out.append( getSubmissionRuleString() );
            startTimer( channelMessageEvent.getChannel() );
            return out.toString();
        }
        else
        {
            return String.join( "\n", currGame.alreadyPlaying() );
        }
    }

    private synchronized String checkAnswer( User user, String submission )
    {
        String contestant = Nick.getNick( user ).getBaseNick();
        try
        {
            int contestantAnswer = currGame.interpretedAnswer( submission );
            if ( !answerBreaksThreshold( currGame.getTargetNumber(), contestantAnswer ) )
            {
                if ( !currGame.submissions.containsKey( contestant ) )
                {
                    currGame.setSubmission( contestant, submission, contestantAnswer );
                    return String.format( "Your submission of %s has been received!", submission );
                }
                else
                {
                    return String.format( "Sorry %s, you've already submitted for this game.", contestant );
                }
            }
            else
            {
                karmaDB.modNounKarma( contestant, -1 * INVALID_STAKE );
                return String.format( "You have put an answer that breaks the threshold of +-%d, you lose %d karma.",
                                      ANSWER_THRESHOLD, INVALID_STAKE );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            karmaDB.modNounKarma( contestant, -1 * INVALID_STAKE );
            return String.format( "Sorry %s! You have put an invalid answer, you lose %d karma.", contestant,
                                  INVALID_STAKE );

        }
    }

    private void startTimer( Channel channel )
    {
        timeService.onTimeout( TIMEOUT, () -> {
            timeoutTimer( channel );
        } );
    }

    private void timeoutTimer( Channel channel )
    {
        Set<String> keys = currGame.submissions.keySet();
        channel.sendMessage( String.format( "Your time is up! The target number was %s.",
                                            RED + currGame.getTargetNumber() + RESET ) );
        if ( !keys.isEmpty() )
        {
            channel.sendMessage( "The final submissions are:" );
            for ( String key : keys )
            {
                channel.sendMessage( key + ", with " + currGame.getSubmissionAndAnswer( key ) );
            }
            Set<String> winners = currGame.winners.keySet();
            if ( !winners.isEmpty() )
            {
                int karmaAwarded;
                channel.sendMessage( "The winners are :" );
                for ( String winner : winners )
                {
                    karmaAwarded = currGame.karmaAwarded( winner );
                    channel.sendMessage( winner + ", awarded " + karmaAwarded + " karma." );
                    karmaDB.modNounKarma( winner, karmaAwarded );
                }
            }
            else
            {
                channel.sendMessage( "There are no winners for this game. Better luck next time!" );
            }
        }
        else
        {
            channel.sendMessage( "There were no submissions for this game. Better luck next time!" );
        }
        currGame = null;
        stopTimer();
    }

    private void stopTimer()
    {
        timer.shutdownNow();
        timer = Executors.newFixedThreadPool( 1 );
    }

    private boolean answerBreaksThreshold( int targetNumber, int contestantAnswer )
    {
        return ( Math.abs( targetNumber - contestantAnswer ) >= ANSWER_THRESHOLD );
    }

    private String[] getRules()
    {
        return new String[] { " " + RED + "COUNTDOWN RULES:" + RESET,
            "1) Get as close as you can to the target number using only the numbers given.",
            RED + "TIP: You do not have to use all the numbers." + RESET,
            "2) Answer with !solution <your answer> . Make sure to only use valid characters, such as numbers and + - * / ( ) . ",
            "Breaking Rule 2 will subject you to a loss of " + INVALID_STAKE + " karma.",
            "3) The closer you are to the target number, the more karma you will get (max. 10).",
            RED + "TIP: If you are over/under " + ANSWER_THRESHOLD + " you will be penalized " + INVALID_STAKE
                + " karma." + RESET,
            "4) Use \" /msg CountdownB0t !solution <your answer> \" for your answers." };
    }

    private String getSubmissionRuleString()
    {
        return RED + "Use \" /msg " + RESET + "CountdownB0t1" + RED + " !solution < answer > \" to submit." + RESET;
    }

    private static class CountdownGame
    {
        private final int VALID_NUMBER_COUNT = 6;

        private final List<Integer> LARGE_NUMBERS = new ArrayList<>( Arrays.asList( 25, 50, 75, 100 ) );

        private final List<Integer> SMALL_NUMBERS =
            new ArrayList<>( Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ) );

        private List<Integer> questionNumbers = new ArrayList<>();

        private Hashtable<String, String> submissions = new Hashtable<>();

        private Hashtable<String, Integer> answers = new Hashtable<>();

        private Hashtable<String, Integer> winners = new Hashtable<>();

        private int largeNumberCount;

        private int smallNumberCount;

        private int targetNumber;

        Interpreter interpreter;

        Random rand = new Random();

        CountdownGame()
        {
            shuffleNumbers();
            largeNumberCount = rand.nextInt( LARGE_NUMBERS.size() + 1 );
            smallNumberCount = VALID_NUMBER_COUNT - largeNumberCount;
            questionNumbers.addAll( LARGE_NUMBERS.subList( 0, largeNumberCount ) );
            questionNumbers.addAll( SMALL_NUMBERS.subList( 0, smallNumberCount ) );
            targetNumber = generateTargetNumber();
        }

        // target number should be between 100-999.
        private int generateTargetNumber()
        {
            return rand.nextInt( 900 ) + 100;
        }

        private String getSubmissionAndAnswer( String contestant )
        {
            return String.format( "%s = %d", getSubmission( contestant ), getAnswer( contestant ) );
        }

        private int getAnswer( String contestant )
        {
            return answers.get( contestant );
        }

        private String getSubmission( String contestant )
        {
            return submissions.get( contestant );
        }

        private void setSubmission( String contestant, String submission, int interpretedAnswer )
        {
            submissions.put( contestant, submission );
            answers.put( contestant, interpretedAnswer );
            int karma = karmaAwarded( contestant );
            if ( karma > 0 )
            {
                winners.put( contestant, karma );
            }
        }

        private int karmaAwarded( String contestant )
        {
            if ( answers.containsKey( contestant ) )
            {
                return 10 - ( Math.abs( targetNumber - answers.get( contestant ) ) );
            }
            else
            {
                return 0;
            }
        }

        private int getLargeNumberCount()
        {
            return largeNumberCount;
        }

        private int getSmallNumberCount()
        {
            return smallNumberCount;
        }

        private int getTargetNumber()
        {
            return targetNumber;
        }

        private List<Integer> getQuestionNumbers()
        {
            return questionNumbers;
        }

        private List<String> getCountdownIntro()
        {
            List<String> countdownIntro = new ArrayList<>();
            countdownIntro.add( GREEN + "Welcome to Countdown!" + RESET );
            countdownIntro.add( "See the game rules with !countdownrules." );
            countdownIntro.addAll( Arrays.asList( getQuestion() ) );
            countdownIntro.add( "Good luck! You have 45 seconds." );
            return countdownIntro;
        }

        private String[] getQuestion()
        {
            return new String[] { "Your numbers are:", RED + getQuestionNumbers().toString() + RESET, "Your target is:",
                RED + getTargetNumber() + RESET };
        }

        private void shuffleNumbers()
        {
            Collections.shuffle( LARGE_NUMBERS );
            Collections.shuffle( SMALL_NUMBERS );
        }

        private int interpretedAnswer( String answer )
            throws Exception
        {
            if ( noSpecialCharacters( answer ) )
            {
                if ( hasCorrectNumbers( answer ) )
                {
                    interpreter = new Interpreter();
                    try
                    {
                        interpreter.eval( "result = " + answer );
                        return ( (int) interpreter.get( "result" ) );
                    }
                    catch ( EvalError e )
                    {
                        e.printStackTrace();
                        throw e;
                    }
                }
                else
                {
                    throw new Exception( "wrong numbers are included in solution" );
                }
            }
            else
            {
                throw new Exception( "special characters are included in solution" );
            }
        }

        private boolean noSpecialCharacters( String answer )
        {
            return answer.matches( "^[-*+/()\\s\\d]+$" );
        }

        private boolean hasCorrectNumbers( String answer )
        {
            String[] numList = answer.replaceAll( "[^\\d]+", " " ).trim().split( " " );
            List<Integer> questionNums = new ArrayList<>( getQuestionNumbers() );
            for ( String num : numList )
            {
                int number = Integer.valueOf( num );
                if ( questionNums.contains( number ) )
                {
                    questionNums.remove( (Integer) number );
                }
                else
                {
                    return false;
                }
            }
            return true;
        }

        private String[] alreadyPlaying()
        {
            return ArrayUtils.addAll( new String[] { "A current game is already in progress." }, getQuestion() );
        }
    }
}
