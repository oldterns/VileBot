package com.oldterns.vilebot.util;

import java.time.Duration;
import java.util.concurrent.Future;

public interface TimeoutService {
    Future<?> onTimeout(Duration timeoutDuration, Runnable onTimeout);
}
