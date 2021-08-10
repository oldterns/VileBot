package com.oldterns.irc.bot.services;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.kitteh.irc.client.library.Client;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class BotStartupService {

    @Inject
    Instance<IRCService> ircServices;

    @Inject
    ClientCreator clientCreator;

    Map<String, Client> botNameToClient = new HashMap<>();

    public void onStartup(@Observes StartupEvent e) throws InterruptedException {
        ircServices.stream().forEach(ircService -> {
            Client client = botNameToClient.computeIfAbsent(ircService.botNick(),
                    clientCreator::createClient);
            ircService.getChannelsToJoin().forEach(client::addChannel);
            ircService.setBot(client);
        });
        for (Client client : botNameToClient.values()) {
            client.connect();
            ircServices.stream().forEach(client.getEventManager()::registerEventListener);
            Thread.sleep(1000);
        }
    }

    public void onShutdown(@Observes ShutdownEvent shutdownEvent) {
        for (Client client : botNameToClient.values()) {
            client.shutdown();
        }
    }
}
