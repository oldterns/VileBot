package com.oldterns.vilebot.handlers.user;

import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import net.engio.mbassy.listener.Handler;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@HandlerContainer
public class RemindMe
{

    private static final Pattern remindMePattern = Pattern.compile( "^!remindme (.+) (.+)" );

    @Handler
    public void doRemindMe( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = remindMePattern.matcher( text );

        if ( matcher.matches() )
        {
            String message = matcher.group( 1 );
            String time = matcher.group( 2 );
            Calendar timerTime = getTimerTime( time );
            Timer timer = new Timer();
            timer.schedule( createTimerTask( event, message ), timerTime.getTime() );
            event.replyPrivately( "Created reminder at " + timerTime.getTime() );
        }
    }

    private TimerTask createTimerTask( final ReceivePrivmsg event, final String message )
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                event.replyPrivately( "This is your reminder that you should: " + message );
            }
        };
    }

    private Calendar getTimerTime( final String time )
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.MINUTE, new Integer( time ) );
        return calendar;
    }
}
