/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class UserPing {
    /**
     * Reply to user !ping command with username: pong
     */
    @Handler
    private void userPingPong(ReceivePrivmsg event) {
        String text = event.getText();

        if (text.startsWith("!ping")) {
            event.replyDirectly("pong");
        }
    }
}
