package vilebot.util;

import com.oldterns.vilebot.util.BaseNick;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class BaseNickTest
{

    @Test
    public void baseNickTest()
    {
        String baseNick = "salman";

        Set<String> nicks = new HashSet<>();
        nicks.add( "salman" );
        nicks.add( "salman|wfh" );
        nicks.add( "salman_afk" );
        nicks.add( "xsalman" );
        nicks.add( "salman_server_room" );
        nicks.add( "salman|server_room" );

        for ( String nick : nicks )
        {
            assertEquals( baseNick, BaseNick.toBaseNick( nick ) );
        }
    }
}