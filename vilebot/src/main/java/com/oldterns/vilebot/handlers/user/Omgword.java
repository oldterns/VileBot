package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Based off of the omgword game from CasinoBot: http://casinobot.codeplex.com/
 */
// @HandlerContainer
public class Omgword
    extends ListenerAdapter
{

    private static final Pattern QUESTION_PATTERN = Pattern.compile( "!omgword" );

    private static final Pattern ANSWER_PATTERN = Pattern.compile( "!answer (.*)" );

    private static final String WORD_LIST_PATH = Vilebot.getConfig().get( "OmgwordList" );

    private static final String OMGWORD_CHANNEL = Vilebot.getConfig().get( "OmgwordChannel" );

    private ArrayList<String> words = loadWords();

    private OmgwordGame currentGame;

    private String word;

    private String scrambled;

    private static final Random random = new SecureRandom();

    private static final long TIMEOUT = 30000L;

    private static ExecutorService timer = Executors.newScheduledThreadPool( 1 );

    // @Handler
    @Override
    public void onMessage( MessageEvent event )
    {
        String text = event.getMessage();
        Matcher questionMatcher = QUESTION_PATTERN.matcher( text );
        Matcher answerMatcher = ANSWER_PATTERN.matcher( text );
        // try
        // {
        if ( questionMatcher.matches() && shouldStartGame( event ) )
        {
            startGame( event );
        }
        else if ( answerMatcher.matches() && shouldStartGame( event ) )
        {
            String answer = answerMatcher.group( 1 );
            finishGame( event, answer );
        }
        // }
        // catch ( Exception e )
        // {
        // e.printStackTrace();
        // System.exit( 1 );
        // }
    }

    private boolean shouldStartGame( MessageEvent event )
    {
        String actualChannel = event.getChannel().getName();

        if ( OMGWORD_CHANNEL.equalsIgnoreCase( actualChannel ) )
        {
            return true;
        }
        event.respondWith( "To play Omgword join: " + OMGWORD_CHANNEL );
        return false;
    }

    private synchronized void startGame( MessageEvent event )
    // throws Exception
    {
        if ( currentGame != null )
        {
            event.respondWith( currentGame.getAlreadyPlayingString() );
        }
        else
        {
            currentGame = new OmgwordGame();
            event.respondWith( currentGame.getIntroString() );
            startTimer( event );
        }
    }

    private void startTimer( final MessageEvent event )
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

    private void timeoutTimer( MessageEvent event )
    {
        String message = currentGame.getTimeoutString();
        event.respondWith( message );
        currentGame = null;
    }

    private void stopTimer()
    {
        timer.shutdownNow();
        timer = Executors.newFixedThreadPool( 1 );
    }

    private synchronized void finishGame( MessageEvent event, String answer )
    {
        String answerer = BaseNick.toBaseNick( Objects.requireNonNull( event.getUser() ).getNick() );
        if ( currentGame != null )
        {
            if ( currentGame.isCorrect( answer ) )
            {
                stopTimer();
                event.respondWith( String.format( "Congrats %s, you win %d karma!", answerer,
                                                  currentGame.getStakes() ) );
                KarmaDB.modNounKarma( answerer, currentGame.getStakes() );
                currentGame = null;
            }
            else
            {
                event.respondWith( String.format( "Sorry %s! That is incorrect, you lose %d karma.", answerer,
                                                  currentGame.getStakes() ) );
                KarmaDB.modNounKarma( answerer, -1 * currentGame.getStakes() );
            }
        }
        else
        {
            event.respondWith( "No active game. Start a new one with !omgword" );
        }
    }

    private ArrayList<String> loadWords()
    {
        try
        {
            ArrayList<String> words = new ArrayList<>();
            List<String> lines = Files.readAllLines( Paths.get( WORD_LIST_PATH ), Charset.forName( "UTF-8" ) );
            for ( String line : lines )
            {
                words.addAll( Arrays.asList( line.split( " " ) ) );
            }
            return words;
        }
        catch ( Exception e )
        {
            System.exit( 1 );
        }
        return null;
    }

    private class OmgwordGame
    {

        String getAlreadyPlayingString()
        {
            return "A game is already in progress! Your word is: " + scrambled;
        }

        String getIntroString()
        {
            word = "";
            int index = random.nextInt( words.size() - 1 );
            word = words.get( index );
            scrambled = word;
            char[] chars = words.get( index ).toCharArray();
            while ( scrambled.equals( word ) )
            {
                shuffleArray( chars );
                scrambled = new String( chars );
            }
            return "Welcome to omgword!\nFor " + getStakes() + " karma:\n" + scrambled + "\n30 seconds on the clock.";
        }

        boolean isCorrect( String answer )
        {
            return answer.equals( word );
        }

        String getTimeoutString()
        {
            return "Game over! The correct answer was: " + word;
        }

        int getStakes()
        {
            return (int) Math.ceil( word.length() >> 1 );
        }
    }

    private static void shuffleArray( char[] array )
    {
        char temp;
        int index;
        Random random = new Random();
        for ( int i = array.length - 1; i > 0; i-- )
        {
            index = random.nextInt( i + 1 );
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

}
