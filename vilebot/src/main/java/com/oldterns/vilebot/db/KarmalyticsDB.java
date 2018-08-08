package com.oldterns.vilebot.db;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.oldterns.vilebot.Vilebot;
import com.oldterns.vilebot.karmalytics.HasKarmalytics;
import com.oldterns.vilebot.karmalytics.KarmalyticsRecord;
import redis.clients.jedis.Jedis;

public class KarmalyticsDB
    extends RedisDB
{
    private final static TimeZone TIME_ZONE = TimeZone.getTimeZone( Vilebot.getConfig().get( "timezone" ) );

    private final static String KA_KEY = "karmalytics";

    private final static Map<String, List<String>> karmalyticsIdToGroups = new HashMap<>();

    private final static Map<String, Set<String>> karmalyticsGroupToMemberIds = new HashMap<>();

    private final static Map<String, Function<KarmalyticsRecord, String>> karmalyticsIdToDescriptorFunctions =
        new HashMap<>();

    // A record has the size of `length of timestamp + length of nick + length of id
    // + length of descriptor + size of
    // karma mod + size of score
    // + 4`
    // length on timestamp will be between 21 and 31 (see ISO Timestamps),
    // length of nick will be around 8
    // length of id be around 10
    // length of karma mod will be around 5
    // score is a double, so it will be 8 bytes
    // so the min size would be 31 + 8 + 10 + 5 + 8 + 4 = 64
    //
    // 64 * 1000000 = 64, 000, 000 bytes, or 64MB
    // Unlike the other databases, this one will have a lot of records, so it is
    // necessary to keep them in check
    private final static long MAX_RECORDS = 1000000;

    public static Set<String> getSources()
    {
        return karmalyticsIdToGroups.keySet();
    }

    public static Function<KarmalyticsRecord, String> getRecordDescriptorFunctionForSource( String source )
    {
        return karmalyticsIdToDescriptorFunctions.get( source );
    }

    public static Set<String> getGroups()
    {
        return karmalyticsGroupToMemberIds.keySet();
    }

    public static List<String> getGroupsForMember( String member )
    {
        return karmalyticsIdToGroups.get( member );
    }

    public static boolean isMemberInGroup( String member, String group )
    {
        return karmalyticsGroupToMemberIds.get( group ).contains( member );
    }

    public static void intializeKarmalyticsFor( HasKarmalytics hasKarmalytics )
    {
        if ( hasKarmalytics.getKarmalyticsId().contains( ">" ) )
        {
            throw new IllegalArgumentException( "\">\" is an illegal character in Karmalytics identifiers." );
        }

        if ( !karmalyticsIdToGroups.containsKey( hasKarmalytics.getKarmalyticsId() ) )
        {

            karmalyticsIdToGroups.put( hasKarmalytics.getKarmalyticsId(), hasKarmalytics.getGroups() );
            karmalyticsIdToDescriptorFunctions.put( hasKarmalytics.getKarmalyticsId(),
                                                    hasKarmalytics.getRecordDescriptorFunction() );

            for ( String group : hasKarmalytics.getGroups() )
            {
                karmalyticsGroupToMemberIds.computeIfAbsent( group,
                                                             k -> new HashSet<>() ).add( hasKarmalytics.getKarmalyticsId() );
            }
        }
    }

    public static void modNounKarma( HasKarmalytics source, Optional<String> extraInfo, String nick, int mod )
    {
        if ( !karmalyticsIdToGroups.containsKey( source.getKarmalyticsId() ) )
        {
            throw new IllegalStateException( "Karmalytics is not tracking " + source.getKarmalyticsId() );
        }

        KarmaDB.modNounKarma( nick, mod );

        Jedis jedis = pool.getResource();

        // Check to see if we hit max records, and if so remove the earliest record
        if ( jedis.zcard( KA_KEY ) > MAX_RECORDS )
        {
            jedis.zremrangeByRank( KA_KEY, 0, 0 );
        }
        StringBuilder value = new StringBuilder();

        Instant currentTime = Instant.now();
        value.append( LocalDateTime.ofInstant( currentTime,
                                               TIME_ZONE.toZoneId().getRules().getOffset( currentTime ) ) ).toString();
        value.append( ">" );
        value.append( nick );
        value.append( ">" );
        value.append( source.getKarmalyticsId() );
        value.append( ">" );
        if ( extraInfo.isPresent() )
        {
            value.append( Base64.getEncoder().encodeToString( extraInfo.get().getBytes() ) );
        }
        value.append( ">" );
        if ( mod >= 0 )
        {
            value.append( "+" );
            value.append( mod );
        }
        else
        {
            value.append( mod );
        }

        jedis.zadd( KA_KEY, currentTime.toEpochMilli(), value.toString() );

        pool.returnResource( jedis );
    }

    public static LocalDateTime getTimestampOfFirstRecord()
    {
        Jedis jedis = pool.getResource();

        Set<String> record = jedis.zrange( KA_KEY, 0, 0 );

        pool.returnResource( jedis );

        if ( record.isEmpty() )
        {
            throw new IndexOutOfBoundsException();
        }

        return new KarmalyticsRecord( record.stream().findFirst().get() ).getDateTime();
    }

    public static Set<KarmalyticsRecord> getRecordsBetween( LocalDateTime from, LocalDateTime to )
    {
        Jedis jedis = pool.getResource();

        long fromScore = from.toEpochSecond( TIME_ZONE.toZoneId().getRules().getOffset( from ) ) * 1000;
        long toScore = to.toEpochSecond( TIME_ZONE.toZoneId().getRules().getOffset( to ) ) * 1000;

        Set<KarmalyticsRecord> out =
            jedis.zrangeByScore( KA_KEY, fromScore,
                                 toScore ).stream().map( KarmalyticsRecord::new ).collect( Collectors.toSet() );

        pool.returnResource( jedis );
        return out;

    }

    public static List<KarmalyticsRecord> getRangeByEndRank( long distanceFromEnd, long maxRecordsToFetch )
    {
        Jedis jedis = pool.getResource();

        List<KarmalyticsRecord> out =
            jedis.zrange( KA_KEY, -( distanceFromEnd + maxRecordsToFetch ), -( distanceFromEnd + 1 ) )
                 // Exploit the fact ISO date-times can be lexicologically sorted
                 .stream().sorted().map( KarmalyticsRecord::new ).collect( Collectors.toList() );
        pool.returnResource( jedis );
        return out;
    }

}
