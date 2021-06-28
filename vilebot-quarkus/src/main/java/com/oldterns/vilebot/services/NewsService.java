package com.oldterns.vilebot.services;

import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.util.Colors;
import com.oldterns.vilebot.util.LimitService;
import com.oldterns.vilebot.util.URLFactory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.kitteh.irc.client.library.element.User;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.LimitExceededException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class NewsService {
    @Inject
    URLFactory urlFactory;

    @Inject
    LimitService newsLimit;

    @Inject
    LimitService fakeNewsLimit;

    Map<String, NewsChannel> fakeNewsFeedsByCategory = new LinkedHashMap<>();
    Map<String, NewsChannel> newsFeedsByCategory = new LinkedHashMap<>();

    private static final String DEFAULT_NEWS_CATEGORY = "toronto";
    private static final String DEFAULT_FAKE_NEWS_CATEGORY = "canada";
    int NUM_HEADLINES = 3;
    
    private NewsChannel getNewsChannel(String genre, String url) {
        try {
            return new NewsChannel(genre, urlFactory.build(url));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Unable to build url (" + url + "):", e);
        }
    }
    
    @PostConstruct
    public void setupMaps() {
        buildNewsFeedsByCategory();
        buildFakeNewsFeedsByCategory();
    }
    
    private void buildFakeNewsFeedsByCategory() {
        fakeNewsFeedsByCategory.put( "canada",
                getNewsChannel( "Countries",
                         "https://www.thebeaverton.com/rss" ) );
        fakeNewsFeedsByCategory.put( "usa", getNewsChannel( "Countries",
                 "https://www.theonion.com/rss" ) );
        fakeNewsFeedsByCategory.put( "belgium",
                getNewsChannel( "Countries",  "https://nordpresse.be/rss" ) );
        fakeNewsFeedsByCategory.put( "france",
                getNewsChannel( "Countries",  "http://www.legorafi.fr/rss" ) );
        fakeNewsFeedsByCategory.put( "india", getNewsChannel( "Countries",
                 "http://www.fakingnews.com/rss" ) );
        fakeNewsFeedsByCategory.put( "russia",
                getNewsChannel( "Countries",  "https://fognews.ru/rss" ) );
        fakeNewsFeedsByCategory.put( "serbia",
                getNewsChannel( "Countries",  "https://www.njuz.net/rss" ) );
        fakeNewsFeedsByCategory.put( "venezuela",
                getNewsChannel( "Countries",
                         "http://feeds.feedburner.com/elchiguirebipolar" ) );
        fakeNewsFeedsByCategory.put( "newzealand",
                getNewsChannel( "Countries",
                         "http://www.thecivilian.co.nz/rss" ) );
    }
    
    private void buildNewsFeedsByCategory() {
        newsFeedsByCategory.put( "top",
                getNewsChannel( "General", "https://rss.cbc.ca/lineup/topstories.xml" ) );
        newsFeedsByCategory.put( "world",
                getNewsChannel( "General",
                        "https://news.google.com/news/rss/headlines/section/topic/WORLD?ned=us&hl=en" ) );
        newsFeedsByCategory.put( "canada",
                getNewsChannel( "General",
                         "https://rss.cbc.ca/lineup/canada.xml" ) );
        newsFeedsByCategory.put( "usa",
                getNewsChannel( "General",
                         "http://feeds.reuters.com/Reuters/domesticNews" ) );
        newsFeedsByCategory.put( "britain",
                getNewsChannel( "General",
                         "http://feeds.bbci.co.uk/news/uk/rss.xml" ) );
        newsFeedsByCategory.put( "redhat",
                getNewsChannel( "Open Source",
                         "https://www.redhat.com/en/rss/blog/channel/red-hat-news" ) );
        newsFeedsByCategory.put( "fedora", getNewsChannel( "Open Source",
                 "http://fedoraplanet.org/rss20.xml" ) );
        newsFeedsByCategory.put( "openshift",
                getNewsChannel( "Open Source",
                         "https://blog.openshift.com/category/news/rss" ) );
        newsFeedsByCategory.put( "opensource",
                getNewsChannel( "Open Source",  "https://opensource.com/feed" ) );
        newsFeedsByCategory.put( "politics",
                getNewsChannel( "Topics",
                         "https://rss.cbc.ca/lineup/politics.xml" ) );
        newsFeedsByCategory.put( "business",
                getNewsChannel( "Topics",
                         "https://rss.cbc.ca/lineup/business.xml" ) );
        newsFeedsByCategory.put( "health",
                getNewsChannel( "Topics",
                         "https://rss.cbc.ca/lineup/health.xml" ) );
        newsFeedsByCategory.put( "arts",
                getNewsChannel( "Topics",  "https://rss.cbc.ca/lineup/arts.xml" ) );
        newsFeedsByCategory.put( "tech",
                getNewsChannel( "Topics",
                         "https://rss.cbc.ca/lineup/technology.xml" ) );
        newsFeedsByCategory.put( "offbeat",
                getNewsChannel( "Topics",
                         "https://rss.cbc.ca/lineup/offbeat.xml" ) );
        newsFeedsByCategory.put( "indigenous",
                getNewsChannel( "Topics",
                         "https://www.cbc.ca/cmlink/rss-cbcaboriginal" ) );
        newsFeedsByCategory.put( "sports",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports.xml" ) );
        newsFeedsByCategory.put( "mlb",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports-mlb.xml" ) );
        newsFeedsByCategory.put( "nba",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports-nba.xml" ) );
        newsFeedsByCategory.put( "cfl",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports-cfl.xml" ) );
        newsFeedsByCategory.put( "nfl",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports-nfl.xml" ) );
        newsFeedsByCategory.put( "nhl",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports-nhl.xml" ) );
        newsFeedsByCategory.put( "soccer",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports-soccer.xml" ) );
        newsFeedsByCategory.put( "curling",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports-curling.xml" ) );
        newsFeedsByCategory.put( "skating",
                getNewsChannel( "Sports",
                         "https://rss.cbc.ca/lineup/sports-figureskating.xml" ) );
        newsFeedsByCategory.put( "bc",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-britishcolumbia.xml" ) );
        newsFeedsByCategory.put( "kamloops",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-kamloops.xml" ) );
        newsFeedsByCategory.put( "calgary",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-calgary.xml" ) );
        newsFeedsByCategory.put( "edmonton",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-edmonton.xml" ) );
        newsFeedsByCategory.put( "saskatchewan",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-saskatchewan.xml" ) );
        newsFeedsByCategory.put( "saskatoon",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-saskatoon.xml" ) );
        newsFeedsByCategory.put( "manitoba",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-manitoba.xml" ) );
        newsFeedsByCategory.put( "thunderbay",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-thunderbay.xml" ) );
        newsFeedsByCategory.put( "sudbury",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-sudbury.xml" ) );
        newsFeedsByCategory.put( "windsor",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-windsor.xml" ) );
        newsFeedsByCategory.put( "london",
                getNewsChannel( "Regional",
                         "https://www.cbc.ca/cmlink/rss-canada-london" ) );
        newsFeedsByCategory.put( "waterloo",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-kitchenerwaterloo.xml" ) );
        newsFeedsByCategory.put( "toronto",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-toronto.xml" ) );
        newsFeedsByCategory.put( "hamilton",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-hamiltonnews.xml" ) );
        newsFeedsByCategory.put( "montreal",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-montreal.xml" ) );
        newsFeedsByCategory.put( "newbrunswick",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-newbrunswick.xml" ) );
        newsFeedsByCategory.put( "pei",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-pei.xml" ) );
        newsFeedsByCategory.put( "novascotia",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-novascotia.xml" ) );
        newsFeedsByCategory.put( "newfoundland",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-newfoundland.xml" ) );
        newsFeedsByCategory.put( "north",
                getNewsChannel( "Regional",
                         "https://rss.cbc.ca/lineup/canada-north.xml" ) );
    }

    @OnMessage("!news ?@category")
    public String getNews(User user, Optional<String> category) {
        return getHeadlines(user, newsLimit, newsFeedsByCategory, category.orElse(DEFAULT_NEWS_CATEGORY));
    }

    @OnMessage("!news help")
    public void newsHelp(User user) {
        for (String helpMessagePart : generateHelpMessage("News Categories (example: !news toronto):", newsFeedsByCategory)
                .split("\n")) {
            user.sendMessage(helpMessagePart);
        }
    }

    @OnMessage("!fakenews ?@category")
    public String getFakeNews(User user, Optional<String> category) {
        return getHeadlines(user, fakeNewsLimit, fakeNewsFeedsByCategory, category.orElse(DEFAULT_FAKE_NEWS_CATEGORY));
    }

    @OnMessage("!fakenews help")
    public void fakeNewsHelp(User user) {
        for (String helpMessagePart : generateHelpMessage("Fake News Categories (example: !fakenews canada):", fakeNewsFeedsByCategory)
                .split("\n")) {
            user.sendMessage(helpMessagePart);
        }
    }

    protected String getHeadlines(User user, LimitService limit, Map<String, NewsChannel> feedsByCategory, String category)
    {
        if (!feedsByCategory.containsKey(category)) {
            return null;
        }
        try {
            limit.addUse(user);
        } catch (LimitExceededException e) {
            return e.getMessage();
        }
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed;

        try
        {
            feed = input.build( new XmlReader( feedsByCategory.get( category ).url ) );
        }
        catch ( FeedException | IOException e )
        {
            e.printStackTrace();
            return  "Error opening RSS feed";
        }

        List<SyndEntry> entries = feed.getEntries();
        return entries.stream()
                .map(entry -> Colors.bold( "  " + entry.getTitle() ) + " -> "
                        + entry.getLink())
                .limit(NUM_HEADLINES)
                .collect(Collectors.joining("\n"));
    }

    protected String generateHelpMessage(String intro, Map<String, NewsChannel> feedsByCategory)
    {
        StringBuilder sb = new StringBuilder();

        sb.append( intro );

        String prevGenre = null;

        for ( String category : feedsByCategory.keySet() )
        {
            String currentGenre = feedsByCategory.get( category ).genre;

            if ( !currentGenre.equals( prevGenre ) )
            {
                sb.append( "\n" );
                sb.append( "  " + currentGenre + ":" );
                prevGenre = currentGenre;
            }

            sb.append( " { " + category + " }" );
        }

        return sb.toString();
    }

    private static class NewsChannel {
        final String genre;
        final URL url;

        public NewsChannel(String genre, URL url) {
            this.genre = genre;
            this.url = url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            NewsChannel that = (NewsChannel) o;
            return genre.equals(that.genre) && url.equals(that.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(genre, url);
        }
    }
}
