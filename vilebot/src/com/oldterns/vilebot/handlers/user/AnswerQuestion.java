package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import com.oldterns.vilebot.Vilebot;
import net.engio.mbassy.listener.Handler;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eunderhi on 13/08/15.
 */

@HandlerContainer
public class AnswerQuestion {

    private static final Pattern questionPattern = Pattern.compile("^!(tellme)\\s(.+)$");
    private final String API_KEY = Vilebot.getConfig().get("wolframKey");

    @Handler
    private void tellMe( ReceivePrivmsg event ) {
        String text = event.getText();
        Matcher matcher = questionPattern.matcher( text );

        if (matcher.matches()) {
            String question = matcher.group(2);
            String answer = getAnswer(question);
            event.reply(answer);
        }
    }
    String getAnswer(String searchTerm) {
        String URL = makeURL(searchTerm);
        String response = getContent(URL);
        String answer = parseResponse(response);
        return answer;
    }

    String makeURL(String searchTerm) {
        searchTerm = searchTerm.replace("+", "%2B");
        searchTerm = searchTerm.replace(" ", "+");
        String URL = "http://api.wolframalpha.com/v2/query?input="+searchTerm+"&appid="+API_KEY;
        return URL;
    }

    String getContent(String URL) {
        String content = null;
        URLConnection connection;
        try {
            connection =  new URL(URL).openConnection();
            Scanner scanner = new Scanner(connection.getInputStream());
            scanner.useDelimiter("\\Z");
            content = scanner.next();
        }
        catch ( Exception ex ) {
            ex.printStackTrace();
        }
        return content;
    }
    static String parseResponse(String response) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            org.w3c.dom.Document document = documentBuilder.parse(new InputSource(new StringReader(response)));
            NodeList nodeList = document.getElementsByTagName("subpod");
            String answer = nodeList.item(1).getTextContent().trim();

            if (answer.isEmpty()) {
                throw new Exception();
            }

            return answer;
        }
        catch(Exception e) {
            return "I couldn't find an answer for that.";
        }
    }
}
