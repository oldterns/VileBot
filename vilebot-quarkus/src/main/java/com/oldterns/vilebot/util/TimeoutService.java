package com.oldterns.vilebot.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Future;

public interface TimeoutService {
    LocalDateTime getCurrentDateTime();
    Future<?> onTimeout(Duration timeoutDuration, Runnable onTimeout);
}
