package com.oldterns.vilebot.util;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.naming.LimitExceededException;
import java.time.Duration;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Dependent
public class LimitServiceImpl
    implements LimitService
{

    private final Map<String, Integer> userToUsageCountMap = new ConcurrentHashMap<>();

    private int maxUsesPerPeriod = 2;

    private Duration period = Duration.ofSeconds( 300 );

    @Inject
    TimeService timeService;

    @Override
    public void setLimit( int maxUsesPerPeriod, Duration period )
    {
        this.maxUsesPerPeriod = maxUsesPerPeriod;
        this.period = period;
    }

    @Override
    public void addUse( String noun )
        throws LimitExceededException
    {
        if ( isAtLimit( noun ) )
        {
            throw new LimitExceededException( noun + " has the maximum uses" );
        }

        userToUsageCountMap.merge( noun, 1, Integer::sum );
        timeService.onTimeout( period, () -> {
            userToUsageCountMap.computeIfPresent( noun, ( key, count ) -> {
                if ( count > 1 )
                {
                    return count - 1;
                }
                return null;
            } );
        } );
    }

    @Override
    public boolean isAtLimit( String noun )
    {
        int useCount = userToUsageCountMap.getOrDefault( noun, 0 );
        return useCount >= maxUsesPerPeriod;
    }
}
