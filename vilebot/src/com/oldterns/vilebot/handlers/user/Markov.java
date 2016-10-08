package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.db.LogDB;
import net.engio.mbassy.listener.Handler;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by emmett on 12/08/15.
 */

@HandlerContainer
public class Markov {

    Map<String, List<String>> markovMap = new HashMap<String, List<String>>();
    private static final Pattern cmd = Pattern.compile("^!speak$");

    @AssignedBot
    private KeratinBot bot;

    @Handler
    public void speak(ReceivePrivmsg message) {

        String text = message.getText();
        boolean markovMap = cmd.matcher(text).matches();

        if (markovMap) {
            train();
            String phrase = generatePhrase();
            phrase = mangleNicks(phrase, message);
            message.reply(phrase);
        }
    }

    private void train() {
        String data = LogDB.getLog();
        fillMarkovMap(data);
    }

    private void fillMarkovMap(String data) {
        String[] words = data.split("\\s+");

        for (int i = 0; i < words.length - 3; i++) {
            String key = words[i] + " " + words[i + 1];
            String value = words[i + 2] + " " + words[i + 3];

            if (key.equals(value)) {
                continue;
            } else if (markovMap.get(key) != null) {
                markovMap.get(key).add(value);
            } else {
                List<String> valueList = new ArrayList<String>();
                valueList.add(value);
                markovMap.put(key, valueList);
            }
        }
    }

    private String generatePhrase() {
        Random random = new Random();
        String key = getRandomKey(random);
        String phrase = "";

        while (key != null && phrase.length() < 1000) {
            phrase += key + " ";
            if (shouldEnd(key)) {
                break;
            }
            key = nextKey(key, random);
        }

        return phrase.replace("\n", " ");
    }

    private String nextKey(String key, Random random) {
        List<String> valueList = markovMap.get(key);
        if (valueList == null) {
            return null;
        }

        return valueList.get(random.nextInt(valueList.size()));
    }

    private String getRandomKey(Random random) {
        Object[] values = markovMap.keySet().toArray();
        return (String) values[random.nextInt(values.length)];
    }

    private boolean shouldEnd(String key) {
        return (key.endsWith("!") || key.endsWith("?") || key.endsWith("."));
    }

    private String mangleNicks(String phrase, ReceivePrivmsg msg) {
        List<String> nicks = getNicks(msg);

        if (nicks.isEmpty()) {
            return phrase;
        }

        StringBuilder reply = new StringBuilder();
        for (String word : phrase.split(" ")) {
            reply.append(" ");
            reply.append(
                    inside(nicks, word) ? mangled(word) : word
            );
        }
        return reply.toString().trim();
    }

    private List<String> getNicks(ReceivePrivmsg msg) {
        try {
            return bot.getChannel(msg.getChannel()).getNicks();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private boolean inside(List<String> nicks, String word) {
        for (String nick : nicks) {
            if (word.contains(nick)) {
                return true;
            }
        }
        return false;
    }

    private String mangled(String word) {
        return new StringBuilder(word).reverse().toString();
    }

}