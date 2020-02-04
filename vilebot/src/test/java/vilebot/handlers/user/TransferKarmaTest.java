/**
 * Copyright (C) 2020 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package vilebot.handlers.user;

import com.oldterns.vilebot.handlers.user.KarmaTransfer;
import org.junit.Before;
import org.junit.Test;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransferKarmaTest
{
    private MessageEvent event;

    private static final String VILEBOT_CONFIG_FILE = "cfg/vilebot.conf";

    private static final String VILEBOT_CONFIG_FILE_SECONDARY = "cfg/vilebot.conf.example";

    @Before
    public void setup()
    {
        event = mock( MessageEvent.class );
        User user = mock( User.class );
        Channel channel = mock( Channel.class );
        when( event.getUser() ).thenReturn( user );
        when( user.getNick() ).thenReturn( "senderNick" );
        when( event.getChannel() ).thenReturn( channel );
        when( channel.getName() ).thenReturn( getChannel() );
    }

    @Test
    public void invalidChannelTest()
    {
        Channel channel = mock( Channel.class );
        when( event.getChannel() ).thenReturn( channel );
        when( channel.getName() ).thenReturn( "INVALID CHANNEL" );

        String ircmsg = "!transfer receiver 10";
        when( event.getMessage() ).thenReturn( ircmsg );

        KarmaTransfer karmaTransfer = new KarmaTransfer();
        karmaTransfer.onGenericMessage( event );

        String expectedResponse = "You must be in " + getChannel() + " to transfer karma.";
        verify( event, times( 1 ) ).respondWith( expectedResponse );
    }

    @Test
    public void transferToSelfTest()
    {
        String ircmsg = "!transfer senderNick 10";
        when( event.getMessage() ).thenReturn( ircmsg );

        KarmaTransfer karmaTransfer = new KarmaTransfer();
        karmaTransfer.onGenericMessage( event );

        String expectedResponse = "Really? What's the point of transferring karma to yourself?";
        verify( event, times( 1 ) ).respondWith( expectedResponse );
    }

    @Test
    public void invalidTransferCancelTest()
    {
        String ircmsg = "!transfercancel";
        when( event.getMessage() ).thenReturn( ircmsg );

        KarmaTransfer karmaTransfer = new KarmaTransfer();
        karmaTransfer.onGenericMessage( event );

        verify( event, times( 1 ) ).respondWith( "No active transfer."
            + " To transfer karma enter '!transfer <noun> <karma amount>'." );
    }

    @Test
    public void invalidRejectTransferTest()
    {
        String ircmsg = "!reject";
        when( event.getMessage() ).thenReturn( ircmsg );

        KarmaTransfer karmaTransfer = new KarmaTransfer();
        karmaTransfer.onGenericMessage( event );

        verify( event, times( 1 ) ).respondWith( "No active transfer."
            + " To transfer karma enter '!transfer <noun> <karma amount>'." );
    }

    private String getChannel()
    {
        String configLine = "";
        String channel = "";

        BufferedReader configFile;

        try
        {
            if ( new File( VILEBOT_CONFIG_FILE ).exists() )
            {
                configFile = new BufferedReader( new FileReader( VILEBOT_CONFIG_FILE ) );
            }
            else
            {
                configFile = new BufferedReader( new FileReader( VILEBOT_CONFIG_FILE_SECONDARY ) );
            }
            while ( ( configLine = configFile.readLine() ) != null )
            {
                if ( configLine.length() > 12 )
                {
                    channel = configLine.substring( 0, 11 );
                }
                if ( "ircChannel1".equals( channel ) )
                {
                    break;
                }
            }
        }
        catch ( Exception e )
        {
            System.out.println( "Error reading config file: " + e.getMessage() );
        }

        return configLine.substring( 12 );
    }
}
