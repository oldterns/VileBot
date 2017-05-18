package com.oldterns.vilebot.handlers.user;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.Vilebot;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;

/**
 * Created by ipun on 15/05/16.
 */
@HandlerContainer
public class Fortune {
    private static final Pattern FORTUNE_PATTERN = Pattern.compile( "^!fortune(.*)" );
    private static final String FORTUNE_LIST_PATH = Vilebot.getConfig().get("FortuneList");
    private static final String FORTUNE_INDEX_PATH = Vilebot.getConfig().get("FortuneIndex");
    private ArrayList<String> fortune = loadFortunes();
    private List<String> fortuneIndex = loadFortuneIndex();
    @Handler
    public void fortune(ReceivePrivmsg event) {
    	String text = event.getText();
    	Matcher fortuneMatcher = FORTUNE_PATTERN.matcher(text);
    	try {
    		if (fortuneMatcher.matches()) {
                String dirty = fortuneMatcher.group(1);
                if ( dirty == null || dirty.isEmpty() ) {
                	fortuneReply(event);
                }

    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		System.exit(1);
    	}
    }
    
    
    private void fortuneReply(ReceivePrivmsg event) {
    	int index = Integer.parseInt(fortuneIndex.get(new Random().nextInt(fortuneIndex.size()-1)));
    	String line = fortune.get(index);
    	while (!line.matches("%")) {
	    	event.reply(line);
	    	line = fortune.get(++index);
    	}
    }
    
    
    private ArrayList<String> loadFortunes() {
    	try {
    		ArrayList<String> fortunes = new ArrayList<>();
            List<String> lines = Files.readAllLines(Paths.get(FORTUNE_LIST_PATH));
    		for (String line : lines) {
    			fortunes.add(line);
    		}
    		return fortunes;
    	} catch (Exception e) {
            e.printStackTrace();
    		System.exit(1);
    	}
    	return null;
    }
    
    private List<String> loadFortuneIndex() {
    	try {
            String lines = new String(Files.readAllBytes(Paths.get(FORTUNE_INDEX_PATH)));
    		return Arrays.asList(lines.split("\n"));
    	} catch (Exception e) {
            e.printStackTrace();
    		System.exit(1);
    	}
    	return null;
    }
	
}

