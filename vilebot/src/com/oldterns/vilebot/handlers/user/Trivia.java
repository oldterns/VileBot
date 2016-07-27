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
    private static final String JEOPARDY_CHANNEL = "jeopardyChannel";
    private static final int TIMEOUT  = 30000;
    private static ExecutorService timer = Executors.newScheduledThreadPool(1);

    @Handler
    public void doTrivia(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher questionMatcher = questionPattern.matcher(text);
        Matcher answerMatcher = answerPattern.matcher(text);

        try {
            if (questionMatcher.matches() && shouldStartGame(event.getChannel())) {
                startGame(event);
            } else if (answerMatcher.matches() && shouldStartGame(event.getChannel())) {
                String answer = answerMatcher.group(2);
                finishGame(event, answer);
            }
        } catch(Exception e) {
            event.reply("I don't feel like playing.");
            e.printStackTrace();
        }
    }

    private boolean shouldStartGame(String channel) {
        String limitingChannel = Vilebot.getConfig().get(JEOPARDY_CHANNEL);
        return limitingChannel == null || limitingChannel.equals(channel);
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
            String jsonContent = getQuestionJson();
            JSONObject triviaJSON = new JSONArray(jsonContent).getJSONObject(0);
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
                    .replaceAll("[^A-Za-z\\s\\d]", "")
                    .replaceAll("^the ", "")
                    .replaceAll("^a ", "")
                    .replaceAll("^an ", "");
        }

        private String getQuestionBlurb() {
            return String.format(
                    "Your category is: %s\nFor %d karma:\n%s",
                    category, stakes, question);
        }

        public String getIntroString() {
            return "Welcome to VileBot trivia!\n" + getQuestionBlurb() + "\nYou have 30 seconds!";
        }

        public String getAlreadyPlayingString() {
            return "A game is already in session!\n" + getQuestionBlurb();
        }

        public String getTimeoutString() {
            return String.format("Your 30 seconds is up! The answer we were looking for was:\n%s", answer);
        }

        private String getQuestionJson() throws Exception {
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
    }

}
