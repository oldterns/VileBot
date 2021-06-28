package com.oldterns.vilebot.util;

// Taken from keratin-irc: https://github.com/ASzc/keratin-irc

/*
  Copyright (C) 2013 Alexander Szczuczko

  This file may be modified and distributed under the terms
  of the MIT license. See the LICENSE file for details.
 */

/**
 * Contains the de facto standard IRC colour codes.
 */
public class Colors
{
    public static final String BLACK = "\u000301";

    public static final String BLUE = "\u000312";

    public static final String BOLD = "\u0002";

    public static final String BROWN = "\u000305";

    public static final String CYAN = "\u000311";

    public static final String DARK_BLUE = "\u000302";

    public static final String DARK_GRAY = "\u000314";

    public static final String DARK_GREEN = "\u000303";

    public static final String GREEN = "\u000309";

    public static final String LIGHT_GRAY = "\u000315";

    public static final String MAGENTA = "\u000313";

    public static final String NORMAL = "\u000f";

    public static final String OLIVE = "\u000307";

    public static final String PURPLE = "\u000306";

    public static final String RED = "\u000304";

    public static final String REVERSE = "\u0016";

    public static final String TEAL = "\u000310";

    public static final String UNDERLINE = "\u001f";

    public static final String WHITE = "\u000300";

    public static final String YELLOW = "\u000308";

    /**
     * Return a copy of the input String with IRC bold characters inserted.
     *
     * @return The modified copy of the input String
     */
    public static String bold( String input )
    {
        return BOLD + input + BOLD;
    }
}
