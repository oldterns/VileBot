package com.oldterns.vilebot.util;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ApplicationScoped
public class TimeServiceImpl
    implements TimeService
{
    ExecutorService executorService = Executors.newScheduledThreadPool( 1 );

    @Override
    public long getCurrentTimeMills()
    {
        return System.currentTimeMillis();
    }

    @Override
    public LocalDateTime getCurrentDateTime()
    {
        return LocalDateTime.now();
    }

    @Override
    public Future<?> onTimeout( Duration timeoutDuration, Runnable onTimeout )
    {
        return executorService.submit( () -> {
            try
            {
                Thread.sleep( timeoutDuration.toMillis() );
                onTimeout.run();
            }
            catch ( InterruptedException e )
            {
                // Do nothing; the task completed normally;
            }
        } );
    }
}
