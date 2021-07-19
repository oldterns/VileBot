package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnChannelMessage;
import com.oldterns.vilebot.util.RandomProvider;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class KaomojiService {

    @Inject
    RandomProvider randomProvider;

    // Obtained from: https://raw.githubusercontent.com/cspeterson/splatmoji/master/data/emoticons/emoticons.tsv
    Map<String, List<String>> queryToKaomojiListMap;

    @PostConstruct
    public void constructQueryToKaomojiListMap() {
        queryToKaomojiListMap = new HashMap<>();
        try (InputStream kaomojiListResource = KaomojiService.class.getResourceAsStream("/kaomojilist.tsv")) {
            if (kaomojiListResource == null) {
                throw new IOException("File not found");
            }
            new BufferedReader(new InputStreamReader(kaomojiListResource, StandardCharsets.UTF_8))
                    .lines()
                    .forEach(line -> {
                        String[] parts = line.split("\t");
                        String kaomoji = parts[0];
                        String[] matchedQueries = parts[1].split(", ");
                        for (String matchedQuery : matchedQueries) {
                            queryToKaomojiListMap.computeIfAbsent(matchedQuery, key -> new ArrayList<>()).add(kaomoji);
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open wordlist file: ", e);
        }
    }

    @OnChannelMessage("!kaomoji @query")
    public String getKaomoji(String query) {
        String kaomoji = "";
        switch ( query )
        {
            case "wat":
                return getKaomoji("confused");
            case "nsfw":
            case "wtf":
                return "ⓃⒶⓃⒾ(☉൧ ಠ ꐦ)";
            case "vilebot":
                return "( ͡° ͜ʖ ͡° )";
            default:
                if (queryToKaomojiListMap.containsKey(query.toLowerCase())) {
                    kaomoji = randomProvider.getRandomElement(queryToKaomojiListMap.get(query.toLowerCase()));
                }
                break;
        }

        if ( kaomoji.isEmpty() )
        {
            kaomoji = "щ(ಥдಥщ)";
        }
        return kaomoji;
    }
}
