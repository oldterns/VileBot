package com.oldterns.vilebot.services;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.annotations.Regex;
import com.oldterns.vilebot.util.TimeoutService;
import org.kitteh.irc.client.library.element.User;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class RemindMeService {

    @Inject
    TimeoutService timeoutService;

    String BAD_TIME_UNIT = "The time type given is not valid (use d for day, h for hour, m for minute, s for second)";

    Map<String, Integer> userToReminderCount = new HashMap<>();

    @OnMessage("!remindme @message @length ?@timeUnit")
    public String remindMe(User user, String message, Integer length, @Regex("\\S") String timeUnit) {
        Duration duration;
        switch (timeUnit) {
            case "d": case "D":
                duration = Duration.ofDays(length);
                break;
            case "h": case "H":
                duration = Duration.ofHours(length);
                break;
            case "m": case "M":
                duration = Duration.ofMinutes(length);
                break;
            case "s": case "S":
                duration = Duration.ofSeconds(length);
                break;
            default:
                return  BAD_TIME_UNIT;
        }

        final String userKey = Nick.getNick(user).getBaseNick();
        if (userToReminderCount.getOrDefault(userKey, 0) > 10) {
            return "There is a limit of 10 reminders, please wait until one reminder ends to set a new one.";
        }

        // TODO: Persist this in the database in case VileBot is restarted?
        userToReminderCount.merge(userKey, 1, Integer::sum);
        timeoutService.onTimeout(duration, () -> {
            user.sendMessage("This is your reminder that you should: " + message);
            userToReminderCount.computeIfPresent(userKey, (key, currentCount) -> {
                if (currentCount == 1) {
                    return null;
                }
                return currentCount - 1;
            });
        });

        return "Created reminder for " +  timeoutService.getCurrentDateTime().plus(duration).toString();
    }
}
