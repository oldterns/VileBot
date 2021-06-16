package com.oldterns.vilebot.util;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@ApplicationScoped
public class TimeoutServiceImpl implements TimeoutService {
    ExecutorService executorService = Executors.newScheduledThreadPool( 1 );
    @Override
    public Future<?> onTimeout(Duration timeoutDuration, Runnable onTimeout) {
        return executorService.submit(() -> {
            try
            {
                Thread.sleep( timeoutDuration.toMillis() );
                onTimeout.run();
            }
            catch ( InterruptedException e )
            {
                // Do nothing; the task completed normally;
            }
        });
    }
}
