package com.oldterns.irc.bot.services;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.util.StsUtil;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientCreatorImpl implements ClientCreator {

    @ConfigProperty(name="vilebot.irc.server")
    public String ircServer;

    @ConfigProperty(name="vilebot.irc.port", defaultValue = "6667")
    public Integer ircPort;

    public Client createClient(String nick) {
        return Client.builder()
                     .name(nick).nick(nick).realName(nick).user(nick)
                     .server().host(ircServer).port(ircPort, Client.Builder.Server.SecurityType.INSECURE)
                     .then()
                     .management().stsStorageManager(StsUtil.getDefaultStorageManager())
                     .then()
                     .build();
    }
}
