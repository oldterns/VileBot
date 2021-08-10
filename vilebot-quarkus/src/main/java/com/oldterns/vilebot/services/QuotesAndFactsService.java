package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.Nick;
import com.oldterns.irc.bot.annotations.OnChannelMessage;
import com.oldterns.irc.bot.annotations.OnMessage;
import com.oldterns.irc.bot.annotations.Regex;
import com.oldterns.vilebot.database.ChurchDB;
import com.oldterns.vilebot.database.QuoteFactDB;
import com.oldterns.vilebot.util.IgnoredUsers;
import com.oldterns.vilebot.util.MangleNicks;
import com.oldterns.vilebot.util.RandomProvider;
import net.engio.mbassy.listener.Handler;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.helper.ActorMessageEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@ApplicationScoped
public class QuotesAndFactsService
{
    @Inject
    QuoteFactDB quoteFactDB;

    @Inject
    ChurchDB churchDB;

    @Inject
    IgnoredUsers ignoredUsers;

    @Inject
    JazizService jazizService;

    @Inject
    RandomProvider randomProvider;

    // cache fact and quote dump links
    private Map<String, String> dumpCache = new HashMap<>();

    // update cache when new quotes/facts added
    private Map<String, Integer> dumpSize = new HashMap<>();

    @ConfigProperty( name = "vilebot.pastebin.api.url" )
    String PASTEBIN_API_URL;

    @Handler
    public String onJoin( ChannelJoinEvent event )
    {
        Nick nick = Nick.getNick( event.getUser() );
        if ( !ignoredUsers.getOnJoin().contains( nick.getBaseNick() ) )
        {
            String out;
            if ( randomProvider.getRandomBoolean() )
            {
                return replyWithFact( nick.getBaseNick(), event, false );
            }
            else
            {
                return replyWithQuote( nick.getBaseNick(), event, false );
            }
        }
        else
        {
            return null;
        }
    }

    @OnChannelMessage( "!quoteadd @nick @text" )
    public String quoteAdd( User senderUser, Nick nick, String text )
    {
        return addQuoteOrFact( senderUser, nick, text, "Quotes", ( noun, quote ) -> {
            quoteFactDB.addQuote( noun, quote );
            return formatQuoteReply( noun, quote );
        } );
    }

    @OnChannelMessage( "!factadd @nick @text" )
    public String factAdd( User senderUser, Nick nick, String text )
    {
        return addQuoteOrFact( senderUser, nick, text, "Facts", ( noun, fact ) -> {
            quoteFactDB.addFact( noun, fact );
            return formatFactReply( noun, fact );
        } );
    }

    private String addQuoteOrFact( User senderUser, Nick nick, String text, String type,
                                   BiFunction<String, String, String> addToDatabaseAndGetReply )
    {
        String noun = nick.getBaseNick();
        String sender = Nick.getNick( senderUser ).getBaseNick();

        if ( !sender.equals( noun ) )
        {
            text = trimChars( text, " '\"" );
            return addToDatabaseAndGetReply.apply( noun, text );
        }
        else
        {
            return type + " from yourself are both terrible and uninteresting.";
        }
    }

    @OnMessage( "!factdump @nick" )
    public String factDump( Nick nick )
    {
        return factQuoteDump( nick, quoteFactDB::getFacts, "fact" );
    }

    @OnMessage( "!quotedump @nick" )
    public String quoteDump( Nick nick )
    {
        return factQuoteDump( nick, quoteFactDB::getQuotes, "quote" );
    }

