package com.oldterns.vilebot.services;

import com.oldterns.irc.bot.Nick;
import com.oldterns.vilebot.database.LastSeenDB;
import com.oldterns.vilebot.util.TimeService;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.helper.ActorEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class LastSeenService
{

    private static TimeZone timeZone = TimeZone.getTimeZone( "America/Toronto" );

    private static DateTimeFormatter dateFormatter = makeDateFormat();

    @Inject
    LastSeenDB lastSeenDB;

    @Inject
    TimeService timeService;

    @Handler
    @SuppressWarnings( "unchecked" )
    public void updateLastSeenTime( ActorEvent<?> event )
    {
        // generic infomation is hidden; need to check if the
        // event is for a User
        if ( event.getActor() instanceof User )
        {
            String joiner = Nick.getNick( (ActorEvent<User>) event ).getBaseNick();
            lastSeenDB.updateLastSeenTime( joiner );
        }
    }

    @Handler
    public String onJoin( ChannelJoinEvent event )
    {
        String joiner = Nick.getNick( event ).getBaseNick();
        Optional<Long> maybeLastSeen = lastSeenDB.getLastSeenTime( joiner );

        lastSeenDB.updateLastSeenTime( joiner );
        if ( maybeLastSeen.isEmpty() )
        {
            return null;
        }

        long lastSeen = maybeLastSeen.get();
        long now = timeService.getCurrentTimeMills();
        long timeAgo = now - lastSeen;

        long daysAgo = TimeUnit.MILLISECONDS.toDays( timeAgo );

        if ( daysAgo > 30 )
        {
            return "Hi " + joiner + "! I last saw you " + daysAgo + " days ago at "
                + OffsetDateTime.ofInstant( Instant.ofEpochMilli( lastSeen ),
                                            timeZone.toZoneId().getRules().getOffset( Instant.ofEpochMilli( lastSeen ) ) ).format( dateFormatter )
                + ". Long time, no see.";
        }
        return null;
    }

    private static DateTimeFormatter makeDateFormat()
    {
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mmX" );
        df.setTimeZone( timeZone );
        return DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mmX" ).withZone( timeZone.toZoneId() );
    }
}
