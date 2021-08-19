package com.oldterns.irc.bot.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OnChannelMessage {
    String value();
    String channel() default "<<default>>";
}