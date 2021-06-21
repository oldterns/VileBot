package com.oldterns.vilebot.services;

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

    public void onStartup(@Observes StartupEvent e) throws InterruptedException {
        Map<String, Client> botNameToClient = new HashMap<>();
        ircServices.stream().forEach(ircService -> {
            Client client = botNameToClient.computeIfAbsent(ircService.botNick(),
                    clientCreator::createClient);
            client.getEventManager().registerEventListener(ircService);
            ircService.getChannelsToJoin().forEach(client::addChannel);
            ircService.setBot(client);
        });
        for (Client client : botNameToClient.values()) {
            client.connect();
            Thread.sleep(1000);
        }
    }
}
