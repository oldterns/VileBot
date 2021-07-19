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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class KarmaDB
{
    private static final String keyOfKarmaSortedSet = "noun-karma";

    @Inject
    RedisClient redisClient;

    /**
     * Change the karma of a noun by an integer.
     * 
     * @param noun The noun to change the karma of
     * @param mod The amount to change the karma by, may be negative.
     */
    public void modNounKarma( String noun, Number mod )
    {
        redisClient.zincrby( keyOfKarmaSortedSet, Long.toString( mod.longValue() ), noun );
    }

    /**
     * Get the karma of a noun.
     * 
     * @param noun The noun to query to karma of
     * @return Integer iff the noun has a defined value, else null
     */
    public Optional<Long> getNounKarma( String noun )
    {
        return Optional.ofNullable( redisClient.zscore( keyOfKarmaSortedSet, noun ) ).map( Response::toLong );
    }

    /**
     * Get the rank of a noun based on its karma.
     * 
     * @param noun The noun to query the rank of
     * @return Integer iff the noun has a defined value, else null
     */
    public Optional<Long> getNounRank( String noun )
    {
        return Optional.ofNullable( redisClient.zrevrank( keyOfKarmaSortedSet, noun ) ).map( Response::toLong );
    }

    /**
     * Get the rank of a noun based on its karma, starting at most negative karma.
     * 
     * @param noun The noun to query the reverse rank of
     * @return Integer iff the noun has a defined value, else null
     */
    public Optional<Long> getNounRevRank( String noun )
    {
        return Optional.ofNullable( redisClient.zrank( keyOfKarmaSortedSet,
                                                       noun ) ).map( Response::toLong ).map( revRank -> revRank + 1 );
    }

    /**
     * Get noun from a karma rank (Rank 1 is the member with the highest karma).
     * 
     * @param rank The rank to get the noun of.
     * @return String The noun iff the rank exists, else null.
     */
    public String getRankNoun( long rank )
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
    public Set<String> getRankNouns( Long lower, Long upper )
    {
        Set<String> nouns =
            redisClient.zrevrange( List.of( keyOfKarmaSortedSet, lower.toString(),
                                            upper.toString() ) ).stream().map( Response::toString ).collect( Collectors.toSet() );

        if ( nouns.size() == 0 )
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
    public String getRevRankNoun( long rank )
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
    public Set<String> getRevRankNouns( Long lower, Long upper )
    {
        Set<String> nouns =
            redisClient.zrange( List.of( keyOfKarmaSortedSet, lower.toString(),
                                         upper.toString() ) ).stream().map( Response::toString ).collect( Collectors.toSet() );

        if ( nouns.size() == 0 )
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
    public boolean remNoun( String noun )
    {
        Long existed = redisClient.zrem( List.of( keyOfKarmaSortedSet, noun ) ).toLong();

        if ( existed == null || existed != 1 )
        {
            return false;
        }
        return true;
    }

    public long getTotalKarma()
    {
        Set<String> members =
            redisClient.zrange( List.of( keyOfKarmaSortedSet, "0",
                                         "-1" ) ).stream().map( Response::toString ).collect( Collectors.toSet() );
        return sum( members );
    }

    private long sum( Set<String> members )
    {
        long sum = 0;
        for ( String member : members )
        {
            sum += redisClient.zscore( keyOfKarmaSortedSet, member ).toLong();
        }
        return sum;
    }

}
