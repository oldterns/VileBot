package com.oldterns.vilebot.services;

import org.kitteh.irc.client.library.Client;

public interface ClientCreator {
    public Client createClient(String nick);
}
