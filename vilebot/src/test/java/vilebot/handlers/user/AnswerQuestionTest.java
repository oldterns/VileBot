package vilebot.handlers.user;

import com.oldterns.vilebot.handlers.user.AnswerQuestion;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class AnswerQuestionTest
{
    AnswerQuestion answerQuestion;
    ReceivePrivmsg event;

    @Before
    public void setSenderAndChannel()
    {
        answerQuestion = new AnswerQuestion();
        event = mock( ReceivePrivmsg.class );
        when( event.getSender() ).thenReturn( "salman" );
        when( event.getChannel() ).thenReturn( "#thefoobar" );
    }

    @Test
    public void queryTest1()
    {
        String ircmsg = "!tellme glass";
        String expectedReply = "glass\n"
                + "glass count";
        when( event.getText() ).thenReturn( ircmsg );
        answerQuestion.tellMe( event );
        verify( event, times( 1 ) ).reply( expectedReply );
    }
}
