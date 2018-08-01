/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.db;

import java.util.Set;

import redis.clients.jedis.Jedis;

public class KarmaDB
    extends RedisDB
{
    private static final String keyOfKarmaSortedSet = "noun-karma";

    /**
     * Change the karma of a noun by an integer.
     * 
     * @param noun The noun to change the karma of
     * @param mod The amount to change the karma by, may be negative.
     */
    public static void modNounKarma( String noun, int mod )
    {
        Jedis jedis = pool.getResource();
        try
        {
            jedis.zincrby( keyOfKarmaSortedSet, mod, noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Get the karma of a noun.
     * 
     * @param noun The noun to query to karma of
     * @return Integer iff the noun has a defined value, else null
     */
    public static Integer getNounKarma( String noun )
    {
        Jedis jedis = pool.getResource();
        Double karma;
        try
        {
            karma = jedis.zscore( keyOfKarmaSortedSet, noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }

        if ( karma == null )
        {
            return null;
        }

        return Integer.valueOf( Long.valueOf( Math.round( karma ) ).intValue() );
    }

    /**
     * Get the rank of a noun based on its karma.
     * 
     * @param noun The noun to query the rank of
     * @return Integer iff the noun has a defined value, else null
     */
    public static Integer getNounRank( String noun )
    {
        Jedis jedis = pool.getResource();
        Long rank;
        try
        {
            rank = jedis.zrevrank( keyOfKarmaSortedSet, noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }

        if ( rank == null )
        {
            return null;
        }

        return Integer.valueOf( rank.intValue() + 1 );
    }

    /**
     * Get the rank of a noun based on its karma, starting at most negative karma.
     * 
     * @param noun The noun to query the reverse rank of
     * @return Integer iff the noun has a defined value, else null
     */
    public static Integer getNounRevRank( String noun )
    {
        Jedis jedis = pool.getResource();
        Long rank;
        try
        {
            rank = jedis.zrank( keyOfKarmaSortedSet, noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }

        if ( rank == null )
        {
            return null;
        }

        return Integer.valueOf( rank.intValue() + 1 );
    }

    /**
     * Get noun from a karma rank (Rank 1 is the member with the highest karma).
     * 
     * @param rank The rank to get the noun of.
     * @return String The noun iff the rank exists, else null.
     */
    public static String getRankNoun( long rank )
    {
        Set<String> nouns = getRankNouns( rank - 1, rank );

        if ( nouns != null && nouns.iterator().hasNext() )
        {
            return nouns.iterator().next();
        }
        return null;
    }

    /**
     * Get nouns from karma ranks.
     * 
     * @param lower The lower rank to get the nouns of.
     * @param upper The upper rank to get the nouns of.
     * @return String The noun iff the rank exists, else null.
     */
    public static Set<String> getRankNouns( long lower, long upper )
    {
        Set<String> nouns;

        Jedis jedis = pool.getResource();
        try
        {
            nouns = jedis.zrevrange( keyOfKarmaSortedSet, lower, upper );
        }
        finally
        {
            pool.returnResource( jedis );
        }

        if ( nouns == null || nouns.size() == 0 )
        {
            return null;
        }

        return nouns;
    }

    /**
     * Get noun from a karma rank, starting with the lowest ranks (Rank 1 would be the member with the least karma).
     * 
     * @param rank The reversed rank to get the noun of.
     * @return String The noun iff the rank exists, else null.
     */
    public static String getRevRankNoun( long rank )
    {
        Set<String> nouns = getRevRankNouns( rank - 1, rank );

        if ( nouns != null && nouns.iterator().hasNext() )
        {
            return nouns.iterator().next();
        }
        return null;
    }

    /**
     * Get nouns from a karma rank, starting with the lowest ranks.
     * 
     * @param lower The lower rank to get the nouns of.
     * @param upper The upper rank to get the nouns of.
     * @return String The noun iff the rank exists, else null.
     */
    public static Set<String> getRevRankNouns( long lower, long upper )
    {
        Set<String> nouns;

        Jedis jedis = pool.getResource();
        try
        {
            nouns = jedis.zrange( keyOfKarmaSortedSet, lower, upper );
        }
        finally
        {
            pool.returnResource( jedis );
        }

        if ( nouns == null || nouns.size() == 0 )
        {
            return null;
        }

        return nouns;
    }

    /**
     * Remove noun from the karma/rank set.
     * 
     * @param noun The noun to remove, if it exists.
     * @return true iff the noun existed before removing it.
     */
    public static boolean remNoun( String noun )
    {
        Long existed;

        Jedis jedis = pool.getResource();
        try
        {
            existed = jedis.zrem( keyOfKarmaSortedSet, noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }

        if ( existed == null || existed != 1 )
        {
            return false;
        }
        return true;
    }

    public static long getTotalKarma()
    {
        Jedis jedis = pool.getResource();
        long totalKarma;
        try
        {
            Set<String> members = jedis.zrange( keyOfKarmaSortedSet, 0, -1 );
            totalKarma = sum( members, jedis );
        }
        finally
        {
            pool.returnResource( jedis );
        }
        return totalKarma;
    }

    private static long sum( Set<String> members, Jedis jedis )
    {
        long sum = 0;
        for ( String member : members )
        {
            sum += jedis.zscore( keyOfKarmaSortedSet, member );
        }
        return sum;
    }

}
