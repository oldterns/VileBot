package com.oldterns.irc.bot.services;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.DefaultClient;
import org.kitteh.irc.client.library.util.Cutter;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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


    private static List<String> cutMessage(String message, int maxLengthInBytes) {
        final Function<String, Integer> byteLength = string -> string.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength.apply(message) <= maxLengthInBytes) {
            return List.of(message);
        }
        // Taken from https://stackoverflow.com/a/48870243
        List<String> out = new ArrayList<>();
        CharsetEncoder coder = StandardCharsets.UTF_8.newEncoder();
        ByteBuffer chuck = ByteBuffer.allocate(maxLengthInBytes);  // output buffer of required size
        CharBuffer in = CharBuffer.wrap(message);
        int pos = 0;
        while(true) {
            CoderResult cr = coder.encode(in, chuck, true); // try to encode as much as possible
            int newpos = message.length() - in.length();
            String s = message.substring(pos, newpos);
            out.add(s); // add what has been encoded to the list
            pos = newpos; // store new input position
            chuck.rewind(); // and rewind output buffer
            if (! cr.isOverflow()) {
                break; // everything has been encoded
            }
        }
        return out;
    }

    /**
     * Splits a message into individual lines to be sent to IRC
     * (Limit 512 bytes per line, including command)
     */
    public static String[] splitIrcMessage(String message) {
        final int IRC_MESSAGE_LIMIT = 486; // 512 bytes including command name
        String[] messageLines = message.split("\n");
        List<String> parts = new ArrayList<>(messageLines.length);
        for (String messageLine : messageLines) {
            parts.addAll(cutMessage(messageLine, IRC_MESSAGE_LIMIT));
        }
        String[] messageParts = new String[parts.size()];
        for (int i = 0; i < messageParts.length; i++) {
            messageParts[i] = parts.get(i);
        }
        return messageParts;
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

    public abstract Collection<String> getChannelsToJoin();
}
