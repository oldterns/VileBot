package com.oldterns.vilebot.services.admin;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.util.SessionService;
import io.quarkus.runtime.Quarkus;
import org.kitteh.irc.client.library.element.User;

@ApplicationScoped
public class QuitService {
    @Inject
    SessionService sessionService;

    @Inject
    GroupDB groupDB;

    @OnMessage("!admin quit")
    public void quit(User sender) {
        String username = sessionService.getSession(Nick.getNick(sender) );
        if ( groupDB.isAdmin( username ) )
        {
            Quarkus.blockingExit();
        }
    }
}
