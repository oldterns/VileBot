package com.oldterns.vilebot.handlers.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;

import bsh.Interpreter;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;

@HandlerContainer
public class Countdown {

    private static final String COUNTDOWN_CHANNEL = Vilebot.getConfig().get("countdownChannel");
	private static final long TIMEOUT  = 100000L;
	
	
	private static final Pattern countdownPattern = Pattern.compile("^!countdown");
	private static final Pattern answerPattern = Pattern.compile("^!solution (.*)");
	private static CountdownGame currGame = null;
    private static ExecutorService timer = Executors.newScheduledThreadPool(1);

	private static class CountdownGame {
		private final int VALID_NUMBER_COUNT = 6;
		private final List <Integer> LARGE_NUMBERS = new ArrayList<>(Arrays.asList(25, 50, 75, 100));
		private final List <Integer> SMALL_NUMBERS = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
		private List <Integer> questionNumbers = new ArrayList<>();
		private int largeNumberCount;
		private int smallNumberCount;
		private int targetNumber;
		private int stakes;
		Interpreter interpreter;

		Random rand = new Random();
		
		public CountdownGame() {
			shuffleNumbers();
			largeNumberCount = rand.nextInt(LARGE_NUMBERS.size()+1);
			smallNumberCount = VALID_NUMBER_COUNT - largeNumberCount;
			questionNumbers.addAll(LARGE_NUMBERS.subList(0, largeNumberCount));
			questionNumbers.addAll(SMALL_NUMBERS.subList(0, smallNumberCount));
			Collections.sort(questionNumbers);
			// target number should be between 100-999.
			targetNumber = rand.nextInt(899) + 100;
			// have karma stakes random from 1-10 for now. Not working yet.
			stakes = rand.nextInt(10);
			interpreter = new Interpreter();
		}

		
		private int getTargetNumber() {
			return targetNumber;
		}
		
		private List<Integer> getQuestionNumbers() {
			return questionNumbers;
		}
		
		private int getStakes() {
			return stakes;
		}
		
		private String getCountdownIntro() {
			return "Welcome to Countdown!\n"+getQuestion()+"\n Good luck! You have one minute.";
		}
		
		private String getQuestion() {
			return "Your numbers are: \n"+getQuestionNumbers()+"\nYour target is: \n"+getTargetNumber();
		}
		
		private void shuffleNumbers() {
			Collections.shuffle(LARGE_NUMBERS);
			Collections.shuffle(SMALL_NUMBERS);
		}
		
		// TODO: try finding a possible answer
		
		private boolean isCorrect(String answer) {
			if (hasCorrectNumbers(answer)) {
				return true;
			}
			return false;
		}
		
		
		private boolean hasCorrectNumbers(String answer) {
		// get all integers and confirm they are the same as the valid choices
			List <String> numList = Arrays.asList(answer.replaceAll("[^-?0-9]+", " ").trim().split(" "));
			List <Integer> contestantSolution = new ArrayList<>();
			for (String num : numList) {
				contestantSolution.add(Integer.valueOf(num));
			}
			Collections.sort(contestantSolution);
			return (contestantSolution.equals(getQuestionNumbers()));
		}
		
		private String getTimeoutString() {
            return "Your one minute is up! A possible answer would've been:\n PLACEHOLDER";
		}
		
		private String alreadyPlaying() {
			return "A current game is already in progress.\n"+getQuestionNumbers()+"\nYour target is \n"+getTargetNumber();
		}
	}
	
	@Handler
	public void countdown(ReceivePrivmsg event) {
		String text = event.getText();
		Matcher countdownMatcher= countdownPattern.matcher(text);
		Matcher answerMatcher= answerPattern.matcher(text);
		if (countdownMatcher.matches() && correctChannel(event)) {
			startCountdownGame(event);
		} else if (answerMatcher.matches() && correctChannel(event)) {
			String answer = answerMatcher.group(1);
			event.reply("your solution is :"+answer);
			checkAnswer(event, answer);
		}
	}
	
	private boolean correctChannel(ReceivePrivmsg event) {
		String currChannel = event.getChannel();
		if (COUNTDOWN_CHANNEL.equals(currChannel)) {
			return true;
		} else {
			event.reply("To play Countdown join : " + COUNTDOWN_CHANNEL);
			return false;
		}
	}
	
	private synchronized void startCountdownGame(ReceivePrivmsg event) {
		if (currGame == null) {
			currGame = new CountdownGame();
			event.reply(currGame.getCountdownIntro());
			startTimer(event);
		} else {
			event.reply(currGame.alreadyPlaying());
		}
	}
	
	private synchronized void checkAnswer(ReceivePrivmsg event, String answer) {
		System.out.println("checking answer");
		String contestant = BaseNick.toBaseNick(event.getSender());
		if (currGame != null) {
			event.reply("current game is ongoing");
			if (currGame.isCorrect(answer)) {
				stopTimer();	
                event.reply(String.format("Congrats %s, you win %d karma!", contestant, currGame.getStakes()));
                KarmaDB.modNounKarma(contestant, currGame.getStakes());
                currGame = null;
			} else {
                event.reply(String.format("Sorry %s! That is incorrect, you lose %d karma.",
                		contestant, currGame.getStakes()));
                KarmaDB.modNounKarma(contestant, -1 * currGame.getStakes());
                }
		} else {
			event.reply("No active game. Start a new one with !countdown");
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
       String message = currGame.getTimeoutString();
       event.reply(message);
       currGame = null;
   }

   private void stopTimer() {
       timer.shutdownNow();
       timer = Executors.newFixedThreadPool(1);
   }
	
	
}
