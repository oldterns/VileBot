package vilebot.util;

import com.oldterns.vilebot.util.MangleNicks;

import ca.szc.keratin.bot.Channel;
import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.core.event.message.recieve.ReceiveJoin;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MangleNicksTest
{

    private KeratinBot bot;

    private Channel chan;

    private ReceivePrivmsg rcvPrivMsg;

    private ReceiveJoin rcvJoin;

    private String chanString = "#thefoobar";

    private List<String> nickList = Arrays.asList( "salman", "sasiddiq" );

    @Before
    public void setup()
    {
        bot = mock( KeratinBot.class );
        chan = mock( Channel.class );
        rcvPrivMsg = mock( ReceivePrivmsg.class );
        rcvJoin = mock( ReceiveJoin.class );

        when( bot.getChannel( chanString ) ).thenReturn( chan );
        when( chan.getNicks() ).thenReturn( nickList );
        when( rcvPrivMsg.getChannel() ).thenReturn( chanString );
        when( rcvJoin.getChannel() ).thenReturn( chanString );
    }

    @Test
    public void noNicks()
    {
        String messageText = "i am the karma police";
        String returnText1 = MangleNicks.mangleNicks( bot, rcvPrivMsg, messageText );
        String returnText2 = MangleNicks.mangleNicks( bot, rcvPrivMsg, messageText );

        assertEquals( returnText1, messageText );
        assertEquals( returnText2, messageText );
    }

    @Test
    public void oneNick()
    {
        String messageText = "salman is a man of many bots";
        String returnText1 = MangleNicks.mangleNicks( bot, rcvPrivMsg, messageText );
        String returnText2 = MangleNicks.mangleNicks( bot, rcvPrivMsg, messageText );
        String expectedReturn = "namlas is a man of many bots";

        assertEquals( returnText1, expectedReturn );
        assertEquals( returnText2, expectedReturn );
    }

    @Test
    public void multipleNicks()
    {
        String messageText = "salman is actually sasiddiq";
        String returnText1 = MangleNicks.mangleNicks( bot, rcvPrivMsg, messageText );
        String returnText2 = MangleNicks.mangleNicks( bot, rcvPrivMsg, messageText );
        String expectedReturn = "namlas is actually qiddisas";

        assertEquals( returnText1, expectedReturn );
        assertEquals( returnText2, expectedReturn );
    }
}
