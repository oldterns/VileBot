/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.BaseNick;

import net.engio.mbassy.listener.Handler;

import ca.szc.keratin.bot.Channel;
import ca.szc.keratin.bot.KeratinBot;
import ca.szc.keratin.bot.annotation.AssignedBot;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceiveChannelMode;
import ca.szc.keratin.core.event.message.recieve.ReceiveJoin;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class Ops
{
    private static final Pattern nickPattern = Pattern.compile( "\\S+" );

    private static final Pattern addRemOpPattern = Pattern.compile( "!(un|)op (" + nickPattern + ")" );

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

    // TODO move to admin
    @Handler
    private void addRemOp( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = addRemOpPattern.matcher( text );

        if ( matcher.matches() )
        {
            String mode = BaseNick.toBaseNick( matcher.group( 1 ) );
            String nick = BaseNick.toBaseNick( matcher.group( 2 ) );

            if ( "un".equals( mode ) )
            {
                if ( GroupDB.remOp( nick ) )
                    event.reply( "Removed " + nick + " from operator group" );
                else
                    event.reply( nick + " was not in the operator group" );
            }
            else
            {
                if ( GroupDB.addOp( nick ) )
                    event.reply( "Added " + nick + " to operator group" );
                else
                    event.reply( nick + " was already in the operator group" );
            }
        }
    }
}
