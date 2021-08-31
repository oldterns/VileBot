package com.oldterns.irc.bot.services;

import com.oldterns.irc.bot.annotations.OnMessage;
import org.kitteh.irc.client.library.element.User;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HelpService {
    Map<String, List<String>> groupToCommandList = new HashMap<>();

    public void addHelpCommand(String group, String command) {
        groupToCommandList.computeIfAbsent(group, key -> new ArrayList<>()).add(command);
    }

    @OnMessage("!help")
    public void help(User user) {
        user.sendMessage("Available Commands: ");
        groupToCommandList.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "  " + entry.getKey() + ": " + String.join(" ", entry.getValue()))
                .forEach(user::sendMessage);
    }
}
