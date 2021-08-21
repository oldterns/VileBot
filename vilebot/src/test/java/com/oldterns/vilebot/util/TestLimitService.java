package com.oldterns.vilebot.util;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.naming.LimitExceededException;

@Alternative
@ApplicationScoped
public class TestLimitService
    implements LimitService
{

    @Override
    public void setLimit( int maxUsesPerPeriod, Duration timePeriod )
    {

    }

    @Override
    public void addUse( String noun )
        throws LimitExceededException
    {

    }

    @Override
    public boolean isAtLimit( String noun )
    {
        return false;
    }
}
