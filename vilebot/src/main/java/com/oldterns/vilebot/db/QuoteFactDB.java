/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import java.util.Set;
import java.util.LinkedList;
import java.util.List;

import redis.clients.jedis.Jedis;

public class QuoteFactDB
    extends RedisDB
{
    private static final String keyOfQuoteSetsPrefix = "noun-quotes-";

    private static final String keyOfFactSetsPrefix = "noun-facts-";

    private static final int COUNT = 5;

    /**
     * Add a quote to the quote set of a noun.
     *
     * @param noun The noun to add a quote to
     * @param quote The new quote
     */
    public static void addQuote( String noun, String quote )
    {
        Jedis jedis = pool.getResource();
        try
        {
            jedis.sadd( keyOfQuoteSetsPrefix + noun, quote );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get the quotes of a noun.
     *
     * @param noun The noun to get the quotes of
     * @return The set of quotes for the noun
     */
    public static Set<String> getQuotes( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.smembers( keyOfQuoteSetsPrefix + noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get a random quote from the set of quotes of a noun.
     *
     * @param noun The noun to get the quotes of
     * @return A random quote for the noun
     */
    public static String getRandQuote( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.srandmember( keyOfQuoteSetsPrefix + noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get the number of quotes associated with a noun.
     *
     * @param noun The noun to get the quotes of
     * @return The number of quotes
     */
    public static Long getQuotesLength( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.scard( keyOfQuoteSetsPrefix + noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get 5 random quotes from the set of facts of a noun.
     *
     * @param noun The noun to get the quotes of
     * @return A List of 5 random quotes for the noun
     */
    public static List<String> getRandQuotes( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            List<String> randomQuotes = new LinkedList<String>();
            for ( int i = 0; i < COUNT; i++ )
            {
                String quote = getRandQuote( noun );
                randomQuotes.add( quote );
            }
            return randomQuotes;
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Add a fact to the fact set of a noun.
     *
     * @param noun The noun to add the fact to
     * @param quote The new quote
     */
    public static void addFact( String noun, String fact )
    {
        Jedis jedis = pool.getResource();
        try
        {
            jedis.sadd( keyOfFactSetsPrefix + noun, fact );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get the facts of a noun.
     *
     * @param noun The noun to get the facts of
     * @return The set of facts for the noun
     */
    public static Set<String> getFacts( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.smembers( keyOfFactSetsPrefix + noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get a random fact from the set of facts of a noun.
     *
     * @param noun The noun to get the facts of
     * @return A random fact for the noun
     */
    public static String getRandFact( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.srandmember( keyOfFactSetsPrefix + noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get the number of facts associated with a noun.
     *
     * @param noun The noun to get the facts of
     * @return The number of facts
     */
    public static Long getFactsLength( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.scard( keyOfFactSetsPrefix + noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get 5 random facts from the set of facts of a noun.
     *
     * @param noun The noun to get the facts of
     * @return A List of 5 random facts for the noun
     */
    public static List<String> getRandFacts( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            List<String> randomFacts = new LinkedList<String>();
            for ( int i = 0; i < COUNT; i++ )
            {
                String fact = getRandFact( noun );
                randomFacts.add( fact );
            }
            return randomFacts;
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }
}
