package vilebot.handlers.user;

import org.junit.Before;
import org.junit.Test;

import com.oldterns.vilebot.handlers.user.RemindMe;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

public class RemindMeTest
{

    ReceivePrivmsg event = mock( ReceivePrivmsg.class );

    private final String INVALID_TYPE_ERROR =
        "The time type given is not valid (use d for day, m for month, s for second)";

    private final String NO_TYPE_ERROR = "There was no type given for the time (use d/m/s)";

    private final String TIME_TOO_LARGE_ERROR = "The value of time given is greater than the maximum Integer value";

    private final String TOO_MANY_REMINDERS_ERROR =
        "There is a limit of 10 reminders, please wait until one reminder ends to set a new one.";

    @Before
    public void setSender()
    {
        when( event.getSender() ).thenReturn( "jucookBot" );
    }

    @Test
    public void verifyReminderSentSeconds()
        throws Exception
    {
        final String time = "5s";
        final String createMessage = "!remindme test " + time;

        createRemindMeWithTime( createMessage, 1, getTimerTime( time ) );
        TimeUnit.SECONDS.sleep( 7 );
        verify( event, times( 1 ) ).replyPrivately( "This is your reminder that you should: test" );
    }

    @Test
    public void verifyReminderSentSecondsMultipleWords()
        throws Exception
    {
        final String time = "5s";
        final String createMessage = "!remindme test test " + time;

        createRemindMeWithTime( createMessage, 1, getTimerTime( time ) );
        TimeUnit.SECONDS.sleep( 7 );
        verify( event, times( 1 ) ).replyPrivately( "This is your reminder that you should: test test" );
    }

    @Test
    public void reminderInvalidType()
    {
        final String time = "1y";
        final String createMessage = "!remindme test " + time;
        final String failMessage =
            String.format( "The given time of %s is invalid. The cause is %s.", time, INVALID_TYPE_ERROR );

        createRemindMeWithTime( createMessage, 1, failMessage );
    }

    @Test
    public void reminderInvalidTypeMultipleWords()
    {
        final String time = "1yetyer";
        final String createMessage = "!remindme test test " + time;
        final String failMessage =
            String.format( "The given time of %s is invalid. The cause is %s.", time, INVALID_TYPE_ERROR );

        createRemindMeWithTime( createMessage, 1, failMessage );
    }

    @Test
    public void reminderNoType()
    {
        final String time = "1";
        final String createMessage = "!remindme test " + time;
        final String failMessage =
            String.format( "The given time of %s is invalid. The cause is %s.", time, NO_TYPE_ERROR );

        createRemindMeWithTime( createMessage, 1, failMessage );
    }

    @Test
    public void reminderNoTypeMultipleWords()
    {
        final String time = "1";
        final String createMessage = "!remindme test test " + time;
        final String failMessage =
            String.format( "The given time of %s is invalid. The cause is %s.", time, NO_TYPE_ERROR );

        createRemindMeWithTime( createMessage, 1, failMessage );
    }

    @Test
    public void reminderTimeTooLarge()
    {
        final String time = "1000000000000s";
        final String createMessage = "!remindme test " + time;
        final String failMessage =
            String.format( "The given time of %s is invalid. The cause is %s.", time, TIME_TOO_LARGE_ERROR );

        createRemindMeWithTime( createMessage, 1, failMessage );
    }

    @Test
    public void reminderTimeTooLargeMultipleWords()
    {
        final String time = "1000000000000s";
        final String createMessage = "!remindme test test " + time;
        final String failMessage =
            String.format( "The given time of %s is invalid. The cause is %s.", time, TIME_TOO_LARGE_ERROR );

        createRemindMeWithTime( createMessage, 1, failMessage );
    }

    @Test
    public void tooManyReminders()
        throws Exception
    {
        final String time = "1s";
        final String createMessage = "!remindme test test " + time;
        final String failMessage =
            String.format( "The given time of %s is invalid. The cause is %s.", time, TOO_MANY_REMINDERS_ERROR );
        createRemindMeWithTime( createMessage, 10, getTimerTime( time ) );
        createRemindMeWithTime( createMessage, 1, failMessage );
        TimeUnit.SECONDS.sleep( 10 );
    }

    private void createRemindMeWithTime( final String createMessage, final Integer invocations, final Calendar time )
    {
        for ( int i = 0; i < invocations; i++ )
        {
            when( event.getText() ).thenReturn( createMessage );

            RemindMe remindMe = new RemindMe();
            remindMe.doRemindMe( event );
        }
        verify( event, times( invocations ) ).replyPrivately( "Created reminder for " + time.getTime() );
    }

    private void createRemindMeWithTime( final String createMessage, final Integer invocations,
                                         final String failMessage )
    {
        for ( int i = 0; i < invocations; i++ )
        {
            when( event.getText() ).thenReturn( createMessage );

            RemindMe remindMe = new RemindMe();
            remindMe.doRemindMe( event );
        }
        verify( event, times( invocations ) ).replyPrivately( failMessage );
    }

    private Calendar getTimerTime( final String time )
    {
        Calendar calendar = Calendar.getInstance();
        Integer timeValue = new Integer( time.substring( 0, time.length() - 1 ) );
        switch ( time.substring( time.length() - 1 ) )
        {
            case "d":
                calendar.add( Calendar.DAY_OF_MONTH, timeValue );
            case "m":
                calendar.add( Calendar.MINUTE, timeValue );
            case "s":
                calendar.add( Calendar.SECOND, timeValue );
        }
        return calendar;
    }
}
