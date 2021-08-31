package com.oldterns.irc.bot.services;

import org.kitteh.irc.client.library.Client;

public interface ClientCreator {
    public Client createClient(String nick);
}
