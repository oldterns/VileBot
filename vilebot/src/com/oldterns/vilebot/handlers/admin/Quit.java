/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.IrcEvent;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;
import ca.szc.keratin.core.event.message.send.SendQuit;
import ca.szc.keratin.core.net.message.InvalidMessageCommandException;
import ca.szc.keratin.core.net.message.InvalidMessageParamException;
import ca.szc.keratin.core.net.message.InvalidMessagePrefixException;

@HandlerContainer
public class Quit
{
    /**
     * Exit on the !quit command
     */
    @Handler
    private void quit( ReceivePrivmsg event )
    {
        MBassador<IrcEvent> bus = event.getBus();

        String text = event.getText();

        if ( text.startsWith( "!quit" ) )
        {
            try
            {
                bus.publish( new SendQuit( bus, "Daisy, Daisy, give me your answer, do..." ) );
            }
            catch ( InvalidMessagePrefixException | InvalidMessageCommandException | InvalidMessageParamException e )
            {
                // TODO
            }
            System.exit( 0 );
        }
    }
}
