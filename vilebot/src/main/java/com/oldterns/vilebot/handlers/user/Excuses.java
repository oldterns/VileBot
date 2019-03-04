package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.db.ExcuseDB;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Excuses
    extends ListenerAdapter
{
    private static final Pattern excusePattern = Pattern.compile( "!excuse" );

    @Override
    public void onGenericMessage( GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = excusePattern.matcher( text );

        if ( matcher.matches() )
        {
            String excuse = ExcuseDB.getRandExcuse();
            if ( excuse != null )
            {
                event.respondWith( excuse );
            }
            else
            {
                event.respondWith( "No excuses available" );
            }
        }
    }

    // TODO method to add excuses
}
