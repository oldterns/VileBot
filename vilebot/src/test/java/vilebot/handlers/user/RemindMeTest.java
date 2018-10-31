package vilebot.handlers.user;

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

    @Test
    public void verifyReminderSent()
        throws Exception
    {
        when( event.getText() ).thenReturn( "!remindme test 1" );
        Calendar timerTime = getTimerTime( "1" );

        RemindMe remindMe = new RemindMe();
        remindMe.doRemindMe( event );

        verify( event, times( 1 ) ).replyPrivately( "Created reminder at " + timerTime.getTime() );
        TimeUnit.MINUTES.sleep( 1 );
        verify( event, times( 1 ) ).replyPrivately( "This is your reminder that you should: test" );
    }

    private Calendar getTimerTime( final String time )
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.MINUTE, new Integer( time ) );
        return calendar;
    }
}
