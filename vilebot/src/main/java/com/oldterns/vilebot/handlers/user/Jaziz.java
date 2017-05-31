package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import net.engio.mbassy.listener.Handler;
import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eunderhi on 27/07/16.
 */
@HandlerContainer
public class Jaziz {

    private static final String API_KEY = Vilebot.getConfig().get("thesaurusKey");
    public static final String API_URL = "http://words.bighugelabs.com/api/2/" + API_KEY + "/";
    public static final String API_FORMAT = "/json";
    private static final Random random = new Random();

    private static final Pattern jazizPattern = Pattern.compile( "^!jaziz (.+)" );

    @Handler
    public void jaziz(ReceivePrivmsg event) {
        Matcher questionMatcher = jazizPattern.matcher(event.getText());
        if (questionMatcher.matches()) {
            String message = questionMatcher.group(1);
            try {
                message = jazizify(message);
                event.reply(message);
            }
            catch (Exception e) {
                event.reply("eeeh");
                e.printStackTrace();
            }
        }
    }

    private String jazizify(String message) throws Exception {
        String[] words = getWords(message);
        for (int i = 0; i < words.length; i++) {
            String replacement = words[i].length() > 3 ? randomChoice(getSynonyms(words[i])) : words[i];
            if (!replacement.isEmpty()) {
                words[i] = replacement;
            }
        }
        return stringify(words);
    }

    private String[] getWords(String message) {
        return message
                .toLowerCase()
                .replaceAll("[^A-Za-z\\s]", "")
                .split(" ");
    }

    private List<String> getSynonyms(String word) throws Exception {
        JSONObject json = new JSONObject(getContent(word));

        List<String> synonyms = new ArrayList<String>();
        String[] wordTypes = {"adjective", "noun", "adverb", "verb", "pronoun"};
        for (String type : wordTypes) {
            if (json.has(type)) {
                JSONArray syns = getSyns(json.getJSONObject(type));
                synonyms.addAll(jsonToList(syns));
            }
        }
        return synonyms;
    }

    private JSONArray getSyns(JSONObject json) throws JSONException {
        return json.has("syn") ? json.getJSONArray("syn") : new JSONArray();
    }

    private String randomChoice(List<String> list) {
        if (list.size() == 0) {
            return "";
        }
        int index = random.nextInt(list.size());
        return list.get(index);
    }

    private String stringify(String[] list) {
        StringBuilder builder = new StringBuilder();
        for (String word : list) {
            builder.append(word).append(" ");
        }
        return builder.toString().trim();
    }

    private List<String> jsonToList(JSONArray array) throws JSONException {
        List<String> words = new ArrayList<String>();
        for (int i = 0; i < array.length(); i++) {
            words.add(array.getString(i));
        }
        return words;
    }

    private String getContent(String word) throws Exception {
        String content;
        URLConnection connection;
        connection =  new URL(API_URL + word + API_FORMAT).openConnection();
        connection.addRequestProperty(
                "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)"
        );
        try {
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
            return content;
        }
        catch (FileNotFoundException e) {
            return "{}";
        }
    }

}
