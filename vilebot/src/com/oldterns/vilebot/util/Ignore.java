/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.util;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class Ignore {
    private static Set<String> onJoin = new ConcurrentSkipListSet<String>();

    private static Set<String> autoOp = new ConcurrentSkipListSet<String>();

    public static Set<String> getOnJoin() {
        return onJoin;
    }

    public static Set<String> getAutoOp() {
        return autoOp;
    }

    public static void addOnJoin(String nick) {
        onJoin.add(nick);
    }

    public static void addAutoOp(String channel) {
        autoOp.add(channel);
    }
}
