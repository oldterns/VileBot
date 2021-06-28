package com.oldterns.vilebot.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.Bot;
import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.database.KarmaDB;
import com.oldterns.vilebot.util.TimeService;
import com.oldterns.vilebot.util.URLFactory;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ApplicationScoped
@Bot("JeoBot1")
public class TriviaService {
    final static String JEOPARDY_CHANNEL = "${vilebot.jeopardy.channel}";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final String RED = "\u000304";

    private static final String RESET = "\u000f";

    private static final String BLUE = "\u000302";

    private static final String GREEN = "\u000303";

    private static final Integer RECURSION_LIMIT = 10;

    @Inject
    KarmaDB karmaDB;

    @Inject
    URLFactory urlFactory;

    @Inject
    TimeService timeService;

    @Inject
    ObjectMapper objectMapper;

    TriviaGame game = null;
    TriviaQuestions triviaQuestions = null;
    Future<?> timeoutFuture;
    Lock gameLock = new ReentrantLock();

    @OnChannelMessage(value = "!jeopardy", channel = JEOPARDY_CHANNEL)
    public String startGame(ChannelMessageEvent channelMessageEvent) {
        try {
            gameLock.lock();
            if (game != null) {
                return String.join("\n", game.getAlreadyPlayingString());
            }
            int tries = 0;
            while (tries < RECURSION_LIMIT && (triviaQuestions == null || triviaQuestions.isEmpty())) {
                triviaQuestions = new TriviaQuestions(urlFactory, objectMapper);
                tries++;
            }
            if (triviaQuestions.isEmpty()) {
                throw new IllegalStateException("Tried (" + tries + ") times, all responses were invalid");
            }
            game = new TriviaGame(triviaQuestions.getQuestion());
            timeoutFuture = timeService.onTimeout(TIMEOUT, () -> {
                try {
                    gameLock.lock();
                    game.getTimeoutString().forEach(channelMessageEvent::sendReply);
                    game = null;
                    timeoutFuture = null;
                } finally {
                    gameLock.unlock();
                }
            });
            return String.join("\n", game.getIntroString());
        } catch (Exception e) {
            e.printStackTrace();
            return "I don't feel like playing.";
        } finally {
            gameLock.unlock();
        }
    }

    @OnChannelMessage(value = "!(whatis|whois) @answer", channel = JEOPARDY_CHANNEL)
    public String tryAnswer(User answererUser, String answer) {
        try {
            gameLock.lock();
            if (game == null) {
                game = null;
                return "No active game. Start a new one with !jeopardy";
            }
            String answerer = Nick.getNick(answererUser).getBaseNick();
            int stakes = game.getStakes();
            if (game.isCorrect(answer)) {
                game = null;
                timeoutFuture.cancel(true);
                timeoutFuture = null;

                karmaDB.modNounKarma(answerer, stakes);
                return String.format("Congrats %s, you win %d karma!", answerer, stakes);
            } else {
                karmaDB.modNounKarma(answerer, -stakes);
                return String.format("Sorry %s! That is incorrect, you lose %d karma.", answerer, stakes);
            }
        }
        finally {
            gameLock.unlock();
        }
    }

    private static class TriviaQuestions {
        private static final String API_URL = "http://jservice.io/api/random?count=100";

        private List<TriviaQuestion> triviaQuestionList;

        public TriviaQuestions(URLFactory urlFactory, ObjectMapper objectMapper) throws IOException {
            JsonNode response = objectMapper.readTree(urlFactory.build(API_URL));
            triviaQuestionList = new ArrayList<>();
            response.forEach(questionJson -> {
                String question = questionJson.get("question").asText().trim();
                boolean invalid = !questionJson.get("invalid_count").isNull();
                if (question.equals( "" ) || question.contains( "seen here" ) || invalid) {
                    return;
                }
                String category = questionJson.get( "category" ).get( "title" ).asText();
                String answer = questionJson.get( "answer" ).asText();
                int stakes = getStakes( questionJson );
                triviaQuestionList.add(new TriviaQuestion(stakes, question, answer, category));
            });
        }

        public TriviaQuestion getQuestion() {
            return triviaQuestionList.remove(0);
        }

        private int getStakes( JsonNode trivia )
        {
            if ( trivia.has( "value" ) && !trivia.get( "value" ).isNull() )
            {
                return trivia.get( "value" ).intValue() / 100;
            }
            return 5;
        }

        public boolean isEmpty() {
            return triviaQuestionList.isEmpty();
        }
    }

    private static class TriviaQuestion {
        final int stakes;
        final String question;
        final String answer;
        final String category;

        public TriviaQuestion(int stakes, String question, String answer, String category) {
            this.stakes = stakes;
            this.question = question;
            this.answer = answer;
            this.category = category;
        }
    }

    private static class TriviaGame
    {

        TriviaQuestion triviaQuestion;

        TriviaGame(TriviaQuestion triviaQuestion)
        {
            this.triviaQuestion = triviaQuestion;
        }

        String getQuestion()
        {
            return triviaQuestion.question;
        }

        String getAnswer()
        {
            return triviaQuestion.answer;
        }

        int getStakes()
        {
            return triviaQuestion.stakes;
        }

        private boolean isCorrect( String answer )
        {
            String formattedUserAnswer = formatAnswer( answer );
            String formattedActualAnswer = formatAnswer( triviaQuestion.answer );
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

        private List<String> getQuestionBlurb()
        {
            List<String> questionBlurb = new ArrayList<>();
            questionBlurb.add( "Your category is: " + RED + triviaQuestion.category + RESET );
            questionBlurb.add( "For " + GREEN + triviaQuestion.stakes + RESET + " karma:" );
            questionBlurb.add( BLUE + triviaQuestion.question + RESET );
            return questionBlurb;
        }

        List<String> getIntroString()
        {
            List<String> introString = new ArrayList<>( getQuestionBlurb() );
            introString.add( "30 seconds on the clock." );
            return introString;
        }

        List<String> getAlreadyPlayingString()
        {
            List<String> alreadyPlayingString = new ArrayList<>();
            alreadyPlayingString.add( "A game is already in session!" );
            alreadyPlayingString.addAll( getQuestionBlurb() );
            return alreadyPlayingString;
        }

        List<String> getTimeoutString()
        {
            List<String> timeoutString = new ArrayList<>();
            timeoutString.add( "Your 30 seconds is up! The answer we were looking for was:" );
            timeoutString.add( BLUE + triviaQuestion.answer + RESET );
            return timeoutString;
        }
    }
}
