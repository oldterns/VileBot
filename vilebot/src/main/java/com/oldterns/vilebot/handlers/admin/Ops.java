/*
  Copyright (C) 2013 Oldterns

  This file may be modified and distributed under the terms
  of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.admin;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Sessions;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ops
    extends ListenerAdapter
{
    private static final Pattern nickPattern = Pattern.compile( "(\\S+)" );

    private static final Pattern addRemOpPattern =
        Pattern.compile( "!admin (un|)op ((?:" + nickPattern + "(?:, +| +|$))+)" );

    @Override
    public void onGenericMessage( GenericMessageEvent event )
    {
        String text = event.getMessage();
        Matcher matcher = addRemOpPattern.matcher( text );
        String sender = event.getUser().getNick();

        if ( matcher.matches() )
        {
            String username = Sessions.getSession( sender );
            if ( GroupDB.isAdmin( username ) )
            {
                String mode = BaseNick.toBaseNick( matcher.group( 1 ) );
                String nickBlob = matcher.group( 2 );

                List<String> nicks = new LinkedList<>();
                Matcher nickMatcher = nickPattern.matcher( nickBlob );
                while ( nickMatcher.find() )
                {
                    nicks.add( BaseNick.toBaseNick( nickMatcher.group( 1 ) ) );
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
                        selectedSB.append( " " );
                    }

                    if ( successNicks.length() > 0 )
                        event.respondWith( "Removed " + successNicks.toString() + "from operator group" );
                    if ( failureNicks.length() > 0 )
                        event.respondWith( failureNicks.toString() + "was/were not in the operator group" );
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
                        selectedSB.append( " " );
                    }

                    if ( successNicks.length() > 0 )
                        event.respondWith( "Added " + successNicks.toString() + "to operator group" );
                    if ( failureNicks.length() > 0 )
                        event.respondWith( failureNicks.toString() + "was/were already in the operator group" );
                }
            }
        }
    }
}
