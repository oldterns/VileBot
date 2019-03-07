/*
  Copyright (C) 2013 Oldterns

  This file may be modified and distributed under the terms
  of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import com.oldterns.vilebot.db.GroupDB;
import com.oldterns.vilebot.util.BaseNick;
import com.oldterns.vilebot.util.Ignore;
import org.pircbotx.Configuration;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.JoinEvent;

public class Ops
    extends ListenerAdapter
{

    @Override
    public void onJoin( final JoinEvent event )
    {
        String joiner = event.getUser().getNick();
        String joinerBaseNick = BaseNick.toBaseNick( joiner );

        if ( !Ignore.getAutoOp().contains( event.getChannel().getName() )
            && event.getChannel().isOp( event.getBot().getUserBot() ) )
        {
            if ( event.getBot().getNick().equals( joiner ) )
            {
                for ( User user : event.getChannel().getNormalUsers() )
                {
                    if ( GroupDB.isOp( BaseNick.toBaseNick( user.getNick() ) ) )
                    {
                        event.getChannel().send().op( new Configuration.BotFactory().createUserHostmask( event.getBot(),
                                                                                                         user.getHostmask() ) );
                    }
                }
            }
            else if ( GroupDB.isOp( joinerBaseNick ) )
            {
                event.getChannel().send().op( event.getUserHostmask() );
            }

        }
    }
}
