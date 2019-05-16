package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.jsoup.Jsoup;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import twitter4j.JSONArray;
import twitter4j.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eunderhi on 25/07/16. Simple Jeopardy implementation.
 */

public class Trivia
    extends ListenerAdapter
{

    private static final Pattern questionPattern = Pattern.compile( "^!jeopardy" );

    private static final Pattern answerPattern = Pattern.compile( "^!(whatis|whois) (.*)" );

    private static TriviaGame currentGame = null;

    private static final String JEOPARDY_CHANNEL = Vilebot.getConfig().get( "jeopardyChannel" );

    private static final long TIMEOUT = 30000L;

    private static final String RED = "\u000304";

    private static final String RESET = "\u000f";

    private static final String BLUE = "\u000302";

    private static final String GREEN = "\u000303";

    private static ExecutorService timer = Executors.newScheduledThreadPool( 1 );

    private static final Integer RECURSION_LIMIT = 10;

    private static Integer CURRENT_RECURSION_DEPTH = 0;

    private static final String WELCOME_STRING = "Welcome to Bot Jeopardy!";

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher questionMatcher = questionPattern.matcher( text );
        Matcher answerMatcher = answerPattern.matcher( text );

        try
        {
            if ( questionMatcher.matches() && shouldStartGame( event ) )
            {
                startGame( event );
            }
            else if ( answerMatcher.matches() && shouldStartGame( event ) )
            {
                String answer = answerMatcher.group( 2 );
                finishGame( event, answer );
            }
        }
        catch ( Exception e )
        {
            event.respondWith( "I don't feel like playing." );
            e.printStackTrace();
        }
    }

    private boolean shouldStartGame( GenericMessageEvent event )
    {
        if ( event instanceof MessageEvent
            && ( (MessageEvent) event ).getChannel().getName().equals( JEOPARDY_CHANNEL ) )
        {
            return true;
        }
        event.respondWith( "To play jeopardy join: " + JEOPARDY_CHANNEL );
        return false;
    }

    private synchronized void startGame( GenericMessageEvent event )
        throws Exception
    {
        if ( currentGame != null )
        {
            event.respondWith( currentGame.getAlreadyPlayingString() );
        }
        else
        {
            currentGame = new TriviaGame();
            event.respondWith( WELCOME_STRING );
            event.respondWith( currentGame.getIntroString() );
            startTimer( event );
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
        String message = currentGame.getTimeoutString();
        event.respondWith( message );
        currentGame = null;
    }

    private void stopTimer()
    {
        timer.shutdownNow();
        timer = Executors.newFixedThreadPool( 1 );
    }

    private synchronized void finishGame( GenericMessageEvent event, String answer )
    {
        String answerer = BaseNick.toBaseNick( event.getUser().getNick() );
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
            event.respondWith( "No active game. Start a new one with !jeopardy" );
        }
    }

    private static class TriviaGame
    {

        private int stakes;

        private String question;

        private String answer;

        private String category;

        private static final String API_URL = "http://jservice.io/api/random";

        TriviaGame()
            throws Exception
        {
            JSONObject triviaJSON = getQuestionJSON();
            if ( triviaJSON != null )
            {
                question = triviaJSON.getString( "question" );
                category = triviaJSON.getJSONObject( "category" ).getString( "title" );
                answer = Jsoup.parse( triviaJSON.getString( "answer" ) ).text();
                stakes = getStakes( triviaJSON );
            }
            else
            {
                question = "Could not find a question";
                category = "Could not find a question";
                answer = "Could not find a question";
                stakes = 0;
            }
        }

        private int getStakes( JSONObject trivia )
            throws Exception
        {
            if ( trivia.has( "value" ) && !trivia.isNull( "value" ) )
            {
                return trivia.getInt( "value" ) / 100;
            }
            return 5;
        }

        String getQuestion()
        {
            return question;
        }

        String getAnswer()
        {
            return answer;
        }

        int getStakes()
        {
            return stakes;
        }

        private boolean isCorrect( String answer )
        {
            String formattedUserAnswer = formatAnswer( answer );
            String formattedActualAnswer = formatAnswer( this.answer );
            double distance = new NormalizedLevenshtein().distance( formattedActualAnswer, formattedUserAnswer );
            return distance < 0.5;
        }

        private String formatAnswer( String answer )
        {
            return answer.toLowerCase().replaceAll( "^the ",
                                                    "" ).replaceAll( "^a ",
                                                                     "" ).replaceAll( "^an ",
                                                                                      "" ).replaceAll( "\\(.*\\)",
                                                                                                       "" ).replaceAll( "/.*",
                                                                                                                        "" ).replaceAll( "&",
                                                                                                                                         "and" ).replaceAll( "[^A-Za-z\\d]",
                                                                                                                                                             "" );
        }

        private String getQuestionBlurb()
        {
            return String.format( "Your category is: %s\nFor %s karma:\n%s", RED + category + RESET,
                                  GREEN + stakes + RESET, BLUE + question + RESET );
        }

        String getIntroString()
        {
            return getQuestionBlurb() + "\n30 seconds on the clock.";
        }

        String getAlreadyPlayingString()
        {
            return "A game is already in session!\n" + getQuestionBlurb();
        }

        String getTimeoutString()
        {
            return String.format( "Your 30 seconds is up! The answer we were looking for was:\n%s",
                                  BLUE + answer + RESET );
        }

        private String getQuestionContent()
            throws Exception
        {
            String content;
            URLConnection connection;
            connection = new URL( API_URL ).openConnection();
            connection.addRequestProperty( "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)" );
            Scanner scanner = new Scanner( connection.getInputStream() );
            scanner.useDelimiter( "\\Z" );
            content = scanner.next();
            return content;
        }

        private JSONObject getQuestionJSON()
            throws Exception
        {
            String triviaContent = getQuestionContent();
            JSONObject triviaJSON = new JSONArray( triviaContent ).getJSONObject( 0 );
            String question = triviaJSON.getString( "question" ).trim();
            boolean invalidFlag = !( triviaJSON.getString( "invalid_count" ).equals( "null" ) );
            if ( ( question.equals( "" ) || question.contains( "seen here" ) || invalidFlag )
                && ( CURRENT_RECURSION_DEPTH < RECURSION_LIMIT ) )
            {
                CURRENT_RECURSION_DEPTH += 1;
                return getQuestionJSON();
            }
            else if ( CURRENT_RECURSION_DEPTH >= RECURSION_LIMIT )
            {
                return null;
            }
            return triviaJSON;
        }
    }

}
