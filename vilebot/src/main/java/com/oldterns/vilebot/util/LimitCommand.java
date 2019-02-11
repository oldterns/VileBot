package com.oldterns.vilebot.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class LimitCommand
{
    private Map<String, Integer> usesUserMap = new HashMap<>();

    private int maxUsesPerPeriod = 2;

    private int timePeriodSeconds = 300;

    private static final String OKAY_STRING = " has less than the maximum uses";

    private static final String NOT_OKAY_STRING = " has the maximum uses";

    public LimitCommand()
    {
    }

    public LimitCommand( int maxUsesPerPeriod, int timePeriodSeconds )
    {
        this.maxUsesPerPeriod = maxUsesPerPeriod;
        this.timePeriodSeconds = timePeriodSeconds;
    }

    public String addUse( String user )
    {
        Timer timer = new Timer();
        if ( !usesUserMap.containsKey( user ) )
        {
            usesUserMap.put( user, 0 );
        }
        int uses = getUses( user );
        if ( uses < maxUsesPerPeriod )
        {
            timer.schedule( createTimerTask( user ), timePeriodSeconds * 1000 );
            uses++;
            usesUserMap.put( user, uses );
            return "";
        }
        else
        {
            return user + NOT_OKAY_STRING;
        }
    }

    public int getUses( String user )
    {
        return usesUserMap.get( user );
    }

    public String checkUses( String user )
    {
        int uses = getUses( user );
        if ( uses < maxUsesPerPeriod )
        {
            return user + OKAY_STRING;
        }
        else
        {
            return user + NOT_OKAY_STRING;
        }
    }

    private TimerTask createTimerTask( final String user )
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                int amountOfUses = usesUserMap.get( user );
                amountOfUses--;
                usesUserMap.put( user, amountOfUses );
            }
        };
    }
}