    String factQuoteDump( Nick queried, Function<String, Set<String>> getData, String type )
    {
        Set<String> allFacts = getData.apply( queried.getBaseNick() );
        if ( allFacts.isEmpty() )
        {
            return queried + " has no " + type + "s.";
        }
        else
        {
            String dumpKey = type + queried;
            String response;
            if ( allFacts.size() != dumpSize.getOrDefault( dumpKey, 0 ) )
            {
                try
                {
                    String title = queried + "'s " + type + "s";
                    String pasteLink = dumpToPastebin( title, allFacts );
                    dumpCache.put( dumpKey, pasteLink );
                    dumpSize.put( dumpKey, allFacts.size() );
                    response = type + "dump for " + queried + ": " + pasteLink;
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    response = "Error creating pastebin link";
                }
            }
            else
            {
                String pasteLink = dumpCache.get( dumpKey );
                response = type + "dump for " + queried + ": " + pasteLink;
            }
            return response;
        }
    }

    private String dumpToPastebin( String title, Set<String> allQuotes )
        throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for ( String quote : allQuotes )
        {
            sb.append( URLEncoder.encode( quote + "\n", StandardCharsets.UTF_8 ) );
        }

        URL url = new URL( PASTEBIN_API_URL );
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput( true );
        conn.setRequestMethod( "POST" );
        conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
        conn.setInstanceFollowRedirects( false );
        String input = "format=text&code2=" + title + "\n\n" + sb.toString() + "&poster=vilebot&paste=Send&expiry=m";

