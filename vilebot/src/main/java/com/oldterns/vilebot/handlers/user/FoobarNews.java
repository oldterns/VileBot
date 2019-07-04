/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oldterns.vilebot.handlers.user;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.db.QuoteFactDB;
import com.oldterns.vilebot.util.LimitCommand;
import com.oldterns.vilebot.util.NewsParser;
import com.oldterns.vilebot.util.rss.Channel;
import com.oldterns.vilebot.util.rss.Item;
import com.oldterns.vilebot.util.rss.RSS;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoobarNews
    extends NewsParser
{
    private static final Pattern FOOBAR_NEWS_PATTERN = Pattern.compile( "^!foobarnews(?: ([a-zA-Z]+)|)" );

    private static final Pattern FOOBAR_NEWS_HELP_PATTERN = Pattern.compile( "^!foobarnews help" );

    private static final Logger logger = LoggerFactory.getLogger( FoobarNews.class );

    private final String HELP_MESSAGE = generateHelpMessage();

    private final String HELP_COMMAND = "'!news help'";

    public static LimitCommand limitCommand = new LimitCommand( 100, 1 );

    private static final String RESTRICTED_CHANNEL = Vilebot.getConfig().get( "ircChannel1" );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = FOOBAR_NEWS_PATTERN.matcher( text );
        Matcher helpMatcher = FOOBAR_NEWS_HELP_PATTERN.matcher( text );

        if ( helpMatcher.matches() )
        {
            for ( String line : HELP_MESSAGE.split( "\n" ) )
            {
                event.respondPrivateMessage( line );
            }
        }
        else if ( matcher.matches() )
        {
            String category = matcher.group( 1 );
            if ( category != null && category.equals( "foobar" ) )
            {
                category = null;
            }
            else if ( category != null && !QuoteFactDB.getQuotableFactableNicks().contains( category ) )
            {
                event.respond( category + " is not a nick that has facts or quotes." );
                return;
            }
            HashMap<String, URL> newsFeedsByCategory = new HashMap<>();
            try
            {
                Path newsFile = Files.createTempFile( "FoobarNews", ".xml" );
                logger.info( "Foobar news temp file: " + newsFile.toString() );

                RSS rssFeed = createRssFeed( category );
                JAXBContext context = JAXBContext.newInstance( RSS.class );
                Marshaller marshaller = context.createMarshaller();
                try ( FileOutputStream fileOutputStream = new FileOutputStream( newsFile.toFile() ) )
                {
                    marshaller.marshal( rssFeed, fileOutputStream );
                    URL newsFileURL = newsFile.toUri().toURL();
                    category = ( category != null ) ? category.toLowerCase() : "foobar";

                    newsFeedsByCategory.put( category, newsFileURL );
                    logger.info( newsFileURL.toString() );
                    currentNews( event, matcher, newsFeedsByCategory, category, HELP_COMMAND, limitCommand,
                                 RESTRICTED_CHANNEL, logger );
                }
                catch ( IOException e )
                {
                    throw e;
                }
            }
            catch ( IOException | JAXBException e )
            {
                event.respond( "I could not create temporary file for the news :(" );
            }
        }
    }

    private RSS createRssFeed( String topic )
    {
        final int ITEM_COUNT = 3;
        final String VERSION = "2.0";

        RSS out = new RSS();
        out.version = VERSION;

        Channel channel = new Channel();
        channel.title = "The Foobar News";
        channel.link = "http://localhost";
        channel.description = "The latest in #thefoobar";
        channel.items = new ArrayList<>();

        Markov markov = new Markov();
        if ( topic != null )
        {
            markov.trainOnNick( topic );
        }
        else
        {
            markov.train();
        }

        for ( int i = 0; i < ITEM_COUNT; i++ )
        {
            Item item = new Item();
            item.title = markov.generatePhrase( 50 );
            item.link = "http://localhost";
            item.description = "";
            channel.items.add( item );
        }

        out.channel = channel;
        return out;
    }

    @Override
    protected String generateHelpMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Foobar News Categories (example: !foobarnews foobar):" );
        sb.append( "\n" );

        sb.append( " { foobar }" );
        for ( String nick : QuoteFactDB.getQuotableFactableNicks() )
        {
            sb.append( " { " + nick + " }" );
        }

        return sb.toString();
    }

}
