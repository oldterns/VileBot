package com.oldterns.vilebot.database;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is effectively a duplicate of KarmaDB, used for church related karma
 */
@ApplicationScoped
public class ChurchDB
{
    private static final String keyOfChurchDonorSortedSet = "church-donor-karma";

    private static final String keyOfChurchSortedSet = "church-karma";

    private static final String keyOfChurchDonorTitleSortedSet = "church-title-";

    @Inject
    RedisClient redisClient;

    public boolean isTopDonor( String noun )
    {
        return getDonorRank( noun ).map( rank -> rank <= 4 ).orElse( false );
    }

    /**
     * Change the karma of a noun by an integer.
     *
     * @param noun The noun to change the karma of
     * @param mod The amount to change the karma by, may be negative.
     */
    public void modDonorKarma( String noun, Integer mod )
    {
        redisClient.zincrby( keyOfChurchDonorSortedSet, mod.toString(), noun );
    }

    /**
     * Change the karma not affiliated with a donor
     */
    public void modNonDonorKarma( Integer mod )
    {
        redisClient.zincrby( keyOfChurchSortedSet, mod.toString(), keyOfChurchSortedSet );
    }

    /**
     * Change the title of noun to a string.
     *
     * @param noun The noun to change the karma of
     * @param newTitle The string to change the title to
     */
    public void modDonorTitle( String noun, String newTitle )
    {
        Long titleCount = redisClient.scard( keyOfChurchDonorTitleSortedSet + noun ).toLong();
        for ( Long i = 0L; i < titleCount; i++ )
        {
            redisClient.spop( List.of( keyOfChurchDonorTitleSortedSet + noun ) );
        }
        redisClient.sadd( List.of( keyOfChurchDonorTitleSortedSet + noun, newTitle ) );
    }

    /**
     * Get the karma of a noun.
     *
     * @param noun The noun to query to karma of
     * @return Integer iff the noun has a defined value, else null
     */
    public Optional<Long> getDonorKarma( String noun )
    {
        return Optional.ofNullable( redisClient.zscore( keyOfChurchDonorSortedSet, noun ).toLong() );
    }

    /**
     * Get the rank of a noun based on its karma.
     *
     * @param noun The noun to query the rank of
     * @return Integer iff the noun has a defined value, else null
     */
    public Optional<Long> getDonorRank( String noun )
    {
        return Optional.ofNullable( redisClient.zrevrank( keyOfChurchDonorSortedSet, noun ) ).map( rank -> rank.toLong()
            + 1 );
    }

    /**
     * Get the title of a noun.
     *
     * @param noun The noun to query the rank of
     * @return String iff the noun has a defined value, else null
     */
    public String getDonorTitle( String noun )
    {
        return redisClient.srandmember( List.of( keyOfChurchDonorTitleSortedSet + noun ) ).toString();
    }

    /**
     * Get nouns from karma ranks.
     *
     * @param lower The lower rank to get the nouns of.
     * @param upper The upper rank to get the nouns of.
     * @return String The noun iff the rank exists, else null.
     */
    public Set<String> getDonorsByRanks( Long lower, Long upper )
    {
        Set<String> nouns =
            redisClient.zrevrange( List.of( keyOfChurchDonorSortedSet, lower.toString(),
                                            upper.toString() ) ).stream().map( Response::toString ).collect( Collectors.toSet() );

        if ( nouns.size() == 0 )
        {
            return null;
        }

        return nouns;
    }

    public boolean removeDonor( String noun )
    {
        Long existed = redisClient.zrem( List.of( keyOfChurchDonorSortedSet, noun ) ).toLong();
        if ( Long.valueOf( existed ) != 1 )
        {
            return false;
        }

        return true;
    }

    public long getTotalDonations()
    {
        Set<String> members =
            redisClient.zrange( List.of( keyOfChurchDonorSortedSet, "0",
                                         "-1" ) ).stream().map( Response::toString ).collect( Collectors.toSet() );
        return sum( keyOfChurchDonorSortedSet, members );
    }

    public long getTotalNonDonations()
    {
        Set<String> members =
            redisClient.zrange( List.of( keyOfChurchSortedSet, "0",
                                         "-1" ) ).stream().map( Response::toString ).collect( Collectors.toSet() );
        return sum( keyOfChurchSortedSet, members );
    }

    private long sum( String set, Set<String> members )
    {
        long sum = 0;
        for ( String member : members )
        {
            sum += redisClient.zscore( set, member ).toLong();
        }
        return sum;
    }
}
