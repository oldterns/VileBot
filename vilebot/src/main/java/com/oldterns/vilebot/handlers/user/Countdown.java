package com.oldterns.vilebot.handlers.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.KarmaDB;
import com.oldterns.vilebot.util.BaseNick;

import bsh.EvalError;
import bsh.Interpreter;
import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;

@HandlerContainer
public class Countdown {

    private static final String COUNTDOWN_CHANNEL = Vilebot.getConfig().get("countdownChannel");
	private static final long TIMEOUT  = 45000L;
	
	
	private static final Pattern countdownPattern = Pattern.compile("^!countdown");
	private static final Pattern answerPattern = Pattern.compile("^!solution (.*)");
	private static CountdownGame currGame = null;
    private static ExecutorService timer = Executors.newScheduledThreadPool(1);
    
    @AssignedBot
    private KeratinBot bot;

	private static class CountdownGame {
		private final int VALID_NUMBER_COUNT = 6;
		private final List <Integer> LARGE_NUMBERS = new ArrayList<>(Arrays.asList(25, 50, 75, 100));
		private final List <Integer> SMALL_NUMBERS = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
		private List <Integer> questionNumbers = new ArrayList<>();
		private Hashtable <String, String> submissions = new Hashtable<>(); 
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
			// target number should be between 100-999.
			targetNumber = rand.nextInt(899) + 100;
			// have karma stakes random from 1-10 for now. Not working yet.
			stakes = rand.nextInt(11);
		}
		
		private int getLargeNumberCount() {
			return largeNumberCount;
		}

		private int getSmallNumberCount() {
			return smallNumberCount;
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
			return "Welcome to Countdown!\n"+getQuestion()+"\n Good luck! You have 45 seconds.";
		}
		
		private String getQuestion() {
			return "Your numbers are: \n"+getQuestionNumbers()+"\nYour target is: \n"+getTargetNumber();
		}
		
		private void shuffleNumbers() {
			Collections.shuffle(LARGE_NUMBERS);
			Collections.shuffle(SMALL_NUMBERS);
		}
		
		private int interpretedAnswer(String answer) throws Exception {
			if (noSpecialCharacters(answer) && hasCorrectNumbers(answer)) {
				interpreter = new Interpreter();
				try {
					interpreter.eval("result = "+answer);
					return ((int) interpreter.get("result"));
				} catch (EvalError e) {
					e.printStackTrace();
					throw e;
				}
			} else {
				throw new ArithmeticException("invalid answer string");
			}
		}
		
		private boolean noSpecialCharacters(String answer) {
			return answer.matches("^[0-9*+\\-\\/()\\W]+$");
		}
		
		private boolean hasCorrectNumbers(String answer) {
			List <String> numList = Arrays.asList(answer.replaceAll("[^-?0-9]+", " ").trim().split(" "));
			List <Integer> questionNums = new ArrayList<Integer>(getQuestionNumbers());
			for (String num : numList) {
				int number = Integer.valueOf(num);
				if (questionNums.contains(number)) {
					questionNums.remove((Integer)number);
				} else {
					return false;
				}
			}
			return true;
			}
		
		private String getTimeoutString() {
            return "Your 30 seconds is up! A possible answer would've been:\n PLACEHOLDER";
		}
		
		private String alreadyPlaying() {
			return "A current game is already in progress.\n"+getQuestion();
		}
	}
	
	@Handler
	public void countdown(ReceivePrivmsg event) {
		String text = event.getText();
		Matcher countdownMatcher= countdownPattern.matcher(text);
		Matcher answerMatcher= answerPattern.matcher(text);
		if (countdownMatcher.matches() && correctChannel(event)) {
			startCountdownGame(event);
		} else if (answerMatcher.matches() && correctSolutionChannel(event)) {
			String answer = answerMatcher.group(1);
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
	  
   private boolean isPrivate(ReceivePrivmsg event) {
       try {
           bot.getChannel(event.getChannel()).getNicks();
       }
       catch (Exception e) {
           return true;
       }
       return false;
   }
   
   private boolean inGameChannel(ReceivePrivmsg event) {
	   String currChannel = event.getChannel();
	   if (COUNTDOWN_CHANNEL.equals(currChannel)) {
	       try {
	          return bot.getChannel(event.getChannel()).getNicks().contains(event.getSender());
	       }
	       catch (Exception e) {
	           return false;
	       }
	   }
       return true;
   }
	
	private boolean correctSolutionChannel(ReceivePrivmsg event) {
		String currChannel = event.getChannel();
		if (COUNTDOWN_CHANNEL.equals(currChannel) || (isPrivate(event) && inGameChannel(event))) {		
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
	
	private synchronized void checkAnswer(ReceivePrivmsg event, String submission) {
		String contestant = BaseNick.toBaseNick(event.getSender());
		if (currGame != null) {
			try {
				int contestantAnswer = currGame.interpretedAnswer(submission);
				if (!currGame.submissions.containsKey(contestant)) {
					currGame.submissions.put(contestant, submission+" = "+contestantAnswer);
					if (isPrivate(event)) {
						event.reply(String.format("Your submission of %s has been recieved!", submission));
					} else {
						event.reply(String.format("%s's submission recieved!", contestant));
					}
				} else {
		            event.reply(String.format("Sorry %s, you've already submitted for this game.", contestant));
				}			
			} catch (Exception e) {
				e.printStackTrace();
                event.reply(String.format("Sorry %s! You have put an invalid answer, you lose %d karma.",
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
	   Set<String> keys = currGame.submissions.keySet();
	   event.reply(String.format("Your time is up! The target number was %s \n",currGame.getTargetNumber()));
	   if (!keys.isEmpty()) {
		   event.reply("The final submissions are: \n");
		   for (String key : keys) {
			   event.reply(key+", with "+ currGame.submissions.get(key)+"\n");
		   }
		   
	   } else {
		   event.reply("There were no submissions for this game. Better luck next time!");
	   }
       currGame = null;
   }

   private void stopTimer() {
       timer.shutdownNow();
       timer = Executors.newFixedThreadPool(1);
   }
}
