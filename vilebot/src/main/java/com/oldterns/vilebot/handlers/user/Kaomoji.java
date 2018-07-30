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
 * Created by ltulloch on 07/24/18.
 */
@HandlerContainer
public class Kaomoji {

    public static final String API_URL = "https://jckcthbrt.stdlib.com/kaomoji/?search=";
    public static final String API_FORMAT = "/json";

    private static final Pattern kaomojiPattern = Pattern.compile( "^!kaomoji (.+)" );

    @Handler
    public void kaomoji(ReceivePrivmsg event) {
        Matcher questionMatcher = kaomojiPattern.matcher(event.getText());
        if (questionMatcher.matches()) {
            String message = questionMatcher.group(1);
            try {
                message = kaomojiify(message);
                event.reply(message);
            }
            catch (Exception e) {
                event.reply("ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)");
                e.printStackTrace();
            }
        }
    }

    private String kaomojiify(String message) throws Exception {
        String[] words = splitWords(message);
        String kaomoji = "";
        if (words.length <= 0 ){
            return "ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)";
        }
        if (words.length >= 1 ){
            if (words[0].equals("wat")){
                words[0] = "confused";
            }
            else if (words[0].equals("nsfw") || words[0].equals("wtf")){
                kaomoji = "ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)";
            }
            else if (words[0].equals("vilebot")){
                kaomoji = "( ͡° ͜ʖ ͡° )";

            }
            else {
                kaomoji = getKaomoji(words[0]);
            }

            if (kaomoji.isEmpty()){
               kaomoji = "щ(ಥдಥщ)";
            }
        }
        return kaomoji;
    }

    private String[] splitWords(String message) {
    	return message.split("\\b");
    }

    private String getKaomoji(String word) throws Exception {
        String emoji = "";
        JSONObject json = new JSONObject(getContent(word));
        if(json.has("success")){
            emoji = json.getString("emoji");
        }
        return emoji;
    }

    private String getContent(String word) throws Exception {
        String content;
        URLConnection connection;
        connection =  new URL(API_URL + word ).openConnection();
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
