/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.LinkedList;
import java.util.List;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.Channel;
import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceiveChannelMode;
import ca.szc.keratin.core.event.message.recieve.ReceiveJoin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.BaseNick;

@HandlerContainer
public class Ops
{
    @AssignedBot
    private KeratinBot bot;

    @Handler
    private void giveOpOnJoin( ReceiveJoin event )
    {
        String joiner = event.getJoiner();
        String joinerBaseNick = BaseNick.toBaseNick( joiner );

        Channel channel = bot.getChannel( event.getChannel() );

        if ( !bot.getNick().equals( joiner ) && channel.isOp( bot.getNick() ) && GroupDB.isOp( joinerBaseNick ) )
            bot.opNick( event.getChannel(), joiner );
    }

    @Handler
    private void giveOpOnOp( ReceiveChannelMode event )
    {
        Channel channel = bot.getChannel( event.getTarget() );
        String flags = event.getFlags();
        List<String> flagParams = event.getFlagParams();

        String botNick = bot.getNick();

        if ( flagParams.contains( botNick ) && flags.contains( "+o" ) )
        {
            List<String> nicks = channel.getRegularNicks();

            List<String> newOpNicks = new LinkedList<String>();
            for ( String nick : nicks )
            {
                if ( !botNick.equals( nick ) )
                {
                    String baseNick = BaseNick.toBaseNick( nick );
                    if ( GroupDB.isOp( baseNick ) )
                        newOpNicks.add( nick );
                }
            }

            bot.opNicks( channel.getName(), newOpNicks );
        }
    }
}
