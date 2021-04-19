/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.database;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class QuoteFactDB
{
    private static final String keyOfQuoteSetsPrefix = "noun-quotes-";

    private static final String keyOfFactSetsPrefix = "noun-facts-";

    private static final int COUNT = 5;

    @Inject
    RedisClient redisClient;

    /**
     * Add a quote to the quote set of a noun.
     *
     * @param noun The noun to add a quote to
     * @param quote The new quote
     */
    public void addQuote( String noun, String quote )
    {
        redisClient.sadd( List.of( keyOfQuoteSetsPrefix + noun, quote ) );
    }

    /**
     * Get the quotes of a noun.
     *
     * @param noun The noun to get the quotes of
     * @return The set of quotes for the noun
     */
    public Set<String> getQuotes( String noun )
    {
        return redisClient.smembers( keyOfQuoteSetsPrefix
            + noun ).stream().map( Response::toString ).collect( Collectors.toSet() );
    }

    /**
     * Get a random quote from the set of quotes of a noun.
     *
     * @param noun The noun to get the quotes of
     * @return A random quote for the noun
     */
    public String getRandomQuote( String noun )
    {
        return redisClient.srandmember( List.of( keyOfQuoteSetsPrefix + noun ) ).toString();
    }

    /**
     * Get the number of quotes associated with a noun.
     *
     * @param noun The noun to get the quotes of
     * @return The number of quotes
     */
    public Long getQuotesLength( String noun )
    {
        return redisClient.scard( keyOfQuoteSetsPrefix + noun ).toLong();
    }

    /**
     * Get 5 random quotes from the set of facts of a noun.
     *
     * @param noun The noun to get the quotes of
     * @return A List of 5 random quotes for the noun
     */
    public List<String> getRandomQuotes( String noun )
    {
        List<String> randomQuotes = new LinkedList<String>();

        // TODO: This implementation is wrong; doesn't handle
        // duplicate quotes or when quotes have less than 5 members
        for ( int i = 0; i < COUNT; i++ )
        {
            String quote = getRandomQuote( noun );
            randomQuotes.add( quote );
        }
        return randomQuotes;
    }

    /**
     * Add a fact to the fact set of a noun.
     *
     * @param noun The noun to add the fact to
     * @param quote The new quote
     */
    public void addFact( String noun, String quote )
    {
        redisClient.sadd( List.of( keyOfFactSetsPrefix + noun, quote ) );
    }

    /**
     * Get the facts of a noun.
     *
     * @param noun The noun to get the facts of
     * @return The set of facts for the noun
     */
    public Set<String> getFacts( String noun )
    {
        return redisClient.smembers( keyOfFactSetsPrefix
            + noun ).stream().map( Response::toString ).collect( Collectors.toSet() );
    }

    /**
     * Get a random fact from the set of facts of a noun.
     *
     * @param noun The noun to get the facts of
     * @return A random fact for the noun
     */
    public String getRandomFact( String noun )
    {
        return redisClient.srandmember( List.of( keyOfFactSetsPrefix + noun ) ).toString();
    }

    /**
     * Get the number of facts associated with a noun.
     *
     * @param noun The noun to get the facts of
     * @return The number of facts
     */
    public Long getFactsLength( String noun )
    {
        return redisClient.scard( keyOfFactSetsPrefix + noun ).toLong();
    }

    /**
     * Get 5 random facts from the set of facts of a noun.
     *
     * @param noun The noun to get the facts of
     * @return A List of 5 random facts for the noun
     */
    public List<String> getRandomFacts( String noun )
    {
        List<String> randomFact = new LinkedList<String>();

        // TODO: This implementation is wrong; doesn't handle
        // duplicate facts or when quotes have less than 5 members
        for ( int i = 0; i < COUNT; i++ )
        {
            String quote = getRandomFact( noun );
            randomFact.add( quote );
        }
        return randomFact;
    }
}
