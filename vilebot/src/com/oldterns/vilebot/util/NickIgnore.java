/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.util;

import java.util.HashSet;
import java.util.Set;

public class NickIgnore
{
    private static Set<String> onJoin = new HashSet<String>();

    public static Set<String> getOnJoin()
    {
        return onJoin;
    }

    public static void addOnJoin( String nick )
    {
        onJoin.add( nick );
    }
}
