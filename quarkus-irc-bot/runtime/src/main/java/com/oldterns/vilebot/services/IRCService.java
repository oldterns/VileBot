package com.oldterns.vilebot.services;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kitteh.irc.client.library.Client;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        return defaultChannel;
    }

    public Collection<String> getChannel(String channel) {
        if ("<<default>>".equals(channel)) {
            return Collections.singleton(getMainChannel());
        }
        if ("<<all>>".equals(channel)) {
            return getChannelsToJoin();
        }
        if (channel.startsWith("${")) {
            String channelSet = ConfigProvider.getConfig().getValue(channel.substring(2, channel.length() - 1), String.class);
            return List.of(channelSet.split(","));
        }
        return List.of(channel.split(","));
    }

    public Client getBot() {
        return bot;
    }

    public void setBot(Client bot) {
        this.bot = bot;
    }

    public abstract Collection<String> getChannelsToJoin();
}
