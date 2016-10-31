package com.oldterns.vilebot.db;

import java.util.Set;

import redis.clients.jedis.Jedis;

/**
 * This class is effectively a duplicate of KarmaDB, used for church related karma
 */
public class ChurchDB
    extends RedisDB
{
    private static final String keyOfChurchDonorSortedSet = "church-donor-karma";
    private static final String keyOfChurchSortedSet = "church-karma";
    private static final String keyOfChurchDonorTitleSortedSet = "church-title-";

    /**
     * Change the karma of a noun by an integer.
     *
     * @param noun The noun to change the karma of
     * @param mod The amount to change the karma by, may be negative.
     */
    public static void modDonorKarma( String noun, int mod )
    {
        Jedis jedis = pool.getResource();
        try
        {
            jedis.zincrby( keyOfChurchDonorSortedSet, mod, noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Change the karma not affiliated with a donor
     *
     */
    public static void modNonDonorKarma( int mod )
    {
        Jedis jedis = pool.getResource();
        try
        {
            jedis.zincrby( keyOfChurchSortedSet, mod, keyOfChurchSortedSet );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

    /**
     * Change the title of noun to a string.
     *
     *  @param noun The noun to change the karma of
     *  @param newTitle The string to change the title to
     */
    public static void modDonorTitle( String noun, String newTitle )
    {
        Jedis jedis = pool.getResource();
        Long titleCount = jedis.scard(keyOfChurchDonorTitleSortedSet + noun);
        try
        {
           for ( Long i = 0L; i < titleCount; i++ )
           {
                jedis.spop( keyOfChurchDonorTitleSortedSet + noun );
           }
           jedis.sadd ( keyOfChurchDonorTitleSortedSet + noun, newTitle );
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
    public static Integer getDonorKarma( String noun )
    {
        Jedis jedis = pool.getResource();
        Double karma;
        try
        {
            karma = jedis.zscore( keyOfChurchDonorSortedSet, noun );
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
    public static Integer getDonorRank( String noun )
    {
        Jedis jedis = pool.getResource();
        Long rank;
        try
        {
            rank = jedis.zrevrank( keyOfChurchDonorSortedSet, noun );
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
     * Get the title of a noun.
     *
     * @param noun The noun to query the rank of
     * @return String iff the noun has a defined value, else null
     */
    public static String getDonorTitle( String noun )
    {
        Jedis jedis = pool.getResource();
        try
        {
            return jedis.srandmember( keyOfChurchDonorTitleSortedSet + noun );
        }
        finally
        {
            pool.returnResource( jedis );
        }
    }

     /**
     * Get nouns from karma ranks.
     *
     * @param lower The lower rank to get the nouns of.
     * @param upper The upper rank to get the nouns of.
     * @return String The noun iff the rank exists, else null.
     */
    public static Set<String> getDonorsByRanks( long lower, long upper )
    {
        Set<String> nouns;
        Jedis jedis = pool.getResource();
        try
        {
            nouns = jedis.zrevrange( keyOfChurchDonorSortedSet, lower, upper );
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

    public static boolean removeDonor( String noun )
    {
        Long existed;
        Jedis jedis = pool.getResource();
        try
        {
            existed = jedis.zrem( keyOfChurchDonorSortedSet, noun );
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

    public static long getTotalDonations()
    {
        Jedis jedis = pool.getResource();
        long totalKarma;
        try
        {
            Set<String> members = jedis.zrange( keyOfChurchDonorSortedSet, 0, -1 );
            totalKarma = sum( members, jedis );
        }
        finally
        {
            pool.returnResource( jedis );
        }
        return totalKarma;
    }

    public static long getTotalNonDonations()
    {
         Jedis jedis = pool.getResource();
         long totalKarma;
         try
         {
             Set<String> members = jedis.zrange( keyOfChurchSortedSet, 0, -1 );
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
        for ( String member : members ) {
            sum += jedis.zscore( keyOfChurchDonorSortedSet, member );
        }
        return sum;
    }
}
