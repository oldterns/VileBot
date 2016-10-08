/**
 * Copyright (C) 2013 Oldterns
 *
 * This file may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.oldterns.vilebot.handlers.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oldterns.vilebot.CharactersThatBreakEclipse;

import net.engio.mbassy.listener.Handler;
import ca.szc.keratin.bot.annotation.HandlerContainer;
import ca.szc.keratin.core.event.message.recieve.ReceivePrivmsg;

@HandlerContainer
public class Jokes {
    private static final Pattern listJokePattern = Pattern.compile("!(randommeme|dance|chuck)");

    private static final Pattern redditPattern = Pattern.compile("(?i)reddit");

    private static final Random random = new Random();

    private static final List<String> memes = generateMemes();

    private static final List<String> dances = generateDances();

    private static final List<String> chucks = generateChucks();

    @Handler
    private void redditLOD(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher matcher = redditPattern.matcher(text);

        if (matcher.find()) {
            if (random.nextInt(10) > 6) {
                event.reply(CharactersThatBreakEclipse.LODEMOT);
            }
        }
    }

    /**
     * Reply to user !randommeme command with a random selection from the meme list
     */
    @Handler
    private void listBasedJokes(ReceivePrivmsg event) {
        String text = event.getText();
        Matcher matcher = listJokePattern.matcher(text);

        if (matcher.matches()) {
            String mode = matcher.group(1);

            String reply;

            if ("dance".equals(mode))
                reply = dances.get(random.nextInt(dances.size()));
            else if ("chuck".equals(mode))
                reply = chucks.get(random.nextInt(chucks.size()));
            else
                reply = memes.get(random.nextInt(memes.size()));

            event.reply(reply);
        }
    }

    private static List<String> generateChucks() {
        List<String> chucks = new ArrayList<String>();

        // Anyone called Chuck except Chuck Norris

        chucks.add("Chuck Palahniuk once said, \"Your birth is a mistake you'll spend your whole life trying to correct.\"");
        chucks.add("Chuck Tanner once said, \"If you don't like the way the Atlanta Braves are playing then you don't like baseball.\"");
        chucks.add("Chuck Berry once said, \"Roll over, Beethoven, and tell Tchaikovsky the news.\"");
        chucks.add("Chuck Yeager once said, \"Rules are made for people who aren't willing to make up their own.\"");
        chucks.add("Chuck Klosterman once said, \"I guess what I'm saying is, I'm not your enemy.\"");
        chucks.add("Chuck Jones once said, \"A lion's work hours are only when he's hungry; once he's satisfied, the predator and prey live peacefully together.\"");
        chucks.add("Chuck Close once said, \"I'm very learning-disabled, and I think it drove me to what I'm doing.\"");
        chucks.add("Chuck D once said, \"I think governments are the cancer of civilization.\"");
        chucks.add("Chuck Knox once said, \"Always have a plan, and believe in it. Nothing happens by accident.\"");
        chucks.add("Chuck Zito once said, \"If the challenge to fight was there, I always took it.\"");

        return chucks;
    }

    private static List<String> generateDances() {
        List<String> dances = new ArrayList<String>();

        dances.add("No. " + CharactersThatBreakEclipse.LODEMOT);
        dances.add("Bots as vile as I do not dance.");
        dances.add("ScumbagBot does not dance.");
        dances.add("Chuck Norris once asked me to dance. He didn't have a comeback after the roundhouse kickban.");
        dances.add(CharactersThatBreakEclipse.KIRBYFLIP + "\n*kills Kirby*\n*kills dance*");
        dances.add("Does doing a barrel roll count?");

        return dances;
    }

    private static List<String> generateMemes() {
        List<String> memes = new ArrayList<String>();

        memes.add(CharactersThatBreakEclipse.LODEMOT);
        memes.add("Derp.");
        memes.add("looks like his string was, *puts on glasses*\nnull terminated.\nYEEEEEEEEEEEEEEEEEEEEEEAAAAAAHHHHHHHHH");
        memes.add("Yo Dawg");
        memes.add("Imma gunna cut you so bad, you gunna wish I dun cut you so bad");
        memes.add("So i herd u liek mudkipz");
        memes.add("Never gunna give you up, Never gunna let you down!");
        memes.add("DO A BARREL ROLL ._. |: .-. :| ._.");
        memes.add("ScumbagBot, shows up to the party, drinks all your beer.");
        memes.add("Don't tase me bro");

        return memes;
    }

    @SuppressWarnings("unused")
    private static List<String> generateEaster() {
        List<String> easter = new ArrayList<String>();

        easter.add("I hate easter.  Laying foil-covered eggs is no fun, no fun at all.");
        easter.add("Buck, buck.");
        easter.add("twitches her nose.  Isn't that cute?");
        easter.add("strains and grunts a little, and out pops a Cadbury cream egg!");
        easter.add("Has anybody seen Santa Claus around?  He's supposed to cover for me on my lunch break.");
        easter.add("nibbles on a hunk of milk chocolate.");
        easter.add("takes three hops to the left.");
        easter.add("Buck.");
        easter.add("Bu-buck.");
        easter.add("takes three hops to the right.");
        easter.add("Bu-bu-buck.");
        easter.add("just sits here being fuzzy.");
        easter.add("Buck, buck, buck.");

        return easter;
    }

}
