package com.oldterns.vilebot.services.admin;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.oldterns.vilebot.Nick;
import com.oldterns.vilebot.annotations.OnMessage;
import com.oldterns.vilebot.database.GroupDB;
import com.oldterns.vilebot.database.PasswordDB;
import com.oldterns.vilebot.util.SessionService;
import org.kitteh.irc.client.library.element.User;

@ApplicationScoped
public class AuthService {
    @Inject
    SessionService sessionService;

    @Inject
    GroupDB groupDB;

    @Inject
    PasswordDB passwordDB;

    final Duration MAX_SESSION_LENGTH = Duration.ofMinutes(5);
    // It is odd to allow auth in channels, but apparently the previous version allowed it
    @OnMessage("!admin auth @username @password")
    public String login(User sender, Nick username, String password) {
        if ( groupDB.isAdmin( username.getBaseNick() ) && passwordDB.isValidPassword( username.getBaseNick(), password ) )
        {
            sessionService.addSession( Nick.getNick(sender), username.getBaseNick(), MAX_SESSION_LENGTH );
            return "Authentication successful. Session active for " + MAX_SESSION_LENGTH.toMinutes() + " minutes.";
        }
        else
        {
            return "Authentication failed";
        }
    }
}
