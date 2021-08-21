package com.oldterns.vilebot.util;

import java.util.HashSet;
import java.util.Set;

import com.oldterns.irc.bot.Nick;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat( Nick.valueOf( nick ).getBaseNick() ).isEqualTo( baseNick );
        }
    }
}
