package com.oldterns.irc.bot.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Delimiter {
    String value();
}
