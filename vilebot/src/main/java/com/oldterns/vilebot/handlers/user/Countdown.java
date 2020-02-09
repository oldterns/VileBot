package com.oldterns.vilebot.handlers.user;

import bsh.EvalError;
import bsh.Interpreter;
import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;
import org.apache.commons.lang3.ArrayUtils;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ipun on 29/06/17. Countdown implementation based off of Trivia.java
 */
public class Countdown
    extends ListenerAdapter
{
    private static final String COUNTDOWN_CHANNEL = Vilebot.getConfig().get( "countdownChannel" );

    private static final long TIMEOUT = 45000L;

    private static final int ANSWER_THRESHOLD = 200;

    private static final int INVALID_STAKE = 10;

    private static final String RED = "\u000304";

    private static final String RESET = "\u000f";

    private static final String BLUE = "\u000302";

    private static final String GREEN = "\u000303";

    private static final Pattern countdownPattern = Pattern.compile( "^!countdown" );

    private static final Pattern answerPattern = Pattern.compile( "^!solution (.*)" );

    private static final Pattern rulesPattern = Pattern.compile( "!countdownrules" );

    private static CountdownGame currGame = null;

    private static ExecutorService timer = Executors.newScheduledThreadPool( 1 );

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

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher countdownMatcher = countdownPattern.matcher( text );
        Matcher answerMatcher = answerPattern.matcher( text );
        Matcher rulesMatcher = rulesPattern.matcher( text );
        if ( countdownMatcher.matches() && correctChannel( event ) )
        {
            startCountdownGame( event );
        }
        else if ( answerMatcher.matches() && checkValidSubmission( event ) )
        {
            String answer = answerMatcher.group( 1 );
            checkAnswer( event, answer );
        }
        else if ( rulesMatcher.matches() )
        {
            for ( String line : getRules() )
            {
                event.respondPrivateMessage( line );
            }
        }
    }

    private boolean correctChannel( GenericMessageEvent event )
    {
        if ( event instanceof MessageEvent )
        {
            String currChannel = ( (MessageEvent) event ).getChannel().getName();
            if ( currChannel.equals( COUNTDOWN_CHANNEL ) )
            {
                return true;
            }
        }
        event.respondWith( "To play Countdown join : " + COUNTDOWN_CHANNEL );
        return false;
    }

    private boolean isPrivate( GenericMessageEvent event )
    {
        return event instanceof PrivateMessageEvent;
    }

    private boolean checkValidSubmission( GenericMessageEvent event )
    {
        if ( currGame == null )
        {
            event.respondWith( "No active game. Start a new one with !countdown." );
            return false;
        }
        else if ( ( isPrivate( event ) ) )
        {
            return true;
        }

        String currChannel = ( (MessageEvent) event ).getChannel().getName();
        if ( currChannel.equals( COUNTDOWN_CHANNEL ) )
        {
            event.respondWith( getSubmissionRuleString( event ) );
        }
        else
        {
            event.respondWith( "To play Countdown join : " + COUNTDOWN_CHANNEL );
        }
        return false;
    }

    private synchronized void startCountdownGame( GenericMessageEvent event )
    {
        if ( currGame == null )
        {
            currGame = new CountdownGame();
            for ( String line : currGame.getCountdownIntro() )
            {
                event.respondWith( line );
            }
            event.respondWith( getSubmissionRuleString( event ) );
            startTimer( event );
        }
        else
        {
            for ( String line : currGame.alreadyPlaying() )
            {
                event.respondWith( line );
            }
        }
    }

    private synchronized void checkAnswer( GenericMessageEvent event, String submission )
    {
        String contestant = BaseNick.toBaseNick( event.getUser().getNick() );
        try
        {
            int contestantAnswer = currGame.interpretedAnswer( submission );
            if ( !answerBreaksThreshold( currGame.getTargetNumber(), contestantAnswer ) )
            {
                if ( !currGame.submissions.containsKey( contestant ) )
                {
                    currGame.setSubmission( contestant, submission, contestantAnswer );
                    event.respondWith( String.format( "Your submission of %s has been received!", submission ) );
                }
                else
                {
                    event.respondWith( String.format( "Sorry %s, you've already submitted for this game.",
                                                      contestant ) );
                }
            }
            else
            {
                event.respondWith( String.format( "You have put an answer that breaks the threshold of +-%d, you lose %d karma.",
                                                  ANSWER_THRESHOLD, INVALID_STAKE ) );
                KarmaDB.modNounKarma( contestant, -1 * INVALID_STAKE );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            event.respondWith( String.format( "Sorry %s! You have put an invalid answer, you lose %d karma.",
                                              contestant, INVALID_STAKE ) );
            KarmaDB.modNounKarma( contestant, -1 * INVALID_STAKE );
        }

    }

    private void startTimer( final GenericMessageEvent event )
    {
        timer.submit( () -> {
            try
            {
                Thread.sleep( TIMEOUT );
                timeoutTimer( event );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        } );
    }

    private void timeoutTimer( GenericMessageEvent event )
    {
        Set<String> keys = currGame.submissions.keySet();
        event.respondWith( String.format( "Your time is up! The target number was %s.",
                                          RED + currGame.getTargetNumber() + RESET ) );
        if ( !keys.isEmpty() )
        {
            event.respondWith( "The final submissions are:" );
            for ( String key : keys )
            {
                event.respondWith( key + ", with " + currGame.getSubmissionAndAnswer( key ) );
            }
            Set<String> winners = currGame.winners.keySet();
            if ( !winners.isEmpty() )
            {
                int karmaAwarded;
                event.respondWith( "The winners are :" );
                for ( String winner : winners )
                {
                    karmaAwarded = currGame.karmaAwarded( winner );
                    event.respondWith( winner + ", awarded " + karmaAwarded + " karma." );
                    KarmaDB.modNounKarma( winner, karmaAwarded );
                }
            }
            else
            {
                event.respondWith( "There are no winners for this game. Better luck next time!" );
            }
        }
        else
        {
            event.respondWith( "There were no submissions for this game. Better luck next time!" );
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

    private String getSubmissionRuleString( GenericMessageEvent event )
    {
        return RED + "Use \" /msg " + RESET + event.getBot().getNick() + RED + " !solution < answer > \" to submit."
            + RESET;
    }
}
