package com.oldterns.vilebot.handlers.user;

/**
 * Created by eunderhi on 14/08/15.
 */

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by emmett on 12/08/15.
 */

@HandlerContainer
public class GetInfoOn {

    private static final Pattern questionPattern = Pattern.compile("^!(infoon)\\s(.+)$");
    private static final Pattern dumbQuestionPattern = Pattern.compile("^!(infun)\\s(.+)$");

    @Handler
    public void getInfo(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher foonMatch = questionPattern.matcher(text);
        //Matcher funMatch = dumbQuestionPattern.matcher(text);

        if (foonMatch.matches()) {
            String question = foonMatch.group(2);
            String queryModifier = " site:wikipedia.org";
            String answer = getWiki(question, queryModifier);
            event.reply(answer);
        }
    }

    String getWiki(String query, String queryModifier) {
        try {
            String wikiURL = getWikiURLFromGoogle(query + queryModifier);
            String wikipediaContent = getContent(wikiURL);
            String parsedWikiContent = parseResponse(wikipediaContent);
            return parsedWikiContent;
        } catch (Exception e) {
            return "Look, I don't know.";
        }
    }

    String getWikiURLFromGoogle(String query) throws Exception {
        String googleURL = makeGoogleURL(query);
        String googleResponse = getContent(googleURL);
        String wikiURL = getWikiLink(googleResponse);
        return wikiURL;
    }

    String makeGoogleURL(String query) throws Exception {
        query = encode(query);
        return "https://www.google.com/search?q=" + query;
    }

    String getWikiLink(String googleHTML) {
        Document doc = Jsoup.parse(googleHTML);
        Element link = doc.select("a[href*=/url?q=https://en.wikipedia]").first();
        return link.attr("href").replace("/url?q=", "").split("&")[0];
    }

    String getContent(String url) throws Exception {
        String content;
        URLConnection connection;
        connection = new URL(url).openConnection();
        connection.addRequestProperty(
                "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)"
        );
        Scanner scanner = new Scanner(connection.getInputStream());
        scanner.useDelimiter("\\Z");
        content = scanner.next();
        return content;
    }

    String parseResponse(String response) throws Exception {
        Document doc = Jsoup.parse(response);
        Element bodyDiv = doc.getElementById("mw-content-text");
        Element firstParagraph = bodyDiv.getElementsByTag("p").first();
        String answer = firstParagraph.text();
        if (answer.isEmpty()) {
            throw new Exception();
        }
        answer = answer.replaceAll("\\[[0-9]+\\]", "");
        return answer;
    }

    String encode(String string) throws Exception {
        return URLEncoder.encode(string, "UTF-8");
    }
}
