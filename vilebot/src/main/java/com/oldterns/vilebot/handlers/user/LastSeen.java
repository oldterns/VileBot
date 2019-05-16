package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.db.LastSeenDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Ignore;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class LastSeen
    extends ListenerAdapter
{

    private static TimeZone timeZone = TimeZone.getTimeZone( "America/Toronto" );

    private static DateFormat dateFormat = makeDateFormat();

    @Override
    public void onGenericMessage( final GenericMessageEvent event )
    {
        String userNick = BaseNick.toBaseNick( event.getUser().getNick() );
        String botNick = event.getBot().getNick();

        if ( !botNick.equals( userNick ) )
        {
            LastSeenDB.updateLastSeenTime( userNick );
        }

        if ( event instanceof JoinEvent && !botNick.equals( userNick ) && !Ignore.getOnJoin().contains( userNick ) )
        {
            longTimeNoSee( (JoinEvent) event, userNick );
        }
    }

    private void longTimeNoSee( JoinEvent event, String joiner )
    {
        OutputIRC outputQ = event.getBot().send();
        String replyTarget = event.getChannel().getName();

        long lastSeen = LastSeenDB.getLastSeenTime( joiner );
        long now = System.currentTimeMillis();
        long timeAgo = now - lastSeen;

        long daysAgo = TimeUnit.MILLISECONDS.toDays( timeAgo );

        if ( daysAgo > 30 )
        {

            String str = "Hi " + joiner + "! I last saw you " + daysAgo + " days ago at "
                + dateFormat.format( new Date( lastSeen ) ) + ". Long time, no see.";
            outputQ.message( replyTarget, str );
        }

        LastSeenDB.updateLastSeenTime( joiner );
    }

    private static DateFormat makeDateFormat()
    {
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mmX" );
        df.setTimeZone( timeZone );
        return df;
    }
}
