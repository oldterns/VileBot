/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Sessions;

@HandlerContainer
public class Ops
{
    private static final Pattern nickPattern = Pattern.compile( "(\\S+)" );

    private static final Pattern addRemOpPattern = Pattern.compile( "!admin (un|)op ((?:" + nickPattern
        + ")+(?:, +| +|$))" );

    @Handler
    private void addRemOp( ReceivePrivmsg event )
    {
        String text = event.getText();
        Matcher matcher = addRemOpPattern.matcher( text );
        String sender = event.getSender();

        if ( matcher.matches() )
        {
            String username = Sessions.getSession( sender );
            if ( GroupDB.isAdmin( username ) )
            {
                String mode = BaseNick.toBaseNick( matcher.group( 1 ) );
                String nickBlob = BaseNick.toBaseNick( matcher.group( 2 ) );

                List<String> nicks = new LinkedList<String>();
                Matcher nickMatcher = nickPattern.matcher( nickBlob );
                while ( nickMatcher.find() )
                {
                    nicks.add( nickMatcher.group( 1 ) );
                }

                StringBuilder successNicks = new StringBuilder();
                StringBuilder failureNicks = new StringBuilder();

                if ( "un".equals( mode ) )
                {
                    for ( String nick : nicks )
                    {
                        StringBuilder selectedSB;
                        if ( GroupDB.remOp( nick ) )
                            selectedSB = successNicks;
                        else
                            selectedSB = failureNicks;

                        selectedSB.append( nick );
                        selectedSB.append( "" );
                    }

                    if ( successNicks.length() > 0 )
                        event.reply( "Removed " + successNicks.toString() + "from operator group" );
                    if ( failureNicks.length() > 0 )
                        event.reply( failureNicks.toString() + "was/were not in the operator group" );
                }
                else
                {
                    for ( String nick : nicks )
                    {
                        StringBuilder selectedSB;
                        if ( GroupDB.addOp( nick ) )
                            selectedSB = successNicks;
                        else
                            selectedSB = failureNicks;

                        selectedSB.append( nick );
                        selectedSB.append( "" );
                    }

                    if ( successNicks.length() > 0 )
                        event.reply( "Added " + successNicks.toString() + "to operator group" );
                    if ( failureNicks.length() > 0 )
                        event.reply( failureNicks.toString() + "was/were already in the operator group" );
                }
            }
        }
    }
}
