package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import net.engio.mbassy.listener.Handler;
import org.jsoup.Jsoup;
import twitter4j.JSONArray;
import twitter4j.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eunderhi on 25/07/16.
 * Simple Jeopardy implementation.
 */
@HandlerContainer
public class Trivia {

    private static final Pattern questionPattern = Pattern.compile( "^!jeopardy" );
    private static final Pattern answerPattern = Pattern.compile( "^!(whatis|whois) (.*)" );
    private static TriviaGame currentGame = null;
    private static final String JEOPARDY_CHANNEL = Vilebot.getConfig().get("jeopardyChannel");
    private static final int TIMEOUT  = 30000;
    public static final String RED = "\u000304";
    public static final String RESET = "\u000f";
    public static final String BLUE = "\u000302";
    public static final String GREEN = "\u000303";
    private static ExecutorService timer = Executors.newScheduledThreadPool(1);

    @Handler
    public void doTrivia(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher questionMatcher = questionPattern.matcher(text);
        Matcher answerMatcher = answerPattern.matcher(text);

        try {
            if (questionMatcher.matches() && shouldStartGame(event)) {
                startGame(event);
            } else if (answerMatcher.matches() && shouldStartGame(event)) {
                String answer = answerMatcher.group(2);
                finishGame(event, answer);
            }
        } catch(Exception e) {
            event.reply("I don't feel like playing.");
            e.printStackTrace();
        }
    }

    private boolean shouldStartGame(ReceivePrivmsg event) {
        String actualChannel = event.getChannel();

        if (JEOPARDY_CHANNEL.equals(actualChannel)) {
            return true;
        }
        event.reply("To play jeopardy join: " + JEOPARDY_CHANNEL);
        return false;
    }

    private synchronized void startGame(ReceivePrivmsg event) throws Exception {
        if (currentGame != null) {
            event.reply(currentGame.getAlreadyPlayingString());
        }
        else {
            currentGame = new TriviaGame();
            event.reply(currentGame.getIntroString());
            startTimer(event);
        }
    }

    private void startTimer(final ReceivePrivmsg event) {
        timer.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TIMEOUT);
                    timeoutTimer(event);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void timeoutTimer(ReceivePrivmsg event) {
        String message = currentGame.getTimeoutString();
        event.reply(message);
        currentGame = null;
    }

    private void stopTimer() {
        timer.shutdownNow();
        timer = Executors.newFixedThreadPool(1);
    }


    private synchronized void finishGame(ReceivePrivmsg event, String answer) {
        if (currentGame != null) {
            String answerer = event.getSender();
            if (currentGame.isCorrect(answer)) {
                stopTimer();
                event.reply(String.format("Congrats %s, you win %d karma!", event.getSender(), currentGame.getStakes()));
                KarmaDB.modNounKarma(answerer, currentGame.getStakes());
                currentGame = null;
            }
            else {
                event.reply(String.format("Sorry %s! That is incorrect, you lose %d karma.",
                        event.getSender(), currentGame.getStakes()));
                KarmaDB.modNounKarma(answerer, -1 * currentGame.getStakes());
            }
        }
        else {
            event.reply("No active game. Start a new one with !jeopardy");
        }
    }


    private static class TriviaGame {

        private int stakes;
        private String question;
        private String answer;
        private String category;

        private static final String API_URL = "http://jservice.io/api/random";

        public TriviaGame() throws Exception {
            JSONObject triviaJSON = getQuestionJSON();
            question = triviaJSON.getString("question");
            category= triviaJSON.getJSONObject("category").getString("title");
            answer = Jsoup.parse(triviaJSON.getString("answer")).text();
            stakes = getRandomKarma();
        }

        private int getRandomKarma() {
            return new Random().nextInt(10) + 1;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }

        public int getStakes() {
            return stakes;
        }

        private boolean isCorrect(String answer) {
            String formattedUserAnswer = formatAnswer(answer);
            String formattedActualAnswer = formatAnswer(this.answer);
            return formattedActualAnswer.equals(formattedUserAnswer);
        }

        private String formatAnswer(String answer) {
            return  answer.toLowerCase()
                    .replaceAll("\\(.*\\)", "")
                    .replaceAll("^the ", "")
                    .replaceAll("^a ", "")
                    .replaceAll("^an ", "")
                    .replaceAll("[^A-Za-z\\d]", "");
        }

        private String getQuestionBlurb() {
            return String.format(
                    "Your category is: %s\nFor %s karma:\n%s",
                    RED + category + RESET,
                    GREEN + String.valueOf(stakes) + RESET,
                    BLUE + question + RESET);
        }

        public String getIntroString() {
            return "Welcome to Bot Jeopardy!\n" + getQuestionBlurb() + "\n30 seconds on the clock.";
        }

        public String getAlreadyPlayingString() {
            return "A game is already in session!\n" + getQuestionBlurb();
        }

        public String getTimeoutString() {
            return String.format("Your 30 seconds is up! The answer we were looking for was:\n%s",
                    BLUE + answer + RESET);
        }

        private String getQuestionContent() throws Exception {
            String content;
            URLConnection connection;
            connection = new URL(API_URL).openConnection();
            connection.addRequestProperty(
                    "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)"
            );
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
            return content;
        }

        private JSONObject getQuestionJSON() throws Exception {
            String triviaContent = getQuestionContent();
            JSONObject triviaJSON = new JSONArray(triviaContent).getJSONObject(0);
            String question = triviaJSON.getString("question").trim();
            if (question.equals("") || question.contains("seen here")) {
                return getQuestionJSON();
            }
            return triviaJSON;
        }
    }

}