        OutputStream os = conn.getOutputStream();
        os.write( input.getBytes() );
        os.flush();
        assert conn.getResponseCode() == 302;
        return conn.getHeaderField( "Location" );
    }

    @OnMessage( "!factrandom5 @queried" )
    public String factRandomDump( Nick queried )
    {
        return factQuoteRandomDump( "fact", queried.getBaseNick() );
    }

    @OnMessage( "!quoterandom5 @queried" )
    public String quoteRandomDump( Nick queried )
    {
        return factQuoteRandomDump( "quote", queried.getBaseNick() );
    }

    private String factQuoteRandomDump( String mode, String queried )
    {
        if ( "fact".equals( mode ) )
        {
            Long factsLength = quoteFactDB.getFactsLength( queried );
            if ( factsLength == 0 )
            {
                return queried + " has no facts.";
            }
            else if ( factsLength <= 5 )
            {
                return String.join( "\n", quoteFactDB.getFacts( queried ) );
            }
            else
            {
                return String.join( "\n", quoteFactDB.getRandomFacts( queried ) );
            }
        }
        else
        {
            Long quotesLength = quoteFactDB.getQuotesLength( queried );
            if ( quotesLength == 0 )
            {
                return queried + " has no quotes.";
            }
            else if ( quotesLength <= 5 )
            {
                return String.join( "\n", quoteFactDB.getQuotes( queried ) );
            }
            else
            {
                return String.join( "\n", quoteFactDB.getRandomQuotes( queried ) );
            }
        }
    }

    @OnMessage( "!quotenumber @queried" )
    public String quoteNum( Nick queried )
    {
        return factQuoteNum( "quote", queried.getBaseNick() );
    }

    @OnMessage( "!factnumber @queried" )
    public String factNum( Nick queried )
    {
        return factQuoteNum( "fact", queried.getBaseNick() );
    }

    private String factQuoteNum( String mode, String queried )
    {
        if ( "fact".equals( mode ) )
        {
            Long factsLength = quoteFactDB.getFactsLength( queried );
            if ( factsLength == 0 )
            {
                return queried + " has no facts.";
            }
            else
            {
                return queried + " has " + factsLength + " facts.";
            }
        }
        else
        {
            Long quotesLength = quoteFactDB.getQuotesLength( queried );
            if ( quotesLength == 0 )
            {
                return queried + " has no quotes.";
            }
            else
            {
                return queried + " has " + quotesLength + " quotes.";
            }
        }
    }

    @OnMessage( "!fact @nick ?@isJazizify" )
    public String factQuery( ActorMessageEvent<User> event, Nick nick, @Regex( "!jaziz" ) Optional<String> isJazizify )
    {
        return factQuoteQuery( event, "fact", nick.getBaseNick(), isJazizify.isPresent() );
    }

    @OnMessage( "!quote @nick ?@isJazizify" )
    public String quoteQuery( ActorMessageEvent<User> event, Nick nick, @Regex( "!jaziz" ) Optional<String> isJazizify )
    {
        return factQuoteQuery( event, "quote", nick.getBaseNick(), isJazizify.isPresent() );
    }

    private String factQuoteQuery( ActorMessageEvent<User> event, String mode, String noun, boolean jaziz )
    {
        if ( "fact".equals( mode ) )
        {
            String reply = replyWithFact( noun, event, jaziz );
            if ( reply == null )
            {
                return noun + " has no facts.";
            }
            return reply;
        }
        else
        {
            String reply = replyWithQuote( noun, event, jaziz );
            if ( reply == null )
            {
                return noun + " has no quotes.";
            }
            return reply;
        }
    }

    @OnMessage( "!factsearch @nick @regex ?@isJazizify" )
    public String factSearch( ActorMessageEvent<User> event, Nick nick, @Regex( ".*?" ) String regex,
                              @Regex( "!jaziz" ) Optional<String> isJazizify )
    {
        return factQuoteSearch( event, "fact", nick.getBaseNick(), regex, isJazizify.isPresent() );
    }

    @OnMessage( "!quotesearch @nick @regex ?@isJazizify" )
    public String quoteSearch( ActorMessageEvent<User> event, Nick nick, @Regex( ".*?" ) String regex,
                               @Regex( "!jaziz" ) Optional<String> isJazizify )
    {
        return factQuoteSearch( event, "quote", nick.getBaseNick(), regex, isJazizify.isPresent() );
    }

    private String factQuoteSearch( ActorMessageEvent<User> event, String mode, String noun, String regex,
                                    boolean jaziz )
    {
        try
        {
            // Case insensitive added automatically, use (?-i) in a message to re-enable case sensitivity
            Pattern pattern = Pattern.compile( "(?i)" + regex );

            if ( "fact".equals( mode ) )
            {
                Set<String> texts = quoteFactDB.getFacts( noun );
                if ( texts != null )
                {
                    String randomMatch = regexSetSearch( texts, pattern );
                    if ( randomMatch != null )
                    {
                        randomMatch = MangleNicks.mangleNicks( event, randomMatch );
                        if ( jaziz )
                        {
                            try
                            {
                                return formatFactReply( noun, jazizService.jazizify( randomMatch ) );
                            }
                            catch ( Exception e )
                            {
                                e.printStackTrace();
                                return "eeeh";
                            }
                        }
                        else
                        {
                            return formatFactReply( noun, randomMatch );
                        }
                    }
                    else
                    {
                        return noun + " has no matching facts.";
                    }
                }
                else
                {
                    return noun + " has no facts.";
                }
            }
            else
            {
                Set<String> texts = quoteFactDB.getQuotes( noun );
                if ( texts != null )
                {
                    String randomMatch = regexSetSearch( texts, pattern );
                    if ( randomMatch != null )
                    {
                        randomMatch = MangleNicks.mangleNicks( event, randomMatch );
                        if ( jaziz )
                        {
                            try
                            {
                                return formatFactReply( noun, jazizService.jazizify( randomMatch ) );
                            }
                            catch ( Exception e )
                            {
                                e.printStackTrace();
                                return "eeeh";
                            }
                        }
                        else
                        {
                            return formatQuoteReply( noun, randomMatch );
                        }
                    }
                    else
                    {
                        return noun + " has no matching quotes.";
                    }
                }
                else
                {
                    return noun + " has no quotes.";
                }
            }
        }
        catch ( PatternSyntaxException e )
        {
            return "Syntax error in regex pattern";
        }
    }

    private String regexSetSearch( Set<String> texts, Pattern pattern )
    {
        List<String> matchingTexts = new LinkedList<>();

        for ( String text : texts )
        {
            Matcher matcher = pattern.matcher( text );
            if ( matcher.find() )
            {
                matchingTexts.add( text );
            }
        }

        int matchCount = matchingTexts.size();
        if ( matchCount > 0 )
        {
            int selection = randomProvider.getRandomInt( matchCount );
            return matchingTexts.get( selection );
        }
        else
        {
            return null;
        }
    }

    private String replyWithFact( String noun, ActorMessageEvent<User> event, boolean jaziz )
    {
        String replyText = getReplyFact( noun, jaziz );
        if ( replyText != null )
        {
            replyText = MangleNicks.mangleNicks( event, replyText );
            return formatFactReply( getTitle( noun ), replyText );
        }
        else
        {
            return null;
        }
    }

    private String replyWithFact( String noun, ChannelJoinEvent event, boolean jaziz )
    {
        String replyText = getReplyFact( noun, jaziz );
        if ( replyText != null )
        {
            replyText = MangleNicks.mangleNicks( (ChannelEvent) event, replyText );
            return formatFactReply( getTitle( noun ), replyText );
        }
        else
        {
            return null;
        }
    }

    private String getReplyFact( String noun, boolean jaziz )
    {
        String text = quoteFactDB.getRandomFact( noun );
        if ( text == null )
        {
            return null;
        }
        if ( jaziz )
        {
            try
            {
                text = jazizService.jazizify( text );
            }
            catch ( Exception e )
            {
                text = "eeeh";
                e.printStackTrace();
            }
        }
        return text;
    }

    private String formatFactReply( String noun, String fact )
    {
        return noun + " " + fact;
    }

    private String replyWithQuote( String noun, ActorMessageEvent<User> event, boolean jaziz )
    {
        String replyText = getReplyQuote( noun, jaziz );
        if ( replyText != null )
        {
            replyText = MangleNicks.mangleNicks( event, replyText );
            return formatQuoteReply( getTitle( noun ), replyText );
        }
        else
        {
            return null;
        }
    }

    private String replyWithQuote( String noun, ChannelJoinEvent event, boolean jaziz )
    {
        String replyText = getReplyQuote( noun, jaziz );
        if ( replyText != null )
        {
            replyText = MangleNicks.mangleNicks( (ChannelEvent) event, replyText );
            return formatQuoteReply( getTitle( noun ), replyText );
        }
        else
        {
            return null;
        }
    }

    private String getReplyQuote( String noun, boolean jaziz )
    {
        String text = quoteFactDB.getRandomQuote( noun );
        if ( text == null )
        {
            return null;
        }
        if ( jaziz )
        {
            try
            {
                text = jazizService.jazizify( text );
            }
            catch ( Exception e )
            {
                text = "eeeh";
                e.printStackTrace();
            }
        }
        return text;
    }

    private String formatQuoteReply( String noun, String quote )
    {
        return noun + " once said, \"" + quote + "\".";
    }

    private String getTitle( String noun )
    {
        if ( churchDB.isTopDonor( noun ) )
        {
            String title = churchDB.getDonorTitle( noun );
            if ( title.trim().length() > 0 )
            {
                noun = title;
            }
        }
        return noun;
    }

    /**
     * Removes all specified leading and trailing characters in the array charsToRemove.
     *
     * @param input The string to process
     * @param charsToRemove All characters to remove, treated as a set
     * @return A copy of the input String with the characters removed
     */
    private String trimChars( String input, String charsToRemove )
    {
        char[] value = input.toCharArray();
        char[] rmChars = charsToRemove.toCharArray();
        Arrays.sort( rmChars );

        int len = value.length;
        int st = 0;

        while ( ( st < len ) && ( Arrays.binarySearch( rmChars, value[st] ) >= 0 ) )
        {
            st++;
        }
        while ( ( st < len ) && ( Arrays.binarySearch( rmChars, value[len - 1] ) >= 0 ) )
        {
            len--;
        }

        return input.substring( st, len );
    }
}
