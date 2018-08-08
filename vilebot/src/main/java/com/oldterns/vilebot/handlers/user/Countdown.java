package com.oldterns.vilebot.handlers.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bsh.EvalError;
import bsh.Interpreter;
import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmalyticsDB;
import com.oldterns.vilebot.karmalytics.HasKarmalytics;
import com.oldterns.vilebot.karmalytics.KarmalyticsRecord;
import com.oldterns.vilebot.util.BaseNick;
import net.engio.mbassy.listener.Handler;

/**
 * Created by ipun on 29/06/17. Countdown implementation based off of Trivia.java
 */
@HandlerContainer
public class Countdown
    implements HasKarmalytics
{

    private static final String COUNTDOWN_CHANNEL = Vilebot.getConfig().get( "countdownChannel" );

    private static final long TIMEOUT = 45000L;

    private static final int ANSWER_THRESHOLD = 200;

    private static final int INVALID_STAKE = 10;

    public static final String RED = "\u000304";

    public static final String RESET = "\u000f";

    public static final String BLUE = "\u000302";

    public static final String GREEN = "\u000303";

    private static final Pattern countdownPattern = Pattern.compile( "^!countdown" );

    private static final Pattern answerPattern = Pattern.compile( "^!solution (.*)" );

    private static final Pattern rulesPattern = Pattern.compile( "!countdownrules" );

    private static CountdownGame currGame = null;

    private static ExecutorService timer = Executors.newScheduledThreadPool( 1 );

    @AssignedBot
    private KeratinBot bot;

    public Countdown()
    {
        KarmalyticsDB.intializeKarmalyticsFor( this );
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

        public CountdownGame()
        {
            shuffleNumbers();
            largeNumberCount = rand.nextInt( LARGE_NUMBERS.size() + 1 );
            smallNumberCount = VALID_NUMBER_COUNT - largeNumberCount;
            questionNumbers.addAll( LARGE_NUMBERS.subList( 0, largeNumberCount ) );
            questionNumbers.addAll( SMALL_NUMBERS.subList( 0, smallNumberCount ) );
            targetNumber = gnereateTargetNumber();
        }

        // target number should be between 100-999.
        private int gnereateTargetNumber()
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

        private String getCountdownIntro()
        {
            return GREEN + "Welcome to Countdown!\n" + RESET + getQuestion() + "\nGood luck! You have 45 seconds.\n";
        }

        private String getQuestion()
        {
            return "Your numbers are: \n" + RED + getQuestionNumbers() + RESET + "\nYour target is: \n" + RED
                + getTargetNumber() + RESET;
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
            List<Integer> questionNums = new ArrayList<Integer>( getQuestionNumbers() );
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

        private String alreadyPlaying()
        {
            return "A current game is already in progress.\n" + getQuestion();
        }
    }

    @Handler
    public void countdown( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher countdownMatcher = countdownPattern.matcher( text );
        Matcher answerMatcher = answerPattern.matcher( text );
        Matcher rulesMatcher = rulesPattern.matcher( text );
        if ( countdownMatcher.matches() && correctChannel( event ) )
        {
            startCountdownGame( event );
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
    }

    private boolean correctChannel( ReceivePrivmsg event )
    {
        String currChannel = event.getChannel();
        if ( COUNTDOWN_CHANNEL.equals( currChannel ) )
        {
            return true;
        }
        else
        {
            event.reply( "To play Countdown join : " + COUNTDOWN_CHANNEL );
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
        if ( COUNTDOWN_CHANNEL.equals( currChannel ) )
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
        else if ( currChannel.equals( COUNTDOWN_CHANNEL ) )
        {
            event.reply( getSubmissionRuleString() );
            return false;
        }
        else
        {
            event.reply( "To play Countdown join : " + COUNTDOWN_CHANNEL );
            return false;
        }
    }

    private synchronized void startCountdownGame( ReceivePrivmsg event )
    {
        if ( currGame == null )
        {
            currGame = new CountdownGame();
            event.reply( currGame.getCountdownIntro() + getSubmissionRuleString() );
            startTimer( event );
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
                int contestantAnswer = currGame.interpretedAnswer( submission );
                if ( !answerBreaksThreshold( currGame.getTargetNumber(), contestantAnswer ) )
                {
                    if ( !currGame.submissions.containsKey( contestant ) )
                    {
                        currGame.setSubmission( contestant, submission, contestantAnswer );
                        if ( isPrivate( event ) )
                        {
                            event.reply( String.format( "Your submission of %s has been recieved!", submission ) );
                        }
                        else
                        {
                            event.reply( String.format( "%s's submission recieved!", contestant ) );
                        }
                    }
                    else
                    {
                        event.reply( String.format( "Sorry %s, you've already submitted for this game.", contestant ) );
                    }
                }
                else
                {
                    event.reply( String.format( "You have put an answer that breaks the threshold of +-%d, you lose %d karma.",
                                                ANSWER_THRESHOLD, INVALID_STAKE ) );
                    modNounKarma( contestant, -1 * INVALID_STAKE );
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                event.reply( String.format( "Sorry %s! You have put an invalid answer, you lose %d karma.", contestant,
                                            INVALID_STAKE ) );
                modNounKarma( contestant, -1 * INVALID_STAKE );
            }
        }
        else
        {
            event.reply( "No active game. Start a new one with !countdown" );
        }

    }

    private void startTimer( final ReceivePrivmsg event )
    {
        timer.submit( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep( TIMEOUT );
                    timeoutTimer( event );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            }
        } );
    }

    private void timeoutTimer( ReceivePrivmsg event )
    {
        stopTimer();
        Set<String> keys = currGame.submissions.keySet();
        event.reply( String.format( "Your time is up! The target number was %s \n",
                                    RED + currGame.getTargetNumber() + RESET ) );
        if ( !keys.isEmpty() )
        {
            event.reply( "The final submissions are: \n" );
            for ( String key : keys )
            {
                event.reply( key + ", with " + currGame.getSubmissionAndAnswer( key ) + "\n" );
            }
            Set<String> winners = currGame.winners.keySet();
            if ( !winners.isEmpty() )
            {
                int karmaAwarded;
                event.reply( "The winners are : \n" );
                for ( String winner : winners )
                {
                    karmaAwarded = currGame.karmaAwarded( winner );
                    event.reply( winner + " awarded " + GREEN + karmaAwarded + RESET + " karma \n" );
                    modNounKarma( winner, karmaAwarded );
                }
            }
            else
            {
                event.reply( "There are no winners for this game. Better luck next time!" );
            }
        }
        else
        {
            event.reply( "There were no submissions for this game. Better luck next time!" );
        }
        currGame = null;
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

    private String getRules()
    {
        return RED + "COUNTDOWN RULES: \n" + RESET
            + "1) Get as close as you can to the target number using only the numbers given. \n" + RED
            + "TIP: You do not have to use all the numbers. \n" + RESET
            + "2) Answer with !solution <your answer> . Make sure to only use valid characters, "
            + "such as numbers and + - * / ( ) . \n" + "Breaking Rule 2 will subject you to a loss of " + INVALID_STAKE
            + " karma. \n" + "3) The closer you are to the target number, the more karma you will get (max. 10). \n"
            + RED + "TIP: If you are over/under " + ANSWER_THRESHOLD + "you will be penalized " + INVALID_STAKE
            + " karma. \n" + RESET + "4) Use /msg Countdownb0t !solution <your answer> for your answers. ";
    }

    private String getSubmissionRuleString()
    {
        return RED + "Use \" /msg " + RESET + bot.getNick() + RED + " !solution < answer > \" to submit." + RESET;
    }

    @Override
    public List<String> getGroups()
    {
        return Arrays.asList( "Gaming" );
    }

    @Override
    public String getKarmalyticsId()
    {
        return "Countdown";
    }

    @Override
    public Function<KarmalyticsRecord, String> getRecordDescriptorFunction()
    {
        return r -> ( r.getKarmaModAmount() > 0 )
                        ? r.getNick() + " has won a game of countdown and got " + r.getKarmaModAmount()
                            + " for winning."
                        : r.getNick() + " has lost a game of countdown and lose " + -r.getKarmaModAmount()
                            + " for losing.";
    }
}
