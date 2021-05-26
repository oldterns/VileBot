package com.oldterns.vilebot.services;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kitteh.irc.client.library.Client;

import java.util.Collection;

public abstract class IRCService {
    @ConfigProperty(name="vilebot.default.nick")
    public String defaultNick;

    @ConfigProperty(name="vilebot.default.channel")
    public String defaultChannel;

    private Client bot;

    public String botNick() {
        return defaultNick;
    }

    public String getMainChannel() {
        return getChannel(defaultChannel);
    }

    public String getChannel(String channel) {
        if ("<<default>>".equals(channel)) {
            return getMainChannel();
        }
        return channel;
    }

    public Client getBot() {
        return bot;
    }

    public void setBot(Client bot) {
        this.bot = bot;
    }

    public abstract Collection<String> getChannelsToJoin();
}
