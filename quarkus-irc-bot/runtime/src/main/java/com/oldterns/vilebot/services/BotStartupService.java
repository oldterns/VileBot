package com.oldterns.vilebot.services;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.util.StsUtil;

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

    @ConfigProperty(name="vilebot.irc.server")
    public String ircServer;

    @ConfigProperty(name="vilebot.irc.port", defaultValue = "6667")
    public Integer ircPort;

    public void onStartup(@Observes StartupEvent e) {
        Map<String, Client> botNameToClient = new HashMap<>();
        ircServices.stream().forEach(ircService -> {
            Client client = botNameToClient.computeIfAbsent(ircService.botNick(),
                    nick -> Client.builder()
                                  .name(nick).nick(nick)
                                  .server().host(ircServer).port(ircPort, Client.Builder.Server.SecurityType.INSECURE)
                                  .then()
                                  .management().stsStorageManager(StsUtil.getDefaultStorageManager())
                                  .then()
                                  .build());
            client.getEventManager().registerEventListener(ircService);
            ircService.getChannelsToJoin().forEach(client::addChannel);
            ircService.setBot(client);
        });
        botNameToClient.values().forEach(Client::connect);
    }
}
