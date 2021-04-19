package com.oldterns.vilebot.services;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.irc.IrcMessage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.schwering.irc.lib.IRCUser;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class IRCService extends RouteBuilder {
    @ConfigProperty(name="vilebot.irc.server")
    public String ircServer;

    @ConfigProperty(name="vilebot.irc.port", defaultValue = "6667")
    public Integer ircPort;

    @ConfigProperty(name="vilebot.default.nick")
    public String defaultNick;

    @ConfigProperty(name="vilebot.default.channel")
    public String defaultChannel;

    public String botNick() {
        return defaultNick;
    }

    public Predicate messageTextMatches(java.util.function.Predicate<String> predicate) {
        return messageMatches(ircMessage -> predicate.test(ircMessage.getMessage()));
    }

    public Predicate messageMatches(java.util.function.Predicate<ChannelMessage> predicate) {
        return (exchange) -> {
            ChannelMessage ircMessage = getChannelMessage(exchange);
            return predicate.test(ircMessage);
        };
    }

    public Processor reply(Function<ChannelMessage, String> replyFunction) {
        return exchange -> {
            IrcMessage ircMessage = getIrcMessage(exchange);
            String reply = replyFunction.apply(new ChannelMessage(ircMessage));
            if (reply != null) {
                IrcMessage out = new IrcMessage(ircMessage.getCamelContext());
                out.setMessage(reply);
                out.setUser(new IRCUser(botNick(), botNick(), ircServer));
                exchange.setMessage(out);
            }
        };
    }

    public Processor handleMessage(Consumer<ChannelMessage> consumer) {
        return exchange -> {
            ChannelMessage channelMessage = getChannelMessage(exchange);
            consumer.accept(channelMessage);
        };
    }

    public IrcMessage getIrcMessage(Exchange exchange) {
        return exchange.getIn(IrcMessage.class);
    }

    public ChannelMessage getChannelMessage(Exchange exchange) {
        return new ChannelMessage(getIrcMessage(exchange));
    }

    public String getMainChannel() {
        return getChannel(defaultChannel);
    }

    public String getChannel(String channel) {
        if ("<<default>>".equals(channel)) {
            return getMainChannel();
        }
        return String.format("irc:%s@%s:%d?channels=%s&nickname=%s", botNick(),
                ircServer, ircPort, channel, botNick());
    }
}
