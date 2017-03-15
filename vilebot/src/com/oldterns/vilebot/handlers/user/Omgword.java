import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;
import net.engio.mbassy.listener.Handler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@HandlerContainer
public class Omgword {

    private static final Pattern questionPattern = Pattern.compile( "!omgword" );
    private static final Pattern answerPattern = Pattern.compile( "!answer (.*)" );
    private static final String wordListPath = Vilebot.getConfig().get("OmgwordList");
    private ArrayList<String> words = loadWords();
    private OmgwordGame currentGame;
    private String word;
    private String scrambled;
    private static final Random random = new SecureRandom();
    private static final long TIMEOUT  = 30000L;
    private static ExecutorService timer = Executors.newScheduledThreadPool(1);

    @Handler
    public void omgword(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher questionMatcher = questionPattern.matcher(text);
        Matcher answerMatcher = answerPattern.matcher(text);
        try {
            if (questionMatcher.matches()) {
                startGame(event);
            } else if (answerMatcher.matches()) {
                String answer = answerMatcher.group(1);
                finishGame(event, answer);
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private synchronized void startGame(ReceivePrivmsg event) throws Exception {
        if (currentGame != null) {
            event.reply(currentGame.getAlreadyPlayingString());
        }
        else {
            currentGame = new OmgwordGame();
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
        String answerer = BaseNick.toBaseNick(event.getSender());
        if (currentGame != null) {
            if (currentGame.isCorrect(answer)) {
                stopTimer();
                event.reply(String.format("Congrats %s, you win %d karma!", answerer, currentGame.getStakes()));
                KarmaDB.modNounKarma(answerer, currentGame.getStakes());
                currentGame = null;
            } else {
                event.reply(String.format("Sorry %s! That is incorrect, you lose %d karma.",
                        answerer, currentGame.getStakes()));
                KarmaDB.modNounKarma(answerer, -1 * currentGame.getStakes());
            }
        }
        else {
            event.reply("No active game. Start a new one with !omgword");
        }
    }

    private ArrayList<String> loadWords() {
        try {
            ArrayList<String> words = new ArrayList<>();
            List<String> lines = Files.readAllLines(Paths.get(wordListPath));
            for (String line : lines) {
                words.addAll(Arrays.asList(line.split(" ")));
            }
            return words;
        } catch (Exception e) {
            System.exit(1);
        }
        return null;
    }

    private class OmgwordGame {

        public String getAlreadyPlayingString() {
            return "A game is already in progress! Your word is: " + scrambled;
        }

        public String getIntroString() {
            word = "";
            int index = random.nextInt(words.size() - 1);
            word = words.get(index);
            scrambled = word;
            char[] chars = words.get(index).toCharArray();
            while (scrambled.equals(word)) {
                shuffleArray(chars);
                scrambled = new String(chars);
            }
            return "Welcome to omgword!\nFor " + getStakes() + " karma:\n" + scrambled +"\n30 seconds on the clock.";
        }

        public boolean isCorrect(String answer) {
            return answer.equals(word);
        }

        public String getTimeoutString() {
            return "Game over! The correct answer was: " + word;
        }

        public int getStakes() {
            return (int) Math.ceil(word.length()/2);
        }
    }

    private static void shuffleArray(char[] array) {
        char temp;
        int index;
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

}

