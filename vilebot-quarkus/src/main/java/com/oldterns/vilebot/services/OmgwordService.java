package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.Bot;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.RandomProvider;
import com.oldterns.vilebot.util.TimeService;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Based off of the omgword game from CasinoBot: http://casinobot.codeplex.com/
 */
@Bot("OmgwordBot")
@ApplicationScoped
public class OmgwordService {

    final static String OMGWORD_CHANNEL = "${vilebot.omgword.channel}";

    @Inject
    RandomProvider randomProvider;

    @Inject
    TimeService timeService;

    @Inject
    KarmaDB karmaDB;

    List<String> wordList;
    Map<AnagramKey, Set<String>> anagramKeyToAnswerSetMap;

    OmgwordGame currentGame;
    Lock lock = new ReentrantLock();

    @PostConstruct
    void buildAnagramKeyToAnswerSetMap() {
        anagramKeyToAnswerSetMap = new HashMap<>();
        try (InputStream wordListResource = OmgwordService.class.getResourceAsStream("/wordlist.txt")) {
            if (wordListResource == null) {
                throw new IOException("File not found");
            }
            wordList = new BufferedReader(new InputStreamReader(wordListResource, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toList());

            for (String word : wordList) {
                anagramKeyToAnswerSetMap.computeIfAbsent(new AnagramKey(word),
                        (key) -> new HashSet<>()).add(word);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open wordlist file: ", e);
        }
    }

    @OnChannelMessage(value = "!omgword", channel = OMGWORD_CHANNEL)
    public String startGame(ChannelMessageEvent event) {
        try {
            lock.lock();
            if (currentGame != null) {
                return currentGame.getAlreadyPlayingString();
            }
            currentGame = new OmgwordGame(event);
            return currentGame.getIntroString();
        } finally {
            lock.unlock();
        }
    }

    @OnChannelMessage(value = "!answer @answer", channel = OMGWORD_CHANNEL)
    public String onAnswer(User answerer, String answer) {
        try {
            lock.lock();

            if (currentGame == null) {
                return "No active game. Start a new one with !omgword";
            }

            Nick answererNick = Nick.getNick(answerer);
            if (currentGame.isCorrect(answer)) {
                String out = String.format( "Congrats %s, you win %d karma!", answererNick.getFullNick(),
                        currentGame.getStakes() );
                karmaDB.modNounKarma(answererNick.getBaseNick(), currentGame.getStakes());
                currentGame.cancelTimeout();
                currentGame = null;
                return out;
            } else {
                karmaDB.modNounKarma(answererNick.getBaseNick(), -currentGame.getStakes());
                return String.format( "Sorry %s! That is incorrect, you lose %d karma.", answererNick.getFullNick(),
                        currentGame.getStakes() );
            }
        } finally {
            lock.unlock();
        }
    }

    private class OmgwordGame
    {
        AnagramKey anagramKey;
        String scrambled;
        Future<?> timeoutHandler;

        public OmgwordGame(ChannelMessageEvent event) {
            String word = randomProvider.getRandomElement(wordList);
            anagramKey = new AnagramKey(word);
            scrambled = word;

            while ( anagramKeyToAnswerSetMap.get(anagramKey).contains(scrambled) ) {
                scrambled = scrambleWord(scrambled);
            }

            timeoutHandler = timeService.onTimeout(Duration.ofSeconds(30), () -> {
                try {
                    lock.lock();
                    event.sendReply(getTimeoutString());
                    currentGame = null;
                } finally {
                    lock.unlock();
                }
            });
        }

        public void cancelTimeout() {
            timeoutHandler.cancel(true);
        }

        String getAlreadyPlayingString()
        {
            return "A game is already in progress! Your word is: " + scrambled;
        }

        String getIntroString()
        {
            return "Welcome to omgword!\nFor " + getStakes() + " karma:\n" + scrambled +
                    "\n30 seconds on the clock.";
        }

        boolean isCorrect( String answer )
        {
            return anagramKeyToAnswerSetMap.get(anagramKey).contains(answer);
        }

        String getTimeoutString()
        {
            Set<String> possibleAnswers = anagramKeyToAnswerSetMap.get(anagramKey);
            String answerPlural = possibleAnswers.size() == 1? "answer was: " : "answers were: ";
            return "Game over! The correct " + answerPlural + String.join(", ", anagramKeyToAnswerSetMap.get(anagramKey));
        }

        int getStakes()
        {
            return (int) Math.ceil( scrambled.length() >> 1 );
        }

        public String scrambleWord(String word) {
            StringBuilder newWord = new StringBuilder();
            StringBuilder remaining = new StringBuilder(word);
            for (int i = 0; i < word.length(); i++) {
                int pickedCharIndex = randomProvider.getRandomInt(remaining.length());
                newWord.append(remaining.charAt(pickedCharIndex));
                remaining.deleteCharAt(pickedCharIndex);
            }
            return newWord.toString();
        }
    }

    public static final class AnagramKey {
        final String lettersSorted;

        public AnagramKey(String word) {
            lettersSorted = word.chars()
                .sorted()
                .collect(StringBuilder::new,
                         StringBuilder::appendCodePoint,
                         StringBuilder::append)
                .toString();
        }

        public String getLettersSorted() {
            return lettersSorted;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            AnagramKey that = (AnagramKey) o;
            return Objects.equals(lettersSorted, that.lettersSorted);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lettersSorted);
        }
    }
}
