package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.database.LogDB;
import com.oldterns.vilebot.util.RandomProvider;
import com.oldterns.vilebot.util.Zalgo;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by emmett on 12/08/15.
 */
@ApplicationScoped
public class MarkovService {

    @Inject
    LogDB logDB;

    @Inject
    RandomProvider randomProvider;

    @OnMessage("!speak")
    public String speak() {
        return generatePhase();
    }

    @OnMessage("!gospel")
    public String gospel() {
        return Zalgo.generate(generatePhase());
    }

    private String generatePhase() {
        Map<String, List<String>> markovMatrix = getMarkovMatrix();
        List<String> keyList = new ArrayList<>(markovMatrix.keySet());
        String key = randomProvider.getRandomElement(keyList);
        StringBuilder phrase = new StringBuilder();
        while (key != null && phrase.length() < 1000) {
            phrase.append( key ).append( " " );
            if (shouldEnd(key)) {
                break;
            }
            List<String> afterKeyList = markovMatrix.get(key);
            if (afterKeyList == null) {
                break;
            }
            key = randomProvider.getRandomElement(afterKeyList);
        }
        return phrase.toString().replace('\n', ' ');
    }

    private boolean shouldEnd(String key) {
        return key.endsWith( "!" ) || key.endsWith( "?" ) || key.endsWith( "." );
    }

    private Map<String, List<String>> getMarkovMatrix() {
        Map<String, List<String>> markovMatrix = new HashMap<>();
        String[] words = logDB.getLog().split("\\s");
        for ( int i = 0; i < words.length - 3; i++ ) {
            String key = words[i] + " " + words[i + 1];
            String value = words[i + 2] + " " + words[i + 3];
            if (key.equals(value)) {
                continue;
            }
            markovMatrix.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return markovMatrix;
    }
}
