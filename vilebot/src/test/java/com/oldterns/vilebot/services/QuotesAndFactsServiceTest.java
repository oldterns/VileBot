package com.oldterns.vilebot.services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import com.oldterns.irc.bot.Nick;
import com.oldterns.vilebot.database.ChurchDB;
import com.oldterns.vilebot.database.QuoteFactDB;
import com.oldterns.vilebot.util.IgnoredUsers;
import com.oldterns.vilebot.util.RandomProvider;
import com.oldterns.vilebot.util.TestUrlStreamHandler;
import com.oldterns.vilebot.util.URLFactory;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
public class QuotesAndFactsServiceTest
{

    @Inject
    QuotesAndFactsService quotesAndFactsService;

    @Inject
    TestUrlStreamHandler urlMocker;

    @InjectSpy
    URLFactory urlFactory;

    @ConfigProperty( name = "vilebot.pastebin.api.url" )
    String PASTEBIN_API_URL;

    @InjectMock
    QuoteFactDB quoteFactDB;

    @InjectMock
    ChurchDB churchDB;

    @InjectMock
    IgnoredUsers ignoredUsers;

    @InjectMock
    JazizService jazizService;

    @InjectMock
    RandomProvider randomProvider;

    @Test
    public void testIgnoreIngoredUsersOnJoin()
    {
        Client client = Mockito.mock( Client.class );
        ServerMessage sourceMessage = Mockito.mock( ServerMessage.class );
        Channel channel = Mockito.mock( Channel.class );
        User user = Mockito.mock( User.class );

        when( user.getNick() ).thenReturn( "ignored" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        when( ignoredUsers.getOnJoin() ).thenReturn( Set.of( "ignored" ) );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );
        assertThat( quotesAndFactsService.onJoin( channelJoinEvent ) ).isNull();
    }

