package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.db.PasswordDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Auth
    extends ListenerAdapter
{
    private static final Pattern authPattern = Pattern.compile( "!admin auth (\\S+) (\\S+)" );

    private static final long sessionLength = TimeUnit.MINUTES.toMillis( 5 );

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = authPattern.matcher( text );
        String sender = event.getUser().getNick();

        if ( matcher.matches() )
        {
            String username = BaseNick.toBaseNick( matcher.group( 1 ) );
            String password = matcher.group( 2 );

            if ( GroupDB.isAdmin( username ) && PasswordDB.isValidPassword( username, password ) )
            {
                Sessions.addSession( sender, username, sessionLength );
                event.respondWith( "Authentication successful. Session active for "
                    + TimeUnit.MILLISECONDS.toMinutes( sessionLength ) + " minutes." );
            }
            else
            {
                event.respondWith( "Authentication failed" );
            }
        }
    }
}
