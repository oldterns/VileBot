/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import java.util.Set;

import redis.clients.jedis.Jedis;

public class QuoteFactDB
    extends RedisDB
{
    private static final String keyOfQuoteSetsPrefix = "noun-quotes-";

    private static final String keyOfFactSetsPrefix = "noun-facts-";

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
}