    @Test
    public void testQuoteOnJoin()
    {
        Client client = Mockito.mock( Client.class );
        ServerMessage sourceMessage = Mockito.mock( ServerMessage.class );
        Channel channel = Mockito.mock( Channel.class );
        User user = Mockito.mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        when( randomProvider.getRandomBoolean() ).thenReturn( false );
        when( quoteFactDB.getRandomQuote( "bob" ) ).thenReturn( "this is a quote" );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );
        assertThat( quotesAndFactsService.onJoin( channelJoinEvent ) ).isEqualTo( "bob once said, \"this is a quote\"." );
    }

    @Test
    public void testFactOnJoin()
    {
        Client client = Mockito.mock( Client.class );
        ServerMessage sourceMessage = Mockito.mock( ServerMessage.class );
        Channel channel = Mockito.mock( Channel.class );
        User user = Mockito.mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        when( randomProvider.getRandomBoolean() ).thenReturn( true );
        when( quoteFactDB.getRandomFact( "bob" ) ).thenReturn( "this is a fact" );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );
        assertThat( quotesAndFactsService.onJoin( channelJoinEvent ) ).isEqualTo( "bob this is a fact" );
    }

    @Test
    public void testTopDonorQuoteOnJoin()
    {
        Client client = Mockito.mock( Client.class );
        ServerMessage sourceMessage = Mockito.mock( ServerMessage.class );
        Channel channel = Mockito.mock( Channel.class );
        User user = Mockito.mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        when( randomProvider.getRandomBoolean() ).thenReturn( false );
        when( quoteFactDB.getRandomQuote( "bob" ) ).thenReturn( "this is a quote" );
        when( churchDB.isTopDonor( "bob" ) ).thenReturn( true );
        when( churchDB.getDonorTitle( "bob" ) ).thenReturn( "king bob" );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );
        assertThat( quotesAndFactsService.onJoin( channelJoinEvent ) ).isEqualTo( "king bob once said, \"this is a quote\"." );
    }

    @Test
    public void testTopDonorFactOnJoin()
    {
        Client client = Mockito.mock( Client.class );
        ServerMessage sourceMessage = Mockito.mock( ServerMessage.class );
        Channel channel = Mockito.mock( Channel.class );
        User user = Mockito.mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        when( randomProvider.getRandomBoolean() ).thenReturn( true );
        when( quoteFactDB.getRandomFact( "bob" ) ).thenReturn( "this is a fact" );
        when( churchDB.isTopDonor( "bob" ) ).thenReturn( true );
        when( churchDB.getDonorTitle( "bob" ) ).thenReturn( "king bob" );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );
        assertThat( quotesAndFactsService.onJoin( channelJoinEvent ) ).isEqualTo( "king bob this is a fact" );
    }

    @Test
    public void testQuoteWithNoQuotesOnJoin()
    {
        Client client = Mockito.mock( Client.class );
        ServerMessage sourceMessage = Mockito.mock( ServerMessage.class );
        Channel channel = Mockito.mock( Channel.class );
        User user = Mockito.mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        when( randomProvider.getRandomBoolean() ).thenReturn( false );
        when( quoteFactDB.getRandomQuote( "bob" ) ).thenReturn( null );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );
        assertThat( quotesAndFactsService.onJoin( channelJoinEvent ) ).isEqualTo( null );
    }

    @Test
    public void testFactWithNoFactsOnJoin()
    {
        Client client = Mockito.mock( Client.class );
        ServerMessage sourceMessage = Mockito.mock( ServerMessage.class );
        Channel channel = Mockito.mock( Channel.class );
        User user = Mockito.mock( User.class );

        when( user.getNick() ).thenReturn( "bob" );
        when( user.getClient() ).thenReturn( client );
        when( channel.getClient() ).thenReturn( client );

        when( randomProvider.getRandomBoolean() ).thenReturn( true );
        when( quoteFactDB.getRandomFact( "bob" ) ).thenReturn( null );

        ChannelJoinEvent channelJoinEvent = new ChannelJoinEvent( client, sourceMessage, channel, user );
        assertThat( quotesAndFactsService.onJoin( channelJoinEvent ) ).isEqualTo( null );
    }

    @Test
    public void testQuoteAdd()
    {
        User user = Mockito.mock( User.class );
        when( user.getNick() ).thenReturn( "alice" );
        Nick nick = Nick.valueOf( "bob" );

        assertThat( quotesAndFactsService.quoteAdd( user, nick,
                                                    "it a wonderful day today" ) ).isEqualTo( "bob once said, \"it a wonderful day today\"." );
        verify( quoteFactDB ).addQuote( nick.getBaseNick(), "it a wonderful day today" );
    }

    @Test
    public void testFactAdd()
    {
        User user = Mockito.mock( User.class );
        when( user.getNick() ).thenReturn( "alice" );
        Nick nick = Nick.valueOf( "bob" );

        assertThat( quotesAndFactsService.factAdd( user, nick, "is bob" ) ).isEqualTo( "bob is bob" );
        verify( quoteFactDB ).addFact( nick.getBaseNick(), "is bob" );
    }

    @Test
    public void testSelfQuoteAdd()
    {
        User user = Mockito.mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        Nick nick = Nick.valueOf( "bob" );

        assertThat( quotesAndFactsService.quoteAdd( user, nick,
                                                    "it a wonderful day today" ) ).isEqualTo( "Quotes from yourself are both terrible and uninteresting." );
        verifyNoInteractions( quoteFactDB );
    }

    @Test
    public void testSelfFactAdd()
    {
        User user = Mockito.mock( User.class );
        when( user.getNick() ).thenReturn( "bob" );
        Nick nick = Nick.valueOf( "bob" );

        assertThat( quotesAndFactsService.factAdd( user, nick,
                                                   "it a wonderful day today" ) ).isEqualTo( "Facts from yourself are both terrible and uninteresting." );
        verifyNoInteractions( quoteFactDB );
    }

    @Test
    public void testEmptyQuoteDump()
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getQuotes( "bob" ) ).thenReturn( Collections.emptySet() );

        assertThat( quotesAndFactsService.quoteDump( nick ) ).isEqualTo( "bob has no quotes." );
    }

    @Test
    public void testEmptyFactDump()
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getFacts( "bob" ) ).thenReturn( Collections.emptySet() );

        assertThat( quotesAndFactsService.factDump( nick ) ).isEqualTo( "bob has no facts." );
    }

    @Test
    public void testQuoteDump()
        throws IOException
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getQuotes( "bob" ) ).thenReturn( new LinkedHashSet<>( List.of( "quote 1", "quote 2",
                                                                                         "quote 3" ) ) );
        urlMocker.mockHttpConnection( "", PASTEBIN_API_URL, null, 302 );
        HttpURLConnection connection = urlMocker.getConnection( PASTEBIN_API_URL );

        OutputStream outputStream = Mockito.mock( OutputStream.class );
        when( connection.getOutputStream() ).thenReturn( outputStream );
        when( connection.getHeaderField( "Location" ) ).thenReturn( "quote-pastebin-link" );
        assertThat( quotesAndFactsService.quoteDump( nick ) ).isEqualTo( "quotedump for bob: quote-pastebin-link" );
        String input =
            "format=text&code2=" + "bob's quotes" + "\n\n" + URLEncoder.encode( "quote 1\n", StandardCharsets.UTF_8 )
                + URLEncoder.encode( "quote 2\n", StandardCharsets.UTF_8 )
                + URLEncoder.encode( "quote 3\n", StandardCharsets.UTF_8 ) + "&poster=vilebot&paste=Send&expiry=m";
        ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass( byte[].class );
        verify( outputStream ).write( argumentCaptor.capture() );
        assertThat( new String( argumentCaptor.getValue() ) ).isEqualTo( input );
        verify( outputStream ).flush();

        assertThat( quotesAndFactsService.quoteDump( nick ) ).isEqualTo( "quotedump for bob: quote-pastebin-link" );
        verify( urlFactory, Mockito.times( 1 ) ).build( PASTEBIN_API_URL );
    }

    @Test
    public void testFactDump()
        throws IOException
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getFacts( "bob" ) ).thenReturn( new LinkedHashSet<>( List.of( "fact 1", "fact 2",
                                                                                        "fact 3" ) ) );
        urlMocker.mockHttpConnection( "", PASTEBIN_API_URL, null, 302 );
        HttpURLConnection connection = urlMocker.getConnection( PASTEBIN_API_URL );

        OutputStream outputStream = Mockito.mock( OutputStream.class );
        when( connection.getOutputStream() ).thenReturn( outputStream );
        when( connection.getHeaderField( "Location" ) ).thenReturn( "fact-pastebin-link" );
        assertThat( quotesAndFactsService.factDump( nick ) ).isEqualTo( "factdump for bob: fact-pastebin-link" );
        String input =
            "format=text&code2=" + "bob's facts" + "\n\n" + URLEncoder.encode( "fact 1\n", StandardCharsets.UTF_8 )
                + URLEncoder.encode( "fact 2\n", StandardCharsets.UTF_8 )
                + URLEncoder.encode( "fact 3\n", StandardCharsets.UTF_8 ) + "&poster=vilebot&paste=Send&expiry=m";
        ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass( byte[].class );
        verify( outputStream ).write( argumentCaptor.capture() );
        assertThat( new String( argumentCaptor.getValue() ) ).isEqualTo( input );
        verify( outputStream ).flush();

        assertThat( quotesAndFactsService.factDump( nick ) ).isEqualTo( "factdump for bob: fact-pastebin-link" );
        verify( urlFactory, Mockito.times( 1 ) ).build( PASTEBIN_API_URL );
    }

    @Test
    public void testRandomQuotesNoQuotes()
    {
        User user = Mockito.mock( User.class );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getQuotesLength( "bob" ) ).thenReturn( 0L );

        quotesAndFactsService.quoteRandomDump( user, nick );
        verify( user ).sendMessage( "bob has no quotes." );
    }

    @Test
    public void testRandomFactsNoFacts()
    {
        User user = Mockito.mock( User.class );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getFactsLength( "bob" ) ).thenReturn( 0L );

        quotesAndFactsService.factRandomDump( user, nick );
        verify( user ).sendMessage( "bob has no facts." );
    }

    @Test
    public void testRandomQuotesLessThan5Quotes()
    {
        User user = Mockito.mock( User.class );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getQuotesLength( "bob" ) ).thenReturn( 3L );
        when( quoteFactDB.getQuotes( "bob" ) ).thenReturn( new LinkedHashSet<>( List.of( "quote 1", "quote 2",
                                                                                         "quote 3" ) ) );

        quotesAndFactsService.quoteRandomDump( user, nick );
        verify( user ).sendMessage( "bob once said, \"quote 1\"." );
        verify( user ).sendMessage( "bob once said, \"quote 2\"." );
        verify( user ).sendMessage( "bob once said, \"quote 3\"." );
    }

    @Test
    public void testRandomFactsLessThan5Facts()
    {
        User user = Mockito.mock( User.class );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getFactsLength( "bob" ) ).thenReturn( 3L );
        when( quoteFactDB.getFacts( "bob" ) ).thenReturn( new LinkedHashSet<>( List.of( "fact 1", "fact 2",
                                                                                        "fact 3" ) ) );

        quotesAndFactsService.factRandomDump( user, nick );
        verify( user ).sendMessage( "bob fact 1" );
        verify( user ).sendMessage( "bob fact 2" );
        verify( user ).sendMessage( "bob fact 3" );
    }

    @Test
    public void testRandomQuotes6Quotes()
    {
        User user = Mockito.mock( User.class );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getQuotesLength( "bob" ) ).thenReturn( 6L );
        when( quoteFactDB.getRandomQuotes( "bob" ) ).thenReturn( List.of( "quote 1", "quote 2", "quote 3", "quote 4",
                                                                          "quote 5" ) );

        quotesAndFactsService.quoteRandomDump( user, nick );
        verify( user ).sendMessage( "bob once said, \"quote 1\"." );
        verify( user ).sendMessage( "bob once said, \"quote 2\"." );
        verify( user ).sendMessage( "bob once said, \"quote 3\"." );
        verify( user ).sendMessage( "bob once said, \"quote 4\"." );
        verify( user ).sendMessage( "bob once said, \"quote 5\"." );
    }

    @Test
    public void testRandomFacts6Facts()
    {
        User user = Mockito.mock( User.class );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getFactsLength( "bob" ) ).thenReturn( 6L );
        when( quoteFactDB.getRandomFacts( "bob" ) ).thenReturn( List.of( "fact 1", "fact 2", "fact 3", "fact 4",
                                                                         "fact 5" ) );

        quotesAndFactsService.factRandomDump( user, nick );
        verify( user ).sendMessage( "bob fact 1" );
        verify( user ).sendMessage( "bob fact 2" );
        verify( user ).sendMessage( "bob fact 3" );
        verify( user ).sendMessage( "bob fact 4" );
        verify( user ).sendMessage( "bob fact 5" );
    }

    @Test
    public void testQuoteNumNoQuotes()
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getQuotesLength( "bob" ) ).thenReturn( 0L );

        assertThat( quotesAndFactsService.quoteNum( nick ) ).isEqualTo( "bob has no quotes." );
    }

    @Test
    public void testFactNumNoFacts()
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getFactsLength( "bob" ) ).thenReturn( 0L );

        assertThat( quotesAndFactsService.factNum( nick ) ).isEqualTo( "bob has no facts." );
    }

    @Test
    public void testQuoteNumSomeQuotes()
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getQuotesLength( "bob" ) ).thenReturn( 3L );

        assertThat( quotesAndFactsService.quoteNum( nick ) ).isEqualTo( "bob has 3 quotes." );
    }

    @Test
    public void testFactNumSomeFacts()
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getFactsLength( "bob" ) ).thenReturn( 3L );

        assertThat( quotesAndFactsService.factNum( nick ) ).isEqualTo( "bob has 3 facts." );
    }

    @Test
    public void testQuoteQueryNoQuotes()
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getRandomQuote( "bob" ) ).thenReturn( null );

        assertThat( quotesAndFactsService.quoteQuery( null, nick,
                                                      Optional.empty() ) ).isEqualTo( "bob has no quotes." );
    }

    @Test
    public void testFactQueryNoFacts()
    {
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getRandomFact( "bob" ) ).thenReturn( null );

        assertThat( quotesAndFactsService.factQuery( null, nick, Optional.empty() ) ).isEqualTo( "bob has no facts." );
    }

    @Test
    public void testQuoteQuery()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );

        User alice = mock( User.class );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        User bob = mock( User.class );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );

        when( channel.getUsers() ).thenReturn( List.of( alice, bob ) );
        when( channel.getClient() ).thenReturn( client );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getRandomQuote( "bob" ) ).thenReturn( "hi alice!" );

        ServerMessage sourceMessage = mock( ServerMessage.class );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!quote bob" );
        assertThat( quotesAndFactsService.quoteQuery( channelMessageEvent, nick,
                                                      Optional.empty() ) ).isEqualTo( "bob once said, \"hi !ecila\"." );
    }

    @Test
    public void testFactQuery()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );

        User alice = mock( User.class );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        User bob = mock( User.class );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );

        when( channel.getUsers() ).thenReturn( List.of( alice, bob ) );
        when( channel.getClient() ).thenReturn( client );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getRandomFact( "bob" ) ).thenReturn( "is also alice" );

        ServerMessage sourceMessage = mock( ServerMessage.class );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!fact bob" );
        assertThat( quotesAndFactsService.factQuery( channelMessageEvent, nick,
                                                     Optional.empty() ) ).isEqualTo( "bob is also ecila" );
    }

    @Test
    public void testQuoteJazizQuery()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );

        User alice = mock( User.class );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        User bob = mock( User.class );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );

        when( channel.getUsers() ).thenReturn( List.of( alice, bob ) );
        when( channel.getClient() ).thenReturn( client );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getRandomQuote( "bob" ) ).thenReturn( "hi alice!" );
        when( jazizService.jazizify( "hi alice!" ) ).thenReturn( "hey alice!" );

        ServerMessage sourceMessage = mock( ServerMessage.class );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!quote bob" );
        assertThat( quotesAndFactsService.quoteQuery( channelMessageEvent, nick,
                                                      Optional.of( "jaziz" ) ) ).isEqualTo( "bob once said, \"hey !ecila\"." );
    }

    @Test
    public void testFactJazizQuery()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );

        User alice = mock( User.class );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        User bob = mock( User.class );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );

        when( channel.getUsers() ).thenReturn( List.of( alice, bob ) );
        when( channel.getClient() ).thenReturn( client );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getRandomFact( "bob" ) ).thenReturn( "is alice" );
        when( jazizService.jazizify( "is alice" ) ).thenReturn( "actually is alice" );

        ServerMessage sourceMessage = mock( ServerMessage.class );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!quote bob" );
        assertThat( quotesAndFactsService.factQuery( channelMessageEvent, nick,
                                                     Optional.of( "jaziz" ) ) ).isEqualTo( "bob actually is ecila" );
    }

    @Test
    public void testQuoteSearch()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );

        User alice = mock( User.class );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        User bob = mock( User.class );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );

        when( channel.getUsers() ).thenReturn( List.of( alice, bob ) );
        when( channel.getClient() ).thenReturn( client );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getQuotes( "bob" ) ).thenReturn( new LinkedHashSet<>( List.of( "alice 1", "quote 2",
                                                                                         "alice 3" ) ) );

        ServerMessage sourceMessage = mock( ServerMessage.class );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!quotesearch bob alice" );

        when( randomProvider.getRandomInt( 2 ) ).thenReturn( 0 );
        assertThat( quotesAndFactsService.quoteSearch( channelMessageEvent, nick, "alice",
                                                       Optional.empty() ) ).isEqualTo( "bob once said, \"ecila 1\"." );

        when( randomProvider.getRandomInt( 2 ) ).thenReturn( 1 );
        assertThat( quotesAndFactsService.quoteSearch( channelMessageEvent, nick, "alice",
                                                       Optional.empty() ) ).isEqualTo( "bob once said, \"ecila 3\"." );
        assertThat( quotesAndFactsService.quoteSearch( channelMessageEvent, nick, "404",
                                                       Optional.empty() ) ).isEqualTo( "bob has no matching quotes." );
    }

    @Test
    public void testFactSearch()
    {
        Client client = mock( Client.class );
        Channel channel = mock( Channel.class );

        User alice = mock( User.class );
        when( alice.getNick() ).thenReturn( "alice" );
        when( alice.getClient() ).thenReturn( client );
        User bob = mock( User.class );
        when( bob.getNick() ).thenReturn( "bob" );
        when( bob.getClient() ).thenReturn( client );

        when( channel.getUsers() ).thenReturn( List.of( alice, bob ) );
        when( channel.getClient() ).thenReturn( client );
        Nick nick = Nick.valueOf( "bob" );
        when( quoteFactDB.getFacts( "bob" ) ).thenReturn( new LinkedHashSet<>( List.of( "alice 1", "fact 2",
                                                                                        "alice 3" ) ) );

        ServerMessage sourceMessage = mock( ServerMessage.class );

        ChannelMessageEvent channelMessageEvent =
            new ChannelMessageEvent( client, sourceMessage, alice, channel, "!factsearch bob alice" );

        when( randomProvider.getRandomInt( 2 ) ).thenReturn( 0 );
        assertThat( quotesAndFactsService.factSearch( channelMessageEvent, nick, "alice",
                                                      Optional.empty() ) ).isEqualTo( "bob ecila 1" );

        when( randomProvider.getRandomInt( 2 ) ).thenReturn( 1 );
        assertThat( quotesAndFactsService.factSearch( channelMessageEvent, nick, "alice",
                                                      Optional.empty() ) ).isEqualTo( "bob ecila 3" );
        assertThat( quotesAndFactsService.factSearch( channelMessageEvent, nick, "404",
                                                      Optional.empty() ) ).isEqualTo( "bob has no matching facts." );
    }
}
