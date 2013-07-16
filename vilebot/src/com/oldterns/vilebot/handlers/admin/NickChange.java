/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import net.engio.mbassy.listener.Handler;

import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class NickChange
{
    @AssignedBot
    private KeratinBot bot;


    @Handler
    private void changeNick( ReceivePrivmsg event )
    {
        String text = event.getText();

        if ( text.startsWith( "!nick " ) )
        {
            bot.setNick( text.substring( text.indexOf( ' ' ) ) );

            // try
            // {
            // bus.publish( new SendNick( bus, text.substring( 7 ) ) );
            // }
            // catch ( InvalidMessagePrefixException | InvalidMessageCommandException | InvalidMessageParamException e )
            // {
            // Logger.error( e, "Error sending nick" );
            // }
        }
    }
}
